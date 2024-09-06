package com.example.bluetoothanalyzer3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluetoothanalyzer3.ui.theme.BluetoothAnalyzer3Theme
import kotlinx.coroutines.Delay
import kotlinx.coroutines.delay
import quevedo.soares.leandro.blemadeeasy.*
import quevedo.soares.leandro.blemadeeasy.models.BLEDevice

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		val ble = BLE(componentActivity = this)

		setContent {
			BluetoothAnalyzer3Theme {
				Surface {
					Scaffold(
						modifier = Modifier
							.fillMaxSize()
							.padding(WindowInsets.statusBars.asPaddingValues()), topBar = {
							Text(text = "Bluetooth analyzer", style = MaterialTheme.typography.headlineMedium)
						}
					) { innerPadding ->
						AppScreen(modifier = Modifier.padding(innerPadding), ble = ble)
					}

				}
			}
		}
	}
}


@SuppressLint("MissingPermission")
@Composable
fun AppScreen(
	modifier: Modifier = Modifier,
	ble: BLE,
	viewModel: BluetoothViewModel = viewModel(),
) {
	val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
	val isShouldShouldShowRational by viewModel.isShouldShowRational.collectAsState()
	val counter by viewModel.counter.collectAsState()
	val scannedDevices by viewModel.scannedDevices.collectAsState()
	val isScanning by viewModel.isScanning.collectAsState()
	val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
	var isReady by remember { mutableStateOf(true) }
	val context = LocalContext.current

	Column(modifier = modifier) {
		LaunchedEffect(counter) {
			if (isReady) {
				isReady = false
				delay(1000)
				if (counter == 4 && !isPermissionGranted) {
					context.startActivity(Intent().apply {
						action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
						data = Uri.parse("package:${context.packageName}")
					})
				}
				viewModel.checkBluetoothPermissions(ble, onReady = { isReady = true })
				if (isPermissionGranted) {
					isReady = false
					viewModel.checkBluetoothState(ble, onReady = { isReady = true })
				}
			}
		}
		if (isPermissionGranted) {
			Button(onClick = {
				viewModel.checkBluetoothState(ble, onReady = {})
				if (isBluetoothEnabled) {
					viewModel.scanForDevices(
						ble,
						onError = { errorCode -> {} })
					Log.d("bluetooth", "Scanning started")
				}
			}, enabled = !isScanning) {
				Text(text = if (isScanning) "scanning..." else "Scan for devices")
			}
		}
		LazyColumn {
			var i = 1
			items(scannedDevices.sortedByDescending { it.rsii }) { device ->
				Row {
					Text(text = "${i++}: $device")
				}
			}
		}
	}
}

