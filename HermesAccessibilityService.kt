package com.hermes.agent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

// ══════════════════════════════════════════════════════════════
//  HermesAccessibilityService
//  UI automation: click, scroll, type — बिना root के भी
// ══════════════════════════════════════════════════════════════

class HermesAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "HermesAccessibility"
        private var instance: HermesAccessibilityService? = null

        fun getInstance() = instance

        // Screen पर tap करो
        fun tap(x: Float, y: Float) {
            instance?.performTap(x, y)
        }

        // Back button
        fun goBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        // Home button
        fun goHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // Recent apps
        fun showRecents() {
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        // Screen lock
        fun lockScreen() {
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        // Notifications shade
        fun openNotifications() {
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        // Quick Settings
        fun openQuickSettings() {
            instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        }
    }

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "Accessibility Service connected ✅")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events monitor कर सकते हैं
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ──────────────────────────────────────────────
    //  Tap gesture
    // ──────────────────────────────────────────────
    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // ──────────────────────────────────────────────
    //  Text में type करो
    // ──────────────────────────────────────────────
    fun typeText(text: String) {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        node?.let {
            val args = android.os.Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }

    // ──────────────────────────────────────────────
    //  Text से element ढूंढो और click करो
    // ──────────────────────────────────────────────
    fun findAndClick(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return if (nodes.isNotEmpty()) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } else false
    }

    // ──────────────────────────────────────────────
    //  Scroll down
    // ──────────────────────────────────────────────
    fun scrollDown() {
        val path = Path().apply {
            moveTo(540f, 1200f)
            lineTo(540f, 400f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
