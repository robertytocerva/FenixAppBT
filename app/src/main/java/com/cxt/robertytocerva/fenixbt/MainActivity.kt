package com.cxt.robertytocerva.fenixbt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

class MainActivity : AppCompatActivity() {

    // Vistas
    private lateinit var tvFuego: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvHum: TextView
    private lateinit var tvHumo: TextView
    private lateinit var btnConectar: Button

    // Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private var isConnected = false
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID Estándar para SPP

    // Permisos
    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvFuego = findViewById(R.id.tvFuego)
        tvTemp = findViewById(R.id.tvTemp)
        tvHum = findViewById(R.id.tvHum)
        tvHumo = findViewById(R.id.tvHumoValue)
        btnConectar = findViewById(R.id.btnConectar)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        btnConectar.setOnClickListener {
            if (checkPermissions()) {
                showPairedDevicesDialog()
            } else {
                requestPermissions()
            }
        }
        gradientAnimation()
    }

    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showPairedDevicesDialog()
            } else {
                Toast.makeText(this, "Se requieren permisos para usar Bluetooth", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPairedDevicesDialog() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceList = pairedDevices?.map { it.name }?.toTypedArray() ?: arrayOf()

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos vinculados", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona un dispositivo")
            .setItems(deviceList) { _, which ->
                val deviceName = deviceList[which]
                val device = pairedDevices?.find { it.name == deviceName }
                device?.let {
                    conectarDispositivo(it)
                }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun conectarDispositivo(device: BluetoothDevice) {
        Thread {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(uuid)
                btSocket?.connect()
                isConnected = true

                runOnUiThread {
                    Toast.makeText(this, "Conectado a ${device.name}", Toast.LENGTH_SHORT).show()
                    btnConectar.isEnabled = false
                }

                escucharDatos()
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnConectar.isEnabled = true
                }
            }
        }.start()
    }

    private fun escucharDatos() {
        val inputStream = btSocket?.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))

        while (isConnected) {
            try {
                val jsonString = reader.readLine()
                if (jsonString != null) {
                    runOnUiThread {
                        actualizarInterfaz(jsonString)
                    }
                }
            } catch (e: IOException) {
                isConnected = false
                break
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun actualizarInterfaz(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val fuego = json.getBoolean("fuegoDetectado")
            val temp = json.getDouble("temperaturaC")
            val hum = json.getDouble("humedadRelativa")
            val gas = json.getDouble("gasPpm")

            if (fuego) {
                tvFuego.text = "¡PELIGRO DETECTADO!"
                tvFuego.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            } else {
                tvFuego.text = "Normal"
                tvFuego.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }

            tvTemp.text = "$temp °C"
            tvHum.text = "$hum %"
            tvHumo.text = "$gas PPM"

        } catch (e: Exception) {
            Log.e("JSON_ERROR", "Error al leer JSON: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            btSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun gradientAnimation() {
        val layout = findViewById<ConstraintLayout>(R.id.main)
        val transition = layout.background as? TransitionDrawable

        var forward = true
        val handler = Handler(Looper.getMainLooper())
        val duration = 5000L // duración de cada transición

        val runnable = object : Runnable {
            override fun run() {
                if (forward) {
                    transition?.startTransition(duration.toInt())
                } else {
                    transition?.reverseTransition(duration.toInt())
                }
                forward = !forward
                handler.postDelayed(this, duration)
            }
        }

        handler.post(runnable)
    }
}
