package com.example.compareride

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RideAutomatorService : AccessibilityService() {

    // Keep track of the last scraped prices to avoid spamming the UI
    private var lastScrapedPrices = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We trigger on window content changes
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            // Search ALL active windows on screen. 
            // We ignore package names because the floating widget obscures it.
            for (window in windows) {
                val rootNode = window.root ?: continue
                
                // Aggressively hunt for prices
                val prices = extractUberPrices(rootNode)
                val pricesStr = prices.joinToString("\n")
                
                // If we found prices and they are new, broadcast them!
                if (prices.isNotEmpty() && pricesStr != lastScrapedPrices) {
                    lastScrapedPrices = pricesStr
                    Log.d("RideScraper", "Found Uber Prices:\n$pricesStr")
                    
                    val intent = Intent("com.example.compareride.PRICES_UPDATED")
                    intent.putExtra("UBER_PRICES", pricesStr)
                    sendBroadcast(intent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun extractUberPrices(node: AccessibilityNodeInfo): List<String> {
        val prices = mutableListOf<String>()
        
        fun traverse(n: AccessibilityNodeInfo) {
            val text = n.text?.toString() ?: ""
            val desc = n.contentDescription?.toString() ?: ""
            val combined = "$text $desc".trim()

            // Look for the Rupee symbol and numbers
            if (combined.contains("₹")) {
                // Filter out massive UI descriptions, keep just the short price/ride description
                if (combined.length in 2..100) {
                    // Clean up newlines for a cleaner display, and filter out irrelevant buttons
                    val cleanText = combined.replace("\n", " - ")
                    if (!cleanText.lowercase().contains("cash") && !cleanText.lowercase().contains("choose")) {
                        prices.add(cleanText)
                    }
                }
            }

            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { traverse(it) }
            }
        }
        
        traverse(node)
        
        // Remove duplicates and return
        return prices.distinct()
    }
    
    override fun onInterrupt() {}
}