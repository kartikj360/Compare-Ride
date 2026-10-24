package com.example.compareride

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RideAutomatorService : AccessibilityService() {

    // The stages of our automation
    enum class AppState {
        IDLE,
        CLICKED_HOME_BUTTON,
        CLICKED_DROP_LOCATION,
        TYPING_DESTINATION
    }

    private var currentState = AppState.IDLE

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            when (currentState) {
                AppState.IDLE -> {
                    if (findAndTapNodeByTextOrDesc(rootNode, "Where do you want to go?")) {
                        Log.d("RideScraper", "STEP 1: Tapped Home Button")
                        currentState = AppState.CLICKED_HOME_BUTTON
                    }
                }
                AppState.CLICKED_HOME_BUTTON -> {
                    // Try to find the "Drop Location" text/box and tap it
                    if (findAndTapNodeByTextOrDesc(rootNode, "Drop Location")) {
                        Log.d("RideScraper", "STEP 2: Tapped Drop Location box")
                        currentState = AppState.CLICKED_DROP_LOCATION
                    } else {
                        // Keep scanning to see what it is actually called if not "Drop Location"
                        printAllNodes(rootNode, "")
                    }
                }
                AppState.CLICKED_DROP_LOCATION -> {
                    // Step 3: Now the keyboard is up, find the actual typing box (EditText)
                    if (injectTextIntoEditText(rootNode, "Airport")) {
                        Log.d("RideScraper", "STEP 3: Successfully typed destination!")
                        currentState = AppState.TYPING_DESTINATION
                    } else {
                        printAllNodes(rootNode, "")
                    }
                }
                AppState.TYPING_DESTINATION -> {
                    // Done for now! Next we will click the first search result.
                }
            }
        }
    }

    // Helper: Finds a node matching text/desc and uses the Robotic Finger to tap it
    private fun findAndTapNodeByTextOrDesc(node: AccessibilityNodeInfo, targetText: String): Boolean {
        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""

        if (desc.contains(targetText, ignoreCase = true) || text.contains(targetText, ignoreCase = true)) {
            performTap(node)
            return true
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                if (findAndTapNodeByTextOrDesc(it, targetText)) return true
            }
        }
        return false
    }

    // Helper: Finds the active text box and pastes our destination into it
    private fun injectTextIntoEditText(node: AccessibilityNodeInfo, destination: String): Boolean {
        // Find the box that allows text entry
        if (node.className?.toString()?.contains("EditText") == true) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, destination)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            return true
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                if (injectTextIntoEditText(it, destination)) return true
            }
        }
        return false
    }

    private fun performTap(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // If the box has no size, we can't tap it
        if (rect.isEmpty) return

        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        dispatchGesture(gesture, null, null)
    }

    private fun printAllNodes(node: AccessibilityNodeInfo, indent: String) {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val id = node.viewIdResourceName
        val className = node.className

        if (!text.isNullOrBlank() || !desc.isNullOrBlank() || id != null) {
            Log.d("RideScraper", "$indent[Class: $className] | Text: '$text' | Desc: '$desc' | ID: '$id'")
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { printAllNodes(it, "$indent  ") }
        }
    }

    override fun onInterrupt() {}
}