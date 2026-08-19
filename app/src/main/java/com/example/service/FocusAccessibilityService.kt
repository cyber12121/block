package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.FocusGuardApp
import com.example.ui.BlockedOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastBlockedTimestamp: Long = 0
    private var lastBlockedTarget: String = ""

    // Battery optimization: throttle inspection on rapid identical events
    private var lastInspectedPackage: String = ""
    private var lastInspectedTime: Long = 0L

    // Comprehensive list of all popular, privacy, and alternative Android browsers
    private val knownBrowserPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "org.mozilla.fennec_aurora",
        "org.mozilla.focus",
        "org.mozilla.klar",
        "com.brave.browser",
        "com.brave.browser_beta",
        "com.brave.browser_nightly",
        "com.microsoft.emmx",
        "com.microsoft.emmx.canary",
        "com.microsoft.emmx.dev",
        "com.microsoft.emmx.beta",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.sec.android.app.sbrowser.lite",
        "com.opera.browser",
        "com.opera.browser.beta",
        "com.opera.mini.native",
        "com.opera.mini.native.beta",
        "com.opera.touch",
        "com.opera.gx",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.vivaldi.browser.snapshot",
        "org.torproject.torbrowser",
        "org.torproject.torbrowser_alpha",
        "com.kiwibrowser.browser",
        "mobi.mgeek.TunnyBrowser",
        "com.UCMobile.intl",
        "com.uc.browser.en",
        "com.ecosia.android",
        "com.cloudmosa.puffinFree",
        "mark.via.gp",
        "acr.browser.barebones",
        "com.yandex.browser",
        "com.aloha.browser",
        "org.bromite.bromite",
        "app.vanadium.browser"
    )

    // System Settings & Package Installers for Anti-Tamper & Anti-Uninstall
    private val systemSettingsPackages = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.samsung.android.packageinstaller"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as? FocusGuardApp
        app?.let {
            scope.launch {
                val sessionManager = FocusSessionManager.getInstance(this@FocusAccessibilityService)
                sessionManager.refreshBlockedTargetsCache(it.repository)
            }
        }
    }

    private fun isBrowserApp(packageName: String): Boolean {
        if (knownBrowserPackages.contains(packageName)) return true
        val lower = packageName.lowercase()
        return lower.contains("browser") ||
               lower.contains("chrome") ||
               lower.contains("firefox") ||
               lower.contains("brave") ||
               lower.contains("opera") ||
               lower.contains("webbrowser")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val targetPkg = event.packageName?.toString() ?: return

        val sessionManager = FocusSessionManager.getInstance(this)
        val sessionState = sessionManager.sessionState.value

        val app = application as? FocusGuardApp
        if (app != null) {
            scope.launch {
                sessionManager.refreshBlockedTargetsCache(app.repository)
            }
        }

        // Check if Minimal Launcher or Active Focus Session is active
        val isMinimalActive = sessionManager.isMinimalLauncherActive()
        
        // If Minimal Launcher or active session is running, prevent unauthorized launcher/app switches
        if (isMinimalActive || sessionState.isActive) {
            val isFgApp = targetPkg == applicationContext.packageName
            val essentialApps = sessionManager.getCustomEssentialApps().map { it.lowercase() }
            val isEssential = essentialApps.contains(targetPkg.lowercase()) || essentialApps.any { targetPkg.lowercase().contains(it) }
            
            // If user swiped to system home or opened non-essential app while Minimal Launcher is active, bring user back to FocusGuard!
            if (!isFgApp && !isEssential && (targetPkg.contains("launcher") || targetPkg.contains("home") || sessionManager.isAppBlocked(targetPkg))) {
                val relaunch = Intent(this, com.example.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("LAUNCH_MINIMAL_MODE", true)
                }
                startActivity(relaunch)
                return
            }
        }

        // Skip our own app, keyboards, and core system UI overlay to save CPU
        if (targetPkg == applicationContext.packageName ||
            targetPkg == "com.android.systemui" ||
            targetPkg.contains("inputmethod") ||
            targetPkg.contains("keyboard") ||
            targetPkg.contains("launcher")
        ) return

        val now = System.currentTimeMillis()
        // Throttle rapid sub-second identical window events
        if (targetPkg == lastInspectedPackage && (now - lastInspectedTime) < 200) {
            return
        }
        lastInspectedPackage = targetPkg
        lastInspectedTime = now

        // 1. Strict Mode Anti-Uninstall & Settings Tamper Protection
        if (sessionState.isStrictMode && systemSettingsPackages.contains(targetPkg)) {
            val rootNode = rootInActiveWindow ?: windows.firstOrNull { it.isActive }?.root
            if (rootNode != null) {
                val fullText = extractAllText(rootNode, maxDepth = 6, visitedCount = intArrayOf(0)).lowercase()
                val ourAppName = "focusguard".lowercase()
                val ourPkg = applicationContext.packageName.lowercase()

                val isAboutOurApp = fullText.contains(ourAppName) || fullText.contains(ourPkg) || fullText.contains("com.example")
                val isTamperAction = fullText.contains("uninstall") ||
                                     fullText.contains("force stop") ||
                                     fullText.contains("deactivate") ||
                                     fullText.contains("clear data") ||
                                     fullText.contains("clear storage") ||
                                     fullText.contains("disable") ||
                                     fullText.contains("device admin")

                if (isAboutOurApp && (isTamperAction || fullText.contains("app info") || fullText.contains("manage app"))) {
                    triggerBlockShield(
                        targetName = "Anti-Uninstall Defense",
                        reason = "FocusGuard cannot be uninstalled, force-stopped, or disabled while Strict Mode is active.",
                        isWebsite = false
                    )
                    return
                }
            }
        }

        // 2. Check if the entire app is in a blocked list (Fast O(1) hash lookup)
        val isBrowser = isBrowserApp(targetPkg)
        if (!isBrowser && sessionManager.isAppBlocked(targetPkg)) {
            triggerBlockShield(
                targetName = getReadableAppName(targetPkg),
                reason = "App '$targetPkg' is restricted in your active focus shield.",
                isWebsite = false
            )
            return
        }

        // 3. Inspect Browsers (Chrome, Brave, Firefox, Edge, Opera, Samsung, DuckDuckGo, etc.)
        if (isBrowser) {
            val rootNode = rootInActiveWindow ?: windows.firstOrNull { it.isActive }?.root
            if (rootNode != null) {
                val urlOrText = extractTextOrUrl(rootNode, maxDepth = 8, visitedCount = intArrayOf(0))
                if (urlOrText.isNotBlank()) {
                    val (isBlocked, matchedRule) = sessionManager.isUrlOrKeywordBlocked(urlOrText)
                    if (isBlocked) {
                        triggerBlockShield(
                            targetName = matchedRule,
                            reason = "Website '$matchedRule' is restricted.",
                            isWebsite = true
                        )
                        return
                    }
                }
            }

            // Also check event text if available
            val eventTexts = event.text.joinToString(" ")
            if (eventTexts.isNotBlank()) {
                val (isBlocked, matchedRule) = sessionManager.isUrlOrKeywordBlocked(eventTexts)
                if (isBlocked) {
                    triggerBlockShield(
                        targetName = matchedRule,
                        reason = "Website '$matchedRule' is restricted.",
                        isWebsite = true
                    )
                }
            }
        }
    }

    private fun getReadableAppName(packageName: String): String {
        return when {
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("tiktok") || packageName.contains("musically") -> "TikTok"
            packageName.contains("twitter") -> "X (Twitter)"
            packageName.contains("facebook") -> "Facebook"
            packageName.contains("netflix") -> "Netflix"
            packageName.contains("twitch") -> "Twitch"
            packageName.contains("reddit") -> "Reddit"
            packageName.contains("snapchat") -> "Snapchat"
            packageName.contains("discord") -> "Discord"
            packageName.contains("roblox") -> "Roblox"
            packageName.contains("pinterest") -> "Pinterest"
            packageName.contains("threads") -> "Threads"
            packageName.contains("tinder") -> "Tinder"
            packageName.contains("bumble") -> "Bumble"
            packageName.contains("primevideo") -> "Prime Video"
            packageName.contains("disney") -> "Disney+"
            else -> packageName
        }
    }

    private fun extractTextOrUrl(node: AccessibilityNodeInfo, maxDepth: Int, visitedCount: IntArray): String {
        if (maxDepth <= 0 || visitedCount[0] > 120) return ""
        visitedCount[0]++

        val sb = StringBuilder()

        val viewId = try { node.viewIdResourceName ?: "" } catch (_: Throwable) { "" }
        val isEditable = try { node.isEditable } catch (_: Throwable) { false }

        val isUrlField = viewId.contains("url", ignoreCase = true) ||
                viewId.contains("location", ignoreCase = true) ||
                viewId.contains("search", ignoreCase = true) ||
                viewId.contains("address", ignoreCase = true) ||
                viewId.contains("omnibox", ignoreCase = true) ||
                viewId.contains("awesome_bar", ignoreCase = true) ||
                viewId.contains("toolbar", ignoreCase = true)

        if (isUrlField || !isEditable) {
            val text = try { node.text?.toString() ?: "" } catch (_: Throwable) { "" }
            if (text.isNotBlank()) {
                sb.append(" ").append(text)
            }
        }

        val desc = try { node.contentDescription?.toString() ?: "" } catch (_: Throwable) { "" }
        if (desc.isNotBlank()) {
            sb.append(" ").append(desc)
        }

        val childCount = try { node.childCount } catch (_: Throwable) { 0 }
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Throwable) { null } ?: continue
            try {
                val childText = extractTextOrUrl(child, maxDepth - 1, visitedCount)
                if (childText.isNotBlank()) {
                    sb.append(" ").append(childText)
                }
            } finally {
                try { child.recycle() } catch (_: Throwable) {}
            }
        }
        return sb.toString()
    }

    private fun extractAllText(node: AccessibilityNodeInfo, maxDepth: Int, visitedCount: IntArray): String {
        if (maxDepth <= 0 || visitedCount[0] > 30) return ""
        visitedCount[0]++

        val sb = StringBuilder()
        val desc = try { node.contentDescription?.toString() ?: "" } catch (_: Throwable) { "" }
        if (desc.isNotBlank()) {
            sb.append(desc).append(" ")
        }

        val isEditable = try { node.isEditable } catch (_: Throwable) { false }
        if (!isEditable) {
            val text = try { node.text?.toString() ?: "" } catch (_: Throwable) { "" }
            if (text.isNotBlank()) {
                sb.append(text).append(" ")
            }
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Throwable) { null } ?: continue
            try {
                sb.append(extractAllText(child, maxDepth - 1, visitedCount))
            } finally {
                try { child.recycle() } catch (_: Throwable) {}
            }
        }
        return sb.toString()
    }

    private fun triggerBlockShield(targetName: String, reason: String, isWebsite: Boolean) {
        val now = System.currentTimeMillis()
        // Prevent flood within 1000ms for same target
        if (targetName == lastBlockedTarget && (now - lastBlockedTimestamp) < 1000) {
            return
        }
        lastBlockedTimestamp = now
        lastBlockedTarget = targetName

        if (isWebsite) {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (_: Exception) {}
        } else {
            try {
                performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (_: Exception) {}
        }

        // Record stat in database
        val app = application as? FocusGuardApp
        app?.let {
            scope.launch {
                it.repository.recordBlockedAttempt()
            }
        }

        // Launch full-screen block shield
        val intent = Intent(this, BlockedOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockedOverlayActivity.EXTRA_TARGET, targetName)
            putExtra(BlockedOverlayActivity.EXTRA_REASON, reason)
            putExtra(BlockedOverlayActivity.EXTRA_IS_WEBSITE, isWebsite)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
