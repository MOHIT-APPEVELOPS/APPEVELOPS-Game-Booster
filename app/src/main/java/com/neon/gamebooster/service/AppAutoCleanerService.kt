package com.neon.gamebooster.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppAutoCleanerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val rootNode = rootInActiveWindow ?: return

        // Auto click "Force stop" or "Clear cache" buttons in system App Info page
        clickButtonByText(rootNode, "Force stop")
        clickButtonByText(rootNode, "FORCE STOP")
        clickButtonByText(rootNode, "OK")
        clickButtonByText(rootNode, "Clear cache")

        rootNode.recycle()
    }

    private fun clickButtonByText(nodeInfo: AccessibilityNodeInfo, text: String): Boolean {
        val list = nodeInfo.findAccessibilityNodeInfosByText(text)
        for (node in list) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
