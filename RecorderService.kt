package com.raven.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class RecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var enableBluetoothSpoof = false

    private val telephonyManager: TelephonyManager by lazy { getSystemService(TELEPHONY_SERVICE) as TelephonyManager }
    private val audioManager: AudioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enableBluetoothSpoof = intent?.getBooleanExtra("enable_bluetooth_spoof", false) ?: false

        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("raven_channel", "Raven Recorder", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        // Start foreground
        val notification = NotificationCompat.Builder(this, "raven_channel")
            .setContentTitle("Raven Recorder Running")
            .setContentText("Listening for calls")
            .setSmallIcon(android.R.drawable.ic_menu_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        return START_STICKY
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> startRecording()
                TelephonyManager.CALL_STATE_IDLE -> stopRecording()
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return

        if (enableBluetoothSpoof) {
            audioManager.isBluetoothScoOn = true
            audioManager.startBluetoothSco()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(getExternalFilesDir(null), "Raven_$timeStamp.m4a")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                Toast.makeText(this@RecorderService, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        isRecording = false

        if (enableBluetoothSpoof) {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
        }

        // Encrypt the last file (simple AES example – use real key in prod)
        val lastFile = getExternalFilesDir(null)?.listFiles()?.lastOrNull { it.name.startsWith("Raven_") && it.name.endsWith(".m4a") }
        lastFile?.let { encryptFile(it) }

        Toast.makeText(this, "Recording saved & encrypted", Toast.LENGTH_SHORT).show()
    }

    private fun encryptFile(file: File) {
        val key = "1234567890123456".toByteArray() // 16-byte key (use secure key in real app)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val input = FileInputStream(file)
        val encryptedFile = File(file.parent, file.name + ".enc")
        val output = FileOutputStream(encryptedFile)

        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            output.write(encrypted)
        }
        val final = cipher.doFinal()
        output.write(final)

        input.close()
        output.close()
        file.delete() // Delete original after encryption
    }

    override fun onDestroy() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        stopRecording()
        super.onDestroy()
    }
}