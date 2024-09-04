package com.example.bluetoothanalyzer3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quevedo.soares.leandro.blemadeeasy.BLE

class BluetoothViewModel : ViewModel() {
	private val _isPermissionGranted = MutableStateFlow<Boolean>(false)
	val isPermissionGranted: StateFlow<Boolean>
		get() = _isPermissionGranted.asStateFlow()

	private val _isBluetoothEnabled = MutableStateFlow<Boolean>(false)
	val isBluetoothEnabled: StateFlow<Boolean>
		get() = _isBluetoothEnabled.asStateFlow()

	private val _isShouldShowRational = MutableStateFlow<Boolean>(false)
	val isShouldShowRational: StateFlow<Boolean>
		get() = _isShouldShowRational.asStateFlow()

	private val _counter = MutableStateFlow<Int>(0)
	val counter: StateFlow<Int>
		get() = _counter.asStateFlow()

	fun checkBluetoothState(ble: BLE, onDisabled: () -> Unit) {
		ble.verifyBluetoothAdapterStateAsync { active ->
			if (active) {
				_isBluetoothEnabled.value = true
			} else {
				onDisabled()
			}
		}
	}

	@SuppressLint("MissingPermission")
	fun checkBluetoothPermissions(ble: BLE) {
		if (counter.value >= 2) return
		_counter.value++
		ble.verifyPermissionsAsync(rationaleRequestCallback = { next ->
			// Include your code to show an Alert or UI explaining why the permissions are required
			// Calling the function bellow if the user agrees to give the permissions
			next()
		}, callback = { granted ->
			if (granted) {
				_isPermissionGranted.value = true
				_isShouldShowRational.value = false
			} else {
				// Include your code to show an Alert or UI indicating that the permissions are required
				_isShouldShowRational.value = true
				GlobalScope.launch {
					delay(3000)
					checkBluetoothPermissions(ble)
				}
				Log.e("bluetooth", "permission denied")
			}
		})
	}
}