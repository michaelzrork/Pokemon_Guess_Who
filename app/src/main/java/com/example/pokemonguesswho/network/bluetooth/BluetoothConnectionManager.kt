package com.example.pokemonguesswho.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

enum class BluetoothConnectionState {
    DISCONNECTED,
    LISTENING,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

@SuppressLint("MissingPermission")
class BluetoothConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "BTConnectionManager"
        // UUID for RFCOMM connection
        private val SERVICE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        private const val SERVICE_NAME = "PokemonGuessWho"
        // UUID for BLE advertising — uses a 16-bit short UUID embedded in the standard BLE base
        private val BLE_SERVICE_UUID = ParcelUuid.fromString("0000ABCD-0000-1000-8000-00805F9B34FB")
        private const val BLE_SCAN_DURATION_MS = 15000L
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    private var inputReader: BufferedReader? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _discoveredGames = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredGames.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Server job so we can cancel it cleanly
    private var serverJob: Job? = null

    // BLE advertiser (host side)
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    // BLE scanner (client side)
    private var bleScanner: BluetoothLeScanner? = null
    private var scanJob: Job? = null
    private val discoveredAddresses = mutableSetOf<String>()

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val name = device.name ?: "Pokemon Host"

            if (discoveredAddresses.add(address)) {
                Log.d(TAG, "BLE scan found game host: $name ($address) RSSI=${result.rssi}")
                addHostedGame(name, address)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error code: $errorCode")
            _isScanning.value = false
            _errorMessage.value = "BLE scan failed (error $errorCode)"
        }
    }

    private val bleAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE advertising started successfully")
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed to start: errorCode=$errorCode")
            isAdvertising = false
            // Fall through — RFCOMM server still works for paired devices
        }
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    // ---- SERVER SIDE (Host) ----

    fun startServerAsync(scope: CoroutineScope, onConnected: () -> Unit, onError: () -> Unit) {
        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = BluetoothConnectionState.LISTENING
                _errorMessage.value = null

                serverSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)

                if (serverSocket == null) {
                    _connectionState.value = BluetoothConnectionState.ERROR
                    _errorMessage.value = "Could not create server socket"
                    onError()
                    return@launch
                }

                Log.d(TAG, "RFCOMM server listening...")

                // Start BLE advertising so clients can find us
                startBleAdvertising()

                while (true) {
                    val socket: BluetoothSocket
                    try {
                        socket = serverSocket?.accept() ?: break
                    } catch (e: IOException) {
                        if (_connectionState.value == BluetoothConnectionState.LISTENING) {
                            Log.d(TAG, "Server socket closed while listening")
                        }
                        break
                    }

                    Log.d(TAG, "Accepted connection from: ${socket.remoteDevice?.name} (${socket.remoteDevice?.address})")

                    try {
                        val reader = BufferedReader(InputStreamReader(socket.inputStream))

                        val handshake = withTimeoutOrNull(3000L) {
                            try {
                                reader.readLine()
                            } catch (e: IOException) {
                                null
                            }
                        }

                        if (handshake == "POKEMON_GUESS_WHO_JOIN") {
                            connectedSocket = socket
                            inputReader = reader
                            outputStream = socket.outputStream
                            _connectedDeviceName.value = socket.remoteDevice?.name ?: "Unknown Device"
                            _connectionState.value = BluetoothConnectionState.CONNECTED
                            Log.d(TAG, "Real client connected: ${socket.remoteDevice?.name}")

                            // Stop advertising and close server socket
                            stopBleAdvertising()
                            try { serverSocket?.close() } catch (_: IOException) {}
                            serverSocket = null

                            onConnected()
                            return@launch
                        } else {
                            Log.d(TAG, "Probe/invalid connection from ${socket.remoteDevice?.name}, continuing...")
                            try { socket.close() } catch (_: IOException) {}
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Connection dropped (probe): ${e.message}")
                        try { socket.close() } catch (_: IOException) {}
                    }
                }

                if (_connectionState.value == BluetoothConnectionState.LISTENING) {
                    _connectionState.value = BluetoothConnectionState.ERROR
                    _errorMessage.value = "Server closed unexpectedly"
                    onError()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Server error", e)
                if (_connectionState.value == BluetoothConnectionState.LISTENING) {
                    _connectionState.value = BluetoothConnectionState.ERROR
                    _errorMessage.value = "Server error: ${e.message}"
                    onError()
                }
            }
        }
    }

    // ---- BLE ADVERTISING (Host broadcasts presence) ----

    private fun startBleAdvertising() {
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bleAdvertiser == null) {
            Log.w(TAG, "BLE advertiser not available on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false) // We don't use BLE GATT — just advertising for discovery
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(BLE_SERVICE_UUID)
            .build()

        try {
            bleAdvertiser?.startAdvertising(settings, data, bleAdvertiseCallback)
            Log.d(TAG, "Starting BLE advertising with UUID: $BLE_SERVICE_UUID")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE advertising: ${e.message}")
        }
    }

    private fun stopBleAdvertising() {
        if (isAdvertising) {
            try {
                bleAdvertiser?.stopAdvertising(bleAdvertiseCallback)
                Log.d(TAG, "BLE advertising stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping BLE advertising: ${e.message}")
            }
            isAdvertising = false
        }
    }

    // ---- CLIENT SIDE (Joiner) ----

    fun startDiscovery() {
        _connectionState.value = BluetoothConnectionState.DISCOVERING
        _errorMessage.value = null
        _discoveredGames.value = emptyList()
        _isScanning.value = true
        discoveredAddresses.clear()

        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "BLE scanner not available")
            _errorMessage.value = "BLE scanner not available"
            _isScanning.value = false
            return
        }

        // Scan filter: only find devices advertising our service UUID
        val filter = ScanFilter.Builder()
            .setServiceUuid(BLE_SERVICE_UUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner?.startScan(listOf(filter), settings, bleScanCallback)
            Log.d(TAG, "BLE scan started, looking for service UUID: $BLE_SERVICE_UUID")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan: ${e.message}")
            _isScanning.value = false
            return
        }

        // Also probe bonded devices via RFCOMM — they might be hosting without BLE
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            probeDevice(device)
        }

        // Auto-stop scan after timeout
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            delay(BLE_SCAN_DURATION_MS)
            if (_isScanning.value) {
                Log.d(TAG, "BLE scan timeout reached, stopping scan")
                withContext(Dispatchers.Main) {
                    stopBleScan()
                    _isScanning.value = false
                }
            }
        }
    }

    private fun probeDevice(device: BluetoothDevice) {
        val address = device.address ?: return
        val name = device.name ?: device.address ?: return

        if (!discoveredAddresses.add(address)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Probing bonded device $name ($address)...")
                val result = withTimeoutOrNull(5000L) {
                    try {
                        val probeSocket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)
                        probeSocket.connect()
                        probeSocket.close()
                        true
                    } catch (e: IOException) {
                        false
                    }
                }
                if (result == true) {
                    Log.d(TAG, "FOUND HOSTED GAME on bonded device $name ($address)")
                    addHostedGame(name, address)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Probe failed for $name: ${e.message}")
            }
        }
    }

    private fun addHostedGame(name: String, address: String) {
        val currentGames = _discoveredGames.value.toMutableList()
        if (currentGames.none { it.address == address }) {
            currentGames.add(BluetoothDeviceInfo(name = name, address = address))
            _discoveredGames.value = currentGames
        }
    }

    private fun stopBleScan() {
        try {
            bleScanner?.stopScan(bleScanCallback)
            Log.d(TAG, "BLE scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan: ${e.message}")
        }
    }

    fun stopDiscovery() {
        stopBleScan()
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    // ---- CLIENT CONNECT ----

    suspend fun connectToDevice(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = BluetoothConnectionState.CONNECTING
            _errorMessage.value = null

            // Stop BLE scan before RFCOMM connect
            stopBleScan()

            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device == null) {
                _connectionState.value = BluetoothConnectionState.ERROR
                _errorMessage.value = "Device not found"
                return@withContext false
            }

            clientSocket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)

            Log.d(TAG, "Connecting to ${device.name} ($address) via RFCOMM...")
            clientSocket?.connect()

            clientSocket?.let { socket ->
                connectedSocket = socket
                setupStreams(socket)
                _connectedDeviceName.value = device.name ?: "Unknown Device"

                // Send handshake
                val handshakeData = ("POKEMON_GUESS_WHO_JOIN\n").toByteArray(Charsets.UTF_8)
                outputStream?.write(handshakeData)
                outputStream?.flush()
                Log.d(TAG, "Sent handshake to ${device.name}")

                _connectionState.value = BluetoothConnectionState.CONNECTED
                Log.d(TAG, "Connected to ${device.name}")
                return@withContext true
            }

            _connectionState.value = BluetoothConnectionState.ERROR
            _errorMessage.value = "Connection failed"
            return@withContext false
        } catch (e: IOException) {
            Log.e(TAG, "Connect error", e)
            _connectionState.value = BluetoothConnectionState.ERROR
            _errorMessage.value = "Connection failed: ${e.message}"
            return@withContext false
        }
    }

    // ---- DATA TRANSFER ----

    private fun setupStreams(socket: BluetoothSocket) {
        inputReader = BufferedReader(InputStreamReader(socket.inputStream))
        outputStream = socket.outputStream
    }

    suspend fun sendMessage(message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream?.let { stream ->
                val data = (message + "\n").toByteArray(Charsets.UTF_8)
                stream.write(data)
                stream.flush()
                Log.d(TAG, "Sent: ${message.take(100)}...")
                return@withContext true
            }
            false
        } catch (e: IOException) {
            Log.e(TAG, "Send error", e)
            handleDisconnection()
            false
        }
    }

    suspend fun readMessage(): String? = withContext(Dispatchers.IO) {
        try {
            val line = inputReader?.readLine()
            if (line != null) {
                Log.d(TAG, "Received: ${line.take(100)}...")
            }
            line
        } catch (e: IOException) {
            Log.e(TAG, "Read error", e)
            handleDisconnection()
            null
        }
    }

    // ---- CLEANUP ----

    private fun handleDisconnection() {
        _connectionState.value = BluetoothConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
    }

    fun disconnect() {
        serverJob?.cancel()
        serverJob = null

        stopBleAdvertising()

        try {
            inputReader?.close()
            outputStream?.close()
            connectedSocket?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Disconnect error", e)
        }

        inputReader = null
        outputStream = null
        connectedSocket = null
        clientSocket = null
        serverSocket = null

        _connectionState.value = BluetoothConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
        _errorMessage.value = null
    }

    fun cleanup() {
        stopDiscovery()
        disconnect()
    }
}
