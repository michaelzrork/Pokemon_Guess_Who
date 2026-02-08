package com.example.pokemonguesswho.network.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.UUID

class BluetoothConnectionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012")
        private const val SERVICE_NAME = "PokemonGuessWho"
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun startListening(): BluetoothServerSocket? {
        return try {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                SERVICE_NAME,
                SERVICE_UUID
            )
            serverSocket
        } catch (e: IOException) {
            Log.e(TAG, "Listen failed", e)
            null
        }
    }
    
    fun acceptConnection(): BluetoothSocket? {
        return try {
            serverSocket?.accept()
        } catch (e: IOException) {
            Log.e(TAG, "Accept failed", e)
            null
        }
    }
    
    fun stopListening() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close server socket", e)
        }
    }
}
