package com.example.bluetoothanalyzer3

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quevedo.soares.leandro.blemadeeasy.BLE
import quevedo.soares.leandro.blemadeeasy.models.BLEDevice

class BluetoothViewModel : ViewModel() {
	private val _scannedDevices = MutableStateFlow<List<BLEDevice>>(emptyList())
	val scannedDevices: StateFlow<List<BLEDevice>>
		get() = _scannedDevices.asStateFlow()

	private val _isPermissionGranted = MutableStateFlow<Boolean>(false)
	val isPermissionGranted: StateFlow<Boolean>
		get() = _isPermissionGranted.asStateFlow()

	private val _isBluetoothEnabled = MutableStateFlow<Boolean>(false)
	val isBluetoothEnabled: StateFlow<Boolean>
		get() = _isBluetoothEnabled.asStateFlow()

	private val _isShouldShowRational = MutableStateFlow<Boolean>(false)
	val isShouldShowRational: StateFlow<Boolean>
		get() = _isShouldShowRational.asStateFlow()

	private val _isScanning = MutableStateFlow<Boolean>(false)
	val isScanning: StateFlow<Boolean>
		get() = _isScanning.asStateFlow()

	private val _counter = MutableStateFlow<Int>(0)
	val counter: StateFlow<Int>
		get() = _counter.asStateFlow()

	fun sortFoundDevices() {
		_scannedDevices.value = _scannedDevices.value.sortedByDescending { it.rsii }
	}

	private var timeSinceLastDevice: Int = 0

	private suspend fun startCountingTime() = coroutineScope {
		launch {
			try {
				delay(1000)
				timeSinceLastDevice++
			} catch (e: CancellationException) {
				timeSinceLastDevice = 0
			} finally {
				timeSinceLastDevice = 0
			}
		}

	}

	@SuppressLint("MissingPermission")
	fun scanForDevices(
		ble: BLE,
		onError: (Int) -> () -> Unit,
		isContinued: Boolean = false,
	) {
		var timer: Job = GlobalScope.launch { }
		if (!isContinued) {
			_scannedDevices.value = emptyList()
		}
		_isScanning.value = true
		ble.scanAsync(duration = 300000,      /* This is optional, if you want to update your interface in realtime */
			onDiscover = { device ->
				if (!isAlreadyFound(device)) {
					_scannedDevices.value += device
					timer.cancel()
					timer = GlobalScope.launch {
						startCountingTime()
					}
					Log.d("Found", _scannedDevices.value.toString())
				} else {
					findAndReplaceRssi(device)
				}
			},

			onFinish = { foundDevices ->
				if (timeSinceLastDevice > 5 && !isContinued) {
					foundDevices.forEach { device ->
						if (!isAlreadyFound(device)) {
							_scannedDevices.value += device
						} else {
							findAndReplaceRssi(device)
						}
					}
					_isScanning.value = false
				} else {
					scanForDevices(ble, onError, isContinued = true)
				}
			}, onError = { errorCode ->
				onError(errorCode)
			})
	}

	fun checkBluetoothState(ble: BLE, onReady: () -> Unit) {
		ble.verifyBluetoothAdapterStateAsync { active ->
			_isBluetoothEnabled.value = active
			onReady()
		}
	}

	private fun isAlreadyFound(device: BLEDevice): Boolean {
		_scannedDevices.value.forEach { scannedDevice ->
			var f = true // нужно чтобы, если устройство поменяло mac-адресс, то оно не дублировалось
			if (device.name != "") f = scannedDevice.name == device.name
			if (scannedDevice.macAddress == device.macAddress && f) return true
		}
		return false
	}

	private fun findAndReplaceRssi(device: BLEDevice) {
		_scannedDevices.value.forEach { scannedDevice ->
			if (scannedDevice.macAddress == device.macAddress) {
				scannedDevice.rsii = device.rsii
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun checkBluetoothPermissions(ble: BLE, onReady: () -> Unit) {
		ble.verifyPermissionsAsync(rationaleRequestCallback = { next ->
			// Include your code to show an Alert or UI explaining why the permissions are required
			// Calling the function bellow if the user agrees to give the permissions
			next()
		}, callback = { granted ->
			_counter.value++
			if (granted) {
				_isPermissionGranted.value = true
				_isShouldShowRational.value = false
			} else {
				// Include your code to show an Alert or UI indicating that the permissions are required

				_isPermissionGranted.value = false
				_isShouldShowRational.value = true
				Log.e("bluetooth", "permission denied")
			}
			onReady()
		})
	}
}

