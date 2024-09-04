package com.example.bluetoothanalyzer3

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluetoothanalyzer3.ui.theme.BluetoothAnalyzer3Theme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import quevedo.soares.leandro.blemadeeasy.*

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
							.padding(WindowInsets.statusBars.asPaddingValues())
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
	var shouldGoToSettings by remember {
		mutableStateOf(false)
	}

	Column(modifier = modifier) {
		Text(text = counter.toString())
		Text(text = "Bluetooth analyzer")
		Text(text = "Permission: ${if (isPermissionGranted) "granted" else "denied"}")
		AnimatedVisibility(visible = isShouldShouldShowRational) {
			Text(text = "Bluetooth permission is necessary")
		}
		LaunchedEffect(Unit) {
			viewModel.checkBluetoothPermissions(ble)
		}
		if (isPermissionGranted) {
			viewModel.checkBluetoothState(ble, onDisabled = {
				shouldGoToSettings = true
			})
		}
	}

	if (shouldGoToSettings) {
		LocalContext.current.goToSettings()
	}
}

fun Context.goToSettings() {
	startActivity(
		Intent().apply {
			action = Settings.ACTION_BLUETOOTH_SETTINGS
			data = Uri.parse("package:$packageName")
		}
	)
}
