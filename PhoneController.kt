package com.hermes.agent.agent

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import java.util.Calendar

// ══════════════════════════════════════════════════════════════
//  PhoneController — Basic phone functions (no root needed)
// ══════════════════════════════════════════════════════════════

class PhoneController(private val context: Context) {

    companion object {
        private const val TAG = "PhoneController"
    }

    // ──────────────────────────────────────────────
    //  📞 Call करो
    // ──────────────────────────────────────────────
    fun makeCall(numberOrName: String) {
        try {
            val number = if (numberOrName.matches(Regex("[0-9+\\-\\s()]+"))) {
                numberOrName
            } else {
                // Contact name से number ढूंढो
                findNumberByName(numberOrName) ?: numberOrName
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${number.replace(" ", "")}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Calling: $number")
        } catch (e: Exception) {
            Log.e(TAG, "Call failed", e)
        }
    }

    private fun findNumberByName(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$name%")

        context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val colIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (colIdx >= 0) return cursor.getString(colIdx)
            }
        }
        return null
    }

    // ──────────────────────────────────────────────
    //  💬 SMS भेजो
    // ──────────────────────────────────────────────
    fun sendSMS(number: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
            Log.d(TAG, "SMS sent to $number")
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  🔦 Flashlight
    // ──────────────────────────────────────────────
    fun toggleFlashlight(on: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, on)
            Log.d(TAG, "Flashlight: ${if (on) "ON" else "OFF"}")
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  🔊 Volume Control
    // ──────────────────────────────────────────────
    fun setVolume(percent: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val volume = (maxVolume * percent / 100)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)

            // Ring volume भी set करो
            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val ringVol = (maxRing * percent / 100)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, ringVol, 0)

            Log.d(TAG, "Volume set to $percent%")
        } catch (e: Exception) {
            Log.e(TAG, "Volume failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  ⏰ Alarm Set करो
    // ──────────────────────────────────────────────
    fun setAlarm(hour: Int, minute: Int, label: String) {
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, label)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Alarm set for $hour:$minute - $label")
        } catch (e: Exception) {
            Log.e(TAG, "Alarm failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  📱 App खोलो
    // ──────────────────────────────────────────────
    fun openApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: throw Exception("App not found: $packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Log.d(TAG, "Opened app: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Open app failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  🔔 Notifications पढ़ो
    // ──────────────────────────────────────────────
    fun readNotifications(): List<String> {
        // NotificationListenerService से मिलेंगी
        return HermesNotificationListener.getNotifications()
    }
}
