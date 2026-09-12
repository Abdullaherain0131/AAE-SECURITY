package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AAEA11yService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Zero-Click Shield: Monitor WhatsApp, SMS, Telegram for suspicious links
        val source = event.source ?: return
        
        when (event.packageName?.toString()) {
            "com.whatsapp", "org.telegram.messenger", "com.android.mms" -> {
                scanNodeForLinks(source)
            }
        }
    }

    private fun scanNodeForLinks(node: AccessibilityNodeInfo) {
        if (node.text != null) {
            val text = node.text.toString()
            if (text.contains("http://") || text.contains("https://")) {
                Log.d("AAEA11yService", "Found link in text: $text")
                // Basic check for zero-click malicious patterns or known bad domains
                if (text.contains(".xyz") || text.contains("free-money") || text.contains("bit.ly")) {
                    Log.w("AAEA11yService", "Suspicious link detected: $text")
                    // In a real scenario, we could show an overlay alert using SYSTEM_ALERT_WINDOW
                }
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanNodeForLinks(child)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.w("AAEA11yService", "Accessibility Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AAEA11yService", "Accessibility Service Connected. Zero-Click Shield Active.")
    }
}
