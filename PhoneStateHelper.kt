package com.raven.recorder

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Modern (non-deprecated) helper for monitoring call state changes.
 * Uses TelephonyCallback instead of deprecated PhoneStateListener.
 */
class PhoneStateHelper(private val context: Context) {

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val executor: Executor = Executors.newSingleThreadExecutor()

    // Callback that reacts to call state changes
    private val callStateCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {

        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d(TAG, "Call state: IDLE (call ended or no call)")
                    // Notify service to STOP recording
                    onCallEnded?.invoke()
                }

                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d(TAG, "Call state: RINGING")
                    // Optional: handle incoming call ring
                }

                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d(TAG, "Call state: OFFHOOK (call active)")
                    // Notify service to START recording
                    onCallActive?.invoke()
                }
            }
        }
    }

    // Callbacks to notify the service/activity
    var onCallActive: (() -> Unit)? = null
    var onCallEnded: (() -> Unit)? = null

    companion object {
        private const val TAG = "PhoneStateHelper"
    }

    /**
     * Start listening for call state changes.
     * Call this in onCreate/onStartCommand of your service or activity.
     */
    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(executor, callStateCallback)
            Log.d(TAG, "Call state listening registered (modern API)")
        } else {
            // For older Android < 12, fallback to deprecated PhoneStateListener (optional)
            // But we recommend minSdk 31+ for simplicity
            Log.w(TAG, "Modern TelephonyCallback not available – consider raising minSdk")
        }
    }

    /**
     * Stop listening. Call this in onDestroy/onStop of service/activity.
     */
    fun stopListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.unregisterTelephonyCallback(callStateCallback)
            Log.d(TAG, "Call state listening unregistered")
        }
    }
}