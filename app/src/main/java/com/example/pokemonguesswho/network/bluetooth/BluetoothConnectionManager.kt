package com.example.pokemonguesswho.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

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
        // BLE service UUID for advertising and GATT service
        private val BLE_SERVICE_UUID = UUID.fromString("0000ABCD-0000-1000-8000-00805F9B34FB")
        private val BLE_SERVICE_PARCEL_UUID = ParcelUuid.fromString("0000ABCD-0000-1000-8000-00805F9B34FB")
        // Characteristic for board data transfer (client writes request, host sends notification)
        private val BOARD_CHAR_UUID = UUID.fromString("0000ABCE-0000-1000-8000-00805F9B34FB")
        private const val BLE_SCAN_DURATION_MS = 15000L
        private const val GATT_ERROR_133 = 133
        private const val MAX_GATT_RETRIES = 5
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

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

    // BLE GATT server (host side)
    private var gattServer: BluetoothGattServer? = null
    private var boardDataToSend: String? = null
    private var onClientConnectedCallback: (() -> Unit)? = null

    // BLE scanner (client side)
    private var bleScanner: BluetoothLeScanner? = null
    private var scanJob: Job? = null
    private val discoveredAddresses = mutableSetOf<String>()

    // BLE GATT client (client side)
    private var gattClient: BluetoothGatt? = null
    private var receivedBoardData: String? = null

    // Store discovered BLE devices so we can connect via GATT
    private val discoveredBleDevices = mutableMapOf<String, BluetoothDevice>()

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val name = device.name ?: "Pokemon Host"

            if (discoveredAddresses.add(address)) {
                Log.d(TAG, "BLE scan found game host: $name ($address) RSSI=${result.rssi}")
                discoveredBleDevices[address] = device
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
        }
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    // ---- HOST SIDE: GATT Server + BLE Advertising ----

    /**
     * Set the board data that will be sent to the client when they connect.
     * Must be called before startServerAsync.
     */
    fun setBoardData(json: String) {
        boardDataToSend = json
        Log.d(TAG, "Board data set (${json.length} chars)")
    }

    fun startServerAsync(scope: CoroutineScope, onConnected: () -> Unit, onError: () -> Unit) {
        serverJob?.cancel()
        onClientConnectedCallback = onConnected

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = BluetoothConnectionState.LISTENING
                _errorMessage.value = null

                // Start GATT server
                val started = startGattServer()
                if (!started) {
                    _connectionState.value = BluetoothConnectionState.ERROR
                    _errorMessage.value = "Could not start GATT server"
                    onError()
                    return@launch
                }

                // Start BLE advertising so clients can find us
                startBleAdvertising()

                Log.d(TAG, "Host ready: GATT server + BLE advertising active")
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
                _connectionState.value = BluetoothConnectionState.ERROR
                _errorMessage.value = "Server error: ${e.message}"
                onError()
            }
        }
    }

    private fun startGattServer(): Boolean {
        val manager = bluetoothManager ?: return false

        gattServer = manager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED && device != null) {
                    Log.d(TAG, "GATT client connected: ${device.name} (${device.address})")
                    _connectedDeviceName.value = device.name ?: "Unknown Device"
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT client disconnected")
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                if (characteristic?.uuid == BOARD_CHAR_UUID && value != null) {
                    val request = String(value, Charsets.UTF_8)
                    Log.d(TAG, "GATT write request from ${device?.name}: $request")

                    if (request == "JOIN") {
                        // Send response acknowledging the write
                        if (responseNeeded) {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                        }

                        // Send the board data back via chunks
                        val data = boardDataToSend
                        if (data != null && device != null && characteristic != null) {
                            sendBoardDataToClient(device, characteristic, data)
                        }
                    } else {
                        if (responseNeeded) {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                        }
                    }
                } else {
                    if (responseNeeded && device != null) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    }
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                if (characteristic?.uuid == BOARD_CHAR_UUID && device != null) {
                    val data = boardDataToSend ?: ""
                    val bytes = data.toByteArray(Charsets.UTF_8)
                    // Handle offset for large data
                    val chunk = if (offset < bytes.size) {
                        bytes.copyOfRange(offset, bytes.size)
                    } else {
                        byteArrayOf()
                    }
                    Log.d(TAG, "GATT read request from ${device.name}, offset=$offset, sending ${chunk.size} bytes")
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, chunk)
                } else {
                    if (device != null) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    }
                }
            }
        })

        if (gattServer == null) return false

        // Create the GATT service with the board data characteristic
        val service = BluetoothGattService(
            BLE_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val boardCharacteristic = BluetoothGattCharacteristic(
            BOARD_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(boardCharacteristic)

        val added = gattServer?.addService(service) ?: false
        Log.d(TAG, "GATT service added: $added")
        return added
    }

    private fun sendBoardDataToClient(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        data: String
    ) {
        // The client will read the data via READ requests
        // Notify that data is available and mark as connected
        Log.d(TAG, "Client requested board data, marking connected")
        _connectionState.value = BluetoothConnectionState.CONNECTED

        // Stop advertising since we have a client
        stopBleAdvertising()

        // Trigger the callback
        onClientConnectedCallback?.invoke()
    }

    private fun startBleAdvertising() {
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bleAdvertiser == null) {
            Log.w(TAG, "BLE advertiser not available on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true) // Clients need to connect via GATT
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(BLE_SERVICE_PARCEL_UUID)
            .build()

        try {
            bleAdvertiser?.startAdvertising(settings, data, bleAdvertiseCallback)
            Log.d(TAG, "Starting BLE advertising with UUID: $BLE_SERVICE_PARCEL_UUID")
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

    // ---- CLIENT SIDE: BLE Scan + GATT Client ----

    fun startDiscovery() {
        _connectionState.value = BluetoothConnectionState.DISCOVERING
        _errorMessage.value = null
        _discoveredGames.value = emptyList()
        _isScanning.value = true
        discoveredAddresses.clear()
        discoveredBleDevices.clear()

        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "BLE scanner not available")
            _errorMessage.value = "BLE scanner not available"
            _isScanning.value = false
            return
        }

        // Scan filter: only find devices advertising our service UUID
        val filter = ScanFilter.Builder()
            .setServiceUuid(BLE_SERVICE_PARCEL_UUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner?.startScan(listOf(filter), settings, bleScanCallback)
            Log.d(TAG, "BLE scan started, looking for service UUID: $BLE_SERVICE_PARCEL_UUID")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan: ${e.message}")
            _isScanning.value = false
            return
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

    // ---- CLIENT CONNECT via GATT ----

    /**
     * Connect to the host via BLE GATT, send JOIN request, and read the board data.
     * Returns the board JSON string, or null on failure.
     */
    suspend fun connectAndGetBoardData(address: String): String? = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = BluetoothConnectionState.CONNECTING
            _errorMessage.value = null

            // Stop BLE scan before connecting
            stopBleScan()

            val device = discoveredBleDevices[address]
                ?: bluetoothAdapter?.getRemoteDevice(address)

            if (device == null) {
                _connectionState.value = BluetoothConnectionState.ERROR
                _errorMessage.value = "Device not found"
                return@withContext null
            }

            Log.d(TAG, "Connecting to ${device.name} ($address) via BLE GATT...")

            val boardData = connectGattAndRead(device)

            if (boardData != null) {
                _connectedDeviceName.value = device.name ?: "Unknown Device"
                _connectionState.value = BluetoothConnectionState.CONNECTED
                Log.d(TAG, "Got board data (${boardData.length} chars) from ${device.name}")
            } else {
                _connectionState.value = BluetoothConnectionState.ERROR
                _errorMessage.value = "Failed to get board data"
            }

            return@withContext boardData
        } catch (e: Exception) {
            Log.e(TAG, "GATT connect error", e)
            _connectionState.value = BluetoothConnectionState.ERROR
            _errorMessage.value = "Connection failed: ${e.message}"
            return@withContext null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectGattAndRead(device: BluetoothDevice): String? {
        // Retry loop for the infamous status 133 GATT_ERROR
        for (attempt in 1..MAX_GATT_RETRIES) {
            Log.d(TAG, "GATT connection attempt $attempt/$MAX_GATT_RETRIES")

            // Small delay before connecting — helps Android BLE stack stabilize
            if (attempt > 1) {
                val backoff = 500L * attempt
                Log.d(TAG, "Waiting ${backoff}ms before retry...")
                delay(backoff)
            } else {
                delay(200) // Small delay even on first attempt
            }

            val result = attemptGattConnect(device)

            if (result != null) {
                return result // Success!
            }

            // If we got here with null, it was a 133 or similar transient error
            Log.w(TAG, "GATT attempt $attempt failed, ${if (attempt < MAX_GATT_RETRIES) "retrying..." else "giving up."}")
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun attemptGattConnect(device: BluetoothDevice): String? {
        return withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine { continuation ->
                var resumed = false

                val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, "GATT connected, discovering services...")
                            gatt?.discoverServices()
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d(TAG, "GATT disconnected (status=$status)")
                            gatt?.close()
                            gattClient = null
                            if (!resumed) {
                                resumed = true
                                continuation.resume(null)
                            }
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, "GATT services discovered")
                            val service = gatt?.getService(BLE_SERVICE_UUID)
                            val characteristic = service?.getCharacteristic(BOARD_CHAR_UUID)

                            if (characteristic != null) {
                                // Write "JOIN" to the characteristic to request board data
                                characteristic.setValue("JOIN".toByteArray(Charsets.UTF_8))
                                val writeStarted = gatt.writeCharacteristic(characteristic)
                                Log.d(TAG, "Write JOIN started: $writeStarted")
                            } else {
                                Log.e(TAG, "Board characteristic not found")
                                if (!resumed) {
                                    resumed = true
                                    gatt?.disconnect()
                                    gatt?.close()
                                    gattClient = null
                                    continuation.resume(null)
                                }
                            }
                        } else {
                            Log.e(TAG, "Service discovery failed: $status")
                            if (!resumed) {
                                resumed = true
                                gatt?.disconnect()
                                gatt?.close()
                                gattClient = null
                                continuation.resume(null)
                            }
                        }
                    }

                    override fun onCharacteristicWrite(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS && characteristic?.uuid == BOARD_CHAR_UUID) {
                            Log.d(TAG, "JOIN written successfully, now reading board data...")
                            // Read the board data from the characteristic
                            gatt?.readCharacteristic(characteristic)
                        } else {
                            Log.e(TAG, "Write failed: $status")
                            if (!resumed) {
                                resumed = true
                                gatt?.disconnect()
                                gatt?.close()
                                gattClient = null
                                continuation.resume(null)
                            }
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS && characteristic?.uuid == BOARD_CHAR_UUID) {
                            val data = characteristic?.value?.let { String(it, Charsets.UTF_8) }
                            Log.d(TAG, "Read board data: ${data?.take(100)}...")

                            // Disconnect after reading
                            gatt?.disconnect()
                            gatt?.close()
                            gattClient = null

                            if (!resumed) {
                                resumed = true
                                continuation.resume(data)
                            }
                        } else {
                            Log.e(TAG, "Read failed: $status")
                            if (!resumed) {
                                resumed = true
                                gatt?.disconnect()
                                gatt?.close()
                                gattClient = null
                                continuation.resume(null)
                            }
                        }
                    }
                }, BluetoothDevice.TRANSPORT_LE)

                gattClient = gatt

                continuation.invokeOnCancellation {
                    gatt?.disconnect()
                    gatt?.close()
                    gattClient = null
                }
            }
        }
    }

    // Keep these for ViewModel compatibility (but they're no longer used for RFCOMM)
    suspend fun sendMessage(message: String): Boolean {
        // Board data is now sent via GATT server read requests
        // This is kept for API compatibility but the data is set via setBoardData()
        return true
    }

    suspend fun readMessage(): String? {
        // Board data is now received via GATT client read
        // This is kept for API compatibility
        return null
    }

    // ---- CLEANUP ----

    fun disconnect() {
        serverJob?.cancel()
        serverJob = null

        stopBleAdvertising()

        try {
            gattServer?.close()
            gattClient?.disconnect()
            gattClient?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }

        gattServer = null
        gattClient = null
        boardDataToSend = null
        onClientConnectedCallback = null

        _connectionState.value = BluetoothConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
        _errorMessage.value = null
    }

    fun cleanup() {
        stopDiscovery()
        disconnect()
    }
}
