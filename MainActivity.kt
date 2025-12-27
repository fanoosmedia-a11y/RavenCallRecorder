package com.raven.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.BLUETOOTH
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggle = findViewById<Button>(R.id.btnToggleRecording)
        val switchBluetooth = findViewById<Switch>(R.id.switchBluetoothSpoof)

        if (checkPermissions()) {
            // Load Bluetooth toggle state (use SharedPrefs in real app)
            switchBluetooth.isChecked = false // Default off
            switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
                // Pass to service via intent extra
                val intent = Intent(this, RecorderService::class.java)
                intent.putExtra("enable_bluetooth_spoof", isChecked)
                startForegroundService(intent)
            }

            btnToggle.setOnClickListener {
                val intent = Intent(this, RecorderService::class.java)
                startForegroundService(intent) // Or stopService for full toggle
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    private fun checkPermissions(): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // Permissions granted – restart activity or start service
        }
    }
}