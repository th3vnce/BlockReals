package com.vnce.blockreals

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ReelsAccessibilityService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences
    private var blockSuspendedUntil = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        blockSuspendedUntil = 0L
        Log.w(TAG, "Service connected! Waiting for Instagram...")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val debugMode = prefs.getBoolean(KEY_DEBUG_MODE, false)
        val currentPackage = event.packageName?.toString() ?: "unknown"

        if (!currentPackage.contains("instagram", ignoreCase = true) &&
            !currentPackage.equals(INSTAGRAM_PACKAGE, ignoreCase = true)) {
            return
        }

        val root = event.source ?: rootInActiveWindow ?: return
        val fullScreenRoot = rootInActiveWindow ?: root
        val homeNode = findHomeTab(fullScreenRoot)
        val isBottomNavVisible = (homeNode != null)

        val isOnHomeTab = homeNode?.isSelected == true
        val match = if (!isOnHomeTab) findReelsIndicator(root, isBottomNavVisible) else null

        // --- Smart Suspension Logic ---
        if (blockSuspendedUntil > System.currentTimeMillis()) {
            if (match == null) {
                // We are safely out of Reels -> Re-arm the blocker instantly!
                Log.w(TAG, "Suspension lifted early – safely out of Reels.")
                blockSuspendedUntil = 0L
            } else {
                return // Still transitioning out of Reels, ignore event
            }
        }

        when {
            match != null && debugMode -> Log.w(TAG, "MATCH FOUND (debug mode, not blocking): $match")
            match != null -> blockReels(homeNode, match)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    private fun blockReels(homeNode: AccessibilityNodeInfo?, reason: String) {
        Log.w(TAG, "BLOCKING REELS: $reason")

        var redirected = false
        if (homeNode != null && performClick(homeNode)) {
            Log.w(TAG, "Successfully redirected to Home tab instead of exiting app.")
            redirected = true
        } else {
            Log.w(TAG, "Home tab nav button not found, falling back to Back action.")
            performGlobalAction(GLOBAL_ACTION_BACK)
            redirected = true
        }

        if (redirected) {
            // Apply the smart suspension (it will cancel itself the moment you hit the Home feed)
            blockSuspendedUntil = System.currentTimeMillis() + SUSPENSION_DURATION_MS
        }
    }

    private fun findHomeTab(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase()
        if (desc == "home") {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findHomeTab(child)
            if (result != null) return result
        }
        return null
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    private fun findReelsIndicator(node: AccessibilityNodeInfo, isBottomNavVisible: Boolean): String? {
        val desc = node.contentDescription?.toString()?.lowercase()

        // 1. Catch the Bottom Nav Reels Tab ONLY when it is actively selected
        if (desc != null && (desc == "reels" || desc == "reels tab" || desc == "reel")) {
            node.refresh()
            if (node.isSelected) {
                return "Reels Tab (Selected)"
            }
        }

        // 2. Catch the full-screen Reel video player ONLY if the bottom nav is hidden
        if (!isBottomNavVisible) {
            if (desc != null && desc.contains("double-tap to play") && desc.contains("reel")) {
                return "Fullscreen Reel Player"
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findReelsIndicator(child, isBottomNavVisible)
            if (result != null) return result
        }

        return null
    }

    companion object {
        private const val TAG = "ReelsBlocker"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val SUSPENSION_DURATION_MS = 2000L

        const val PREFS_NAME = "reels_blocker_prefs"
        const val KEY_DEBUG_MODE = "debug_mode"
    }
}