package com.raven.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.telephony.TelephonyManager
import java.io.File

class CallRecordService : Service() {

    private var recorder: MediaRecorder? = null
    private val prefs by lazy { getSharedPreferences("raven", MODE_PRIVATE) }
    private val method by lazy { prefs.getInt("record_method", 2) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, buildNotification())
        registerPhoneListener()
    }

    private fun registerPhoneListener() {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        tm.listen({ state, _ ->
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> startRecording()
                TelephonyManager.CALL_STATE_IDLE -> stopRecording()
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(when (method) {
                0 -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
                1 -> MediaRecorder.AudioSource.VOICE_CALL
                else -> MediaRecorder.AudioSource.MIC
            })
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(getFilePath())
            try {
                prepare()
                start()
            } catch (e: Exception) {
                // Fallback or log
            }
        }
    }

    private fun getFilePath(): String {
        val dir = File(getExternalFilesDir(null), "RavenCalls")
        dir.mkdirs()
        return File(dir, "call_${System.currentTimeMillis()}.m4a").absolutePath
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) { /* already stopped */ }
        recorder?.release()
        recorder = null
    }

    private fun createChannel() {
        val channel = NotificationChannel("raven_chan", "Raven Calls", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, "raven_chan")
            .setContentTitle("Raven Recorder")
            .setContentText("Monitoring calls")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}

private lateinit var phoneStateHelper: PhoneStateHelper

override fun onCreate() {
    super.onCreate()
    phoneStateHelper = PhoneStateHelper(this)

    phoneStateHelper.onCallActive = {
        startRecording()  // your recording start method
    }

    phoneStateHelper.onCallEnded = {
        stopRecording()   // your recording stop method
    }

    phoneStateHelper.startListening()
}

override fun onDestroy() {
    phoneStateHelper.stopListening()
    super.onDestroy()
}