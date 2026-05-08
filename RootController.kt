package com.hermes.agent.agent

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ══════════════════════════════════════════════════════════════
//  RootController — Root (su) से system control
//  Requires: Magisk / SuperSU / KernelSU
// ══════════════════════════════════════════════════════════════

class RootController(private val context: Context) {

    companion object {
        private const val TAG = "RootController"

        // libsu को initialize करो
        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }

    // ──────────────────────────────────────────────
    //  Root available है?
    // ──────────────────────────────────────────────
    fun isRootAvailable(): Boolean {
        return Shell.isAppGrantedRoot() == true
    }

    // ──────────────────────────────────────────────
    //  📶 WiFi Toggle (Root से)
    // ──────────────────────────────────────────────
    fun toggleWifi(enable: Boolean) {
        try {
            val state = if (enable) "enable" else "disable"
            runRoot("svc wifi $state")
            Log.d(TAG, "WiFi ${if (enable) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "WiFi toggle failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  🔵 Bluetooth Toggle
    // ──────────────────────────────────────────────
    fun toggleBluetooth(enable: Boolean) {
        try {
            val state = if (enable) "enable" else "disable"
            runRoot("svc bluetooth $state")
            Log.d(TAG, "Bluetooth ${if (enable) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "BT toggle failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  📸 Screenshot लो
    // ──────────────────────────────────────────────
    fun takeScreenshot(): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val path = "/sdcard/Pictures/hermes_screenshot_$timestamp.png"
            runRoot("screencap -p $path")
            Log.d(TAG, "Screenshot saved: $path")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot failed", e)
            null
        }
    }

    // ──────────────────────────────────────────────
    //  ☀️ Brightness Set करो (0-255)
    // ──────────────────────────────────────────────
    fun setBrightness(level: Int) {
        try {
            val clamped = level.coerceIn(0, 255)
            runRoot("settings put system screen_brightness $clamped")
            // Auto-brightness बंद करो
            runRoot("settings put system screen_brightness_mode 0")
            Log.d(TAG, "Brightness set to $clamped")
        } catch (e: Exception) {
            Log.e(TAG, "Brightness failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  📴 Mobile Data Toggle
    // ──────────────────────────────────────────────
    fun toggleMobileData(enable: Boolean) {
        try {
            val state = if (enable) "enable" else "disable"
            runRoot("svc data $state")
        } catch (e: Exception) {
            Log.e(TAG, "Mobile data toggle failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  🔇 DND / Silent Mode
    // ──────────────────────────────────────────────
    fun setRingerMode(mode: String) {
        try {
            val modeCode = when (mode.lowercase()) {
                "silent" -> 0
                "vibrate" -> 1
                "normal" -> 2
                else -> 2
            }
            runRoot("media volume --set $modeCode --stream 2")
        } catch (e: Exception) {
            Log.e(TAG, "Ringer mode failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  ⚡ Custom Shell Command
    // ──────────────────────────────────────────────
    fun runShellCommand(command: String): String {
        return try {
            val result = Shell.cmd(command).exec()
            val output = result.out.joinToString("\n")
            Log.d(TAG, "Shell [$command]: $output")
            output
        } catch (e: Exception) {
            Log.e(TAG, "Shell command failed: $command", e)
            "Error: ${e.message}"
        }
    }

    // ──────────────────────────────────────────────
    //  📱 Installed Apps List
    // ──────────────────────────────────────────────
    fun getInstalledApps(): String {
        return runRoot("pm list packages -3") // Third-party apps
    }

    // ──────────────────────────────────────────────
    //  🔋 Battery Info
    // ──────────────────────────────────────────────
    fun getBatteryInfo(): String {
        return runRoot("dumpsys battery | grep -E 'level|status|health|temperature'")
    }

    // ──────────────────────────────────────────────
    //  Private: Root command चलाओ
    // ──────────────────────────────────────────────
    private fun runRoot(command: String): String {
        val result = Shell.cmd(command).exec()
        return result.out.joinToString("\n")
    }
}
