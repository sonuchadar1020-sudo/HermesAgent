package com.hermes.agent.agent

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

// ══════════════════════════════════════════════════════════════
//  HermesNotificationListener
//  सभी notifications पढ़ता है
// ══════════════════════════════════════════════════════════════

class HermesNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifListener"
        private val notificationCache = mutableListOf<String>()

        fun getNotifications(): List<String> = notificationCache.toList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val title = it.notification.extras.getString("android.title") ?: "Unknown"
            val text = it.notification.extras.getString("android.text") ?: ""
            val app = it.packageName

            val notif = "[$app] $title: $text"
            notificationCache.add(0, notif) // नई notification पहले
            if (notificationCache.size > 50) notificationCache.removeAt(50) // Max 50

            Log.d(TAG, "New notification: $notif")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification dismiss हुई
    }
}
