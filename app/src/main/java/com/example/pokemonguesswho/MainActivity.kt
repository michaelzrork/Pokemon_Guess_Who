package com.example.pokemonguesswho

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.ui.AppNavigation
import com.example.pokemonguesswho.ui.AppTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PokemonViewModel

    // Track whether discoverability has already been granted this session
    private var isDiscoverable = false

    // Callback to invoke after discoverability is resolved
    private var onDiscoverabilityResult: ((Boolean) -> Unit)? = null

    // Bluetooth permission launcher
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted or denied - the app will handle it gracefully
    }

    // Discoverability launcher — makes the host device visible to nearby BT scans
    private val discoverabilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode > 0) {
            Log.d("MainActivity", "Device discoverable for ${result.resultCode} seconds")
            isDiscoverable = true
            onDiscoverabilityResult?.invoke(true)
        } else {
            Log.d("MainActivity", "Discoverability denied by user")
            // Even if denied, still let them host — paired devices can still connect
            onDiscoverabilityResult?.invoke(true)
        }
        onDiscoverabilityResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[PokemonViewModel::class.java]

        // Request Bluetooth permissions
        requestBluetoothPermissions()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onRequestDiscoverability = { callback ->
                            ensureDiscoverable(callback)
                        }
                    )
                }
            }
        }
    }

    /**
     * Ensure the device is discoverable. If already discoverable this session,
     * invoke callback immediately. Otherwise launch the system prompt once.
     */
    private fun ensureDiscoverable(callback: (Boolean) -> Unit) {
        if (isDiscoverable) {
            callback(true)
            return
        }
        onDiscoverabilityResult = callback
        requestDiscoverability()
    }

    private fun requestBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        } else {
            // Android 11 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            bluetoothPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @Suppress("DEPRECATION")
    private fun requestDiscoverability() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) // 5 minutes
        }
        discoverabilityLauncher.launch(intent)
    }
}
