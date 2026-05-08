package com.hermes.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Phone rebooted — starting Hermes Agent")
            val serviceIntent = Intent(context, HermesAgentService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
