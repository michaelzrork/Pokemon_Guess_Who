package com.example.pokemonguesswho.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
        private val SERVICE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        private const val SERVICE_NAME = "PokemonGuessWho"
        private const val PROBE_TIMEOUT_MS = 8000L
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

    // These are ONLY devices confirmed to be hosting a Pokemon Guess Who game
    private val _discoveredGames = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredGames.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Coroutine scope for probe jobs — using thread-safe collections
    private val probeScope = CoroutineScope(Dispatchers.IO)
    private val probeJobs = CopyOnWriteArrayList<Job>()

    // Track addresses we've already probed — thread-safe set
    private val probedAddresses = ConcurrentHashMap.newKeySet<String>()

    // Server job so we can cancel it cleanly
    private var serverJob: Job? = null

    // Flag to ignore premature ACTION_DISCOVERY_FINISHED from cancelDiscovery()
    private val discoveryStarted = AtomicBoolean(false)

    // BroadcastReceiver for BT discovery - finds devices, then we probe them
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        Log.d(TAG, "Discovery found device: ${it.name} (${it.address})")
                        probeDevice(it)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Ignore if this is a premature event from cancelDiscovery()
                    if (!discoveryStarted.compareAndSet(true, false)) {
                        Log.d(TAG, "Ignoring premature DISCOVERY_FINISHED")
                        return
                    }
                    Log.d(TAG, "BT discovery scan finished, waiting for probes to complete...")
                    probeScope.launch {
                        // Take a snapshot of current jobs and wait for them
                        val currentJobs = probeJobs.toList()
                        currentJobs.forEach { it.join() }
                        Log.d(TAG, "All probes completed. Found ${_discoveredGames.value.size} hosted games")
                        _isScanning.value = false
                    }
                }
            }
        }
    }

    private var isReceiverRegistered = false

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    // ---- SERVER SIDE (Host) ----

    /**
     * Start listening for incoming connections (host side).
     * Uses listenUsingInsecureRfcommWithServiceRecord so that unpaired devices
     * can also connect (no pairing prompt needed for the probe or the real join).
     */
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

                Log.d(TAG, "Server listening for connections (insecure RFCOMM)...")

                // Accept connections in a loop. Probe connections will connect
                // and immediately disconnect. The real client will send a handshake.
                while (true) {
                    val socket: BluetoothSocket
                    try {
                        socket = serverSocket?.accept() ?: break
                    } catch (e: IOException) {
                        // Server socket was closed (e.g. user cancelled)
                        if (_connectionState.value == BluetoothConnectionState.LISTENING) {
                            Log.d(TAG, "Server socket closed while listening")
                        }
                        break
                    }

                    Log.d(TAG, "Accepted connection from: ${socket.remoteDevice?.name}")

                    try {
                        val reader = BufferedReader(InputStreamReader(socket.inputStream))

                        // Use a timeout to distinguish probes from real clients
                        val handshake = withTimeoutOrNull(3000L) {
                            try {
                                reader.readLine()
                            } catch (e: IOException) {
                                null
                            }
                        }

                        if (handshake == "POKEMON_GUESS_WHO_JOIN") {
                            // This is a real client!
                            connectedSocket = socket
                            inputReader = reader
                            outputStream = socket.outputStream
                            _connectedDeviceName.value = socket.remoteDevice?.name ?: "Unknown Device"
                            _connectionState.value = BluetoothConnectionState.CONNECTED
                            Log.d(TAG, "Real client connected: ${socket.remoteDevice?.name}")

                            // Close server socket - we have our player
                            try {
                                serverSocket?.close()
                            } catch (_: IOException) {}
                            serverSocket = null

                            onConnected()
                            return@launch
                        } else {
                            Log.d(TAG, "Probe/invalid connection from ${socket.remoteDevice?.name}, continuing to listen...")
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

    // ---- CLIENT SIDE (Joiner) ----

    /**
     * Scan for nearby devices and probe each one for our game service.
     * Only devices running a hosted Pokemon Guess Who game will appear in discoveredDevices.
     */
    fun startDiscovery() {
        _connectionState.value = BluetoothConnectionState.DISCOVERING
        _errorMessage.value = null
        _discoveredGames.value = emptyList()
        _isScanning.value = true
        probedAddresses.clear()

        // Cancel any existing probe jobs
        probeJobs.forEach { it.cancel() }
        probeJobs.clear()

        // Register receiver
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(discoveryReceiver, filter)
            isReceiverRegistered = true
        }

        // Cancel any existing discovery first, but don't set our flag yet
        // (this prevents the premature DISCOVERY_FINISHED from being processed)
        discoveryStarted.set(false)
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }

        // Also probe bonded (paired) devices — they can be found without discovery
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            probeDevice(device)
        }

        // Small delay to let any pending DISCOVERY_FINISHED from cancelDiscovery() fire
        probeScope.launch {
            delay(200)

            // Now set the flag and start fresh discovery
            discoveryStarted.set(true)
            val started = bluetoothAdapter?.startDiscovery() ?: false
            Log.d(TAG, "Started discovery: $started, probing bonded devices too")

            if (!started) {
                Log.e(TAG, "Failed to start Bluetooth discovery!")
                _isScanning.value = false
            }
        }
    }

    /**
     * Probe a single device to check if it's running our game service.
     * Attempts a quick RFCOMM connection with our UUID — if it connects,
     * the device is hosting a game. We immediately close the probe socket.
     */
    private fun probeDevice(device: BluetoothDevice) {
        val address = device.address ?: return
        val name = device.name ?: device.address ?: return

        // Don't probe the same device twice (thread-safe check)
        if (!probedAddresses.add(address)) return

        val job = probeScope.launch {
            try {
                Log.d(TAG, "Probing $name ($address) for game service...")

                val result = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                    try {
                        // Use insecure connection so no pairing prompt is needed for the probe
                        val probeSocket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)
                        probeSocket.connect()
                        // Success! This device is running our service.
                        probeSocket.close()
                        true
                    } catch (e: IOException) {
                        false
                    }
                }

                if (result == true) {
                    Log.d(TAG, "FOUND HOSTED GAME on $name ($address)")
                    addHostedGame(name, address)
                } else {
                    Log.d(TAG, "No game on $name ($address)")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Probe failed for $name: ${e.message}")
            }
        }
        probeJobs.add(job)
    }

    private fun addHostedGame(name: String, address: String) {
        val currentGames = _discoveredGames.value.toMutableList()
        if (currentGames.none { it.address == address }) {
            currentGames.add(BluetoothDeviceInfo(name = name, address = address))
            _discoveredGames.value = currentGames
        }
    }

    fun stopDiscovery() {
        discoveryStarted.set(false)
        bluetoothAdapter?.cancelDiscovery()
        probeJobs.forEach { it.cancel() }
        probeJobs.clear()
        _isScanning.value = false
    }

    /**
     * Connect to a host device for real (client side).
     * Sends a handshake message so the host knows this is a real client (not a probe).
     */
    suspend fun connectToDevice(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = BluetoothConnectionState.CONNECTING
            _errorMessage.value = null

            bluetoothAdapter?.cancelDiscovery()

            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device == null) {
                _connectionState.value = BluetoothConnectionState.ERROR
                _errorMessage.value = "Device not found"
                return@withContext false
            }

            // Use insecure connection to avoid pairing prompt
            clientSocket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)

            Log.d(TAG, "Connecting to ${device.name}...")
            clientSocket?.connect()

            clientSocket?.let { socket ->
                connectedSocket = socket
                setupStreams(socket)
                _connectedDeviceName.value = device.name ?: "Unknown Device"

                // Send handshake so the server knows we're a real client
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

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Unregister receiver error", e)
            }
            isReceiverRegistered = false
        }
    }
}
