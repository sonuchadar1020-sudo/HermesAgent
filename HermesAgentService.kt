package com.hermes.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hermes.agent.R

// ══════════════════════════════════════════════════════════════
//  HermesAgentService — Background Foreground Service
//  App बंद हो तो भी agent active रहे
// ══════════════════════════════════════════════════════════════

class HermesAgentService : Service() {

    companion object {
        private const val TAG = "HermesService"
        private const val CHANNEL_ID = "hermes_agent_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d(TAG, "Hermes Agent Service started 🚀")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Service crash हो तो restart हो
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hermes Agent",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Hermes AI Agent background service"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hermes Agent Active 🤖")
            .setContentText("हिंदी में बात करने के लिए tap करें")
            .setSmallIcon(R.drawable.ic_hermes)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
