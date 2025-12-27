package com.raven.recorder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DetectionActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DetectionActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.launch(Dispatchers.IO) {
            val bestMethod = detectBestMethod()
            getSharedPreferences("raven", MODE_PRIVATE).edit()
                .putInt("record_method", bestMethod)
                .apply()

            startService(Intent(this@DetectionActivity, CallRecordService::class.java))
            finishAffinity()
        }
    }

    private fun detectBestMethod(): Int {
        // Priority: 0 = Telecom (best), 1 = MediaProjection, 2 = MIC only
        if (isDefaultDialer() && testVoiceCommunication()) return 0
        if (testMediaProjection()) return 1
        return 2
    }

    private fun isDefaultDialer(): Boolean {
        // Simplified check – in real app use TelecomManager.isDefaultDialer
        return true // Placeholder – implement full check
    }

    private fun testVoiceCommunication(): Boolean = true // Test logic
    private fun testMediaProjection(): Boolean = true // Test logic
}