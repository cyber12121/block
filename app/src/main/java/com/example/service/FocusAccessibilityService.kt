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

    // Throttle database reads: refresh the block cache on app switches,
    // otherwise at most once every few seconds.
    private var lastCacheRefreshTime: Long = 0L

    // Throttle the accessibility-tree URL scan inside browsers.
    // TYPE_WINDOW_CONTENT_CHANGED fires on every DOM mutation — running a full
    // tree walk on every event would hammer CPU. We allow an immediate scan on
    // real page navigations (STATE_CHANGED) and typing (TEXT_CHANGED), and
    // throttle CONTENT_CHANGED to at most once every 500 ms.
    private var lastBrowserUrlScanTime: Long = 0L
    private val BROWSER_SCAN_THROTTLE_MS = 500L

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

        // Keep the block cache warm WITHOUT hammering the database on every event:
        // refresh immediately when the foreground window changes (app switch),
        // otherwise at most once every 3 seconds.
        val nowForCache = System.currentTimeMillis()
        val isWindowSwitch = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (isWindowSwitch || (nowForCache - lastCacheRefreshTime) > CACHE_REFRESH_INTERVAL_MS) {
            lastCacheRefreshTime = nowForCache
            val app = application as? FocusGuardApp
            if (app != null) {
                scope.launch {
                    sessionManager.refreshBlockedTargetsCache(app.repository)
                }
            }
        }

        // Only enforce the "pull the user back into FocusGuard" behaviour during a real
        // focus session. Minimal Launcher can be switched on from the dashboard with no
        // session running so the user can lay it out and pick essential apps; enforcing
        // here as well meant every tap on home or another app bounced them back into the
        // app mid-configuration, with no way to escape short of disabling the service.
        if (sessionState.isActive) {
            val isFgApp = targetPkg == applicationContext.packageName
            val essentialApps = sessionManager.getCustomEssentialApps().map { it.lowercase() }

            // Exact package match only. The previous `targetPkg.contains(essential)`
            // fallback let anything sharing a prefix through the shield — with
            // "com.android.dialer" marked essential, "com.android.dialer.spam" (or any
            // package embedding that string) was silently treated as essential too.
            val isEssential = essentialApps.contains(targetPkg.lowercase())
            val isMinimalLauncherHome = sessionManager.isMinimalLauncherActive()

            val isLauncherOrHome = targetPkg.contains("launcher") || targetPkg.contains("home")

            if (!isFgApp && !isEssential) {
                if (isLauncherOrHome) {
                    // Home/launcher press during a session:
                    // - Strict Mode AND Minimal Launcher is the active screen:
                    //   bounce back — the user must stay inside the Minimal Launcher.
                    // - Every other case (Normal mode, or Strict mode but NOT in Minimal
                    //   Launcher): allow going home freely. App-blocking still fires
                    //   when the user opens a blocked app from their system launcher.
                    if (sessionState.isStrictMode && isMinimalLauncherHome) {
                        val relaunch = Intent(this, com.example.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(relaunch)
                        return
                    }
                    // All other cases — let them go home freely.
                } else if (!isLauncherOrHome && sessionManager.isAppBlocked(targetPkg)) {
                    // Blocked app opened — show the dedicated block shield overlay
                    triggerBlockShield(
                        targetName = getReadableAppName(targetPkg),
                        reason = "App '$targetPkg' is restricted in your active focus shield.",
                        isWebsite = false
                    )
                    return
                }
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

        // 2. Check if the app is in a blocked list (browsers included —
        //    a blocked browser must be blocked like any other app)
        if (sessionManager.isAppBlocked(targetPkg)) {
            triggerBlockShield(
                targetName = getReadableAppName(targetPkg),
                reason = "App '$targetPkg' is restricted in your active focus shield.",
                isWebsite = false
            )
            return
        }

        // 3. Inspect Browsers (Chrome, Brave, Firefox, Edge, Opera, Samsung, DuckDuckGo, etc.)
        //    Blocking triggers on the actual URL and on search queries typed in the
        //    address bar — NOT on arbitrary page text (avoids false positives).
        if (isBrowserApp(targetPkg)) {
            // Fire URL scan on any window or content change — Chrome often fires only
            // TYPE_WINDOW_CONTENT_CHANGED when navigating to a new URL without a new
            // window, so limiting to TYPE_WINDOW_STATE_CHANGED missed most navigations.
            val isRelevantBrowserEvent = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED

            if (isRelevantBrowserEvent) {
                // For CONTENT_CHANGED, throttle the tree walk to avoid CPU hammering.
                // For STATE_CHANGED / TEXT_CHANGED fire immediately (real navigation or typing).
                val isContentChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                val shouldScan = !isContentChanged ||
                        (now - lastBrowserUrlScanTime) > BROWSER_SCAN_THROTTLE_MS

                if (shouldScan) {
                    lastBrowserUrlScanTime = now
                    val urlText = findUrlBarText(targetPkg)
                    if (urlText.isNotBlank()) {
                        val (isBlocked, matchedRule) = sessionManager.isUrlOrKeywordBlocked(urlText)
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
            }

            // Also catch text being typed into the address/search bar in real time.
            // Check event text directly (without requiring the view ID to look like a
            // URL field — that ID-based gate was too strict and missed many browsers).
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                val eventTexts = event.text.joinToString(" ").trim()
                if (eventTexts.isNotBlank()) {
                    val (isBlocked, matchedRule) = sessionManager.isUrlOrKeywordBlocked(eventTexts)
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

    // Well-known address-bar view id suffixes across popular browsers
    private val urlBarIdSuffixes = listOf(
        "url_bar",                          // Chrome, Brave, Edge, Kiwi, Vivaldi (Chromium)
        "mozac_browser_toolbar_url_view",   // Firefox (Fenix)
        "url_bar_title",                    // Firefox (legacy)
        "location_bar_edit_text",           // Samsung Internet
        "url_field",                        // Opera
        "url_edit",                         // Opera variants
        "omnibarTextInput",                 // DuckDuckGo
        "search_box_text",                  // misc
        "addressbarEdit",                   // UC / misc
        "url"                               // generic fallback id
    )

    private fun looksLikeUrlField(viewId: String): Boolean {
        if (viewId.isBlank()) return false
        val lower = viewId.lowercase()
        return lower.contains("url") ||
               lower.contains("omnibox") ||
               lower.contains("omnibar") ||
               lower.contains("address") ||
               lower.contains("location") ||
               lower.contains("search")
    }

    /**
     * Reliably grabs the address-bar text of the active browser window.
     * 1. Fast path: query well-known URL-bar view ids directly (O(1), no tree walk).
     * 2. Fallback: capped breadth-first scan for any view whose id looks like a URL/search field.
     */
    private fun findUrlBarText(browserPkg: String): String {
        val root = rootInActiveWindow ?: windows.firstOrNull { it.isActive }?.root ?: return ""

        for (suffix in urlBarIdSuffixes) {
            val nodes = try {
                root.findAccessibilityNodeInfosByViewId("$browserPkg:id/$suffix")
            } catch (_: Throwable) { null }
            if (nodes.isNullOrEmpty()) continue
            var found = ""
            for (n in nodes) {
                val text = try { n.text?.toString() ?: "" } catch (_: Throwable) { "" }
                if (found.isBlank() && text.isNotBlank()) found = text
            }
            if (found.isNotBlank()) return found
        }

        return scanForUrlField(root, maxNodes = 250)
    }

    private fun scanForUrlField(root: AccessibilityNodeInfo, maxNodes: Int): String {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            val viewId = try { node.viewIdResourceName ?: "" } catch (_: Throwable) { "" }
            if (looksLikeUrlField(viewId)) {
                val text = try { node.text?.toString() ?: "" } catch (_: Throwable) { "" }
                if (text.isNotBlank()) return text
            }
            val childCount = try { node.childCount } catch (_: Throwable) { 0 }
            for (i in 0 until childCount) {
                val child = try { node.getChild(i) } catch (_: Throwable) { null } ?: continue
                queue.add(child)
            }
        }
        return ""
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

        val childCount = try { node.childCount } catch (_: Throwable) { 0 }
        for (i in 0 until childCount) {
            val child = try { node.getChild(i) } catch (_: Throwable) { null } ?: continue
            sb.append(extractAllText(child, maxDepth - 1, visitedCount))
        }
        return sb.toString()
    }

    private fun triggerBlockShield(targetName: String, reason: String, isWebsite: Boolean) {
        val now = System.currentTimeMillis()
        val isRepeatTrigger = targetName == lastBlockedTarget && (now - lastBlockedTimestamp) < 1500
        lastBlockedTimestamp = now
        lastBlockedTarget = targetName

        // ALWAYS push the user away from the blocked content — even on rapid
        // repeat triggers. (Previously the flood guard returned early, letting a
        // re-opened blocked app stay on screen for up to a second.)
        if (isWebsite) {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (_: Exception) {}
        } else {
            try {
                performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (_: Exception) {}
        }

        // Only skip the overlay + stat recording on rapid repeats, to avoid spam
        if (isRepeatTrigger) return

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

    companion object {
        private const val CACHE_REFRESH_INTERVAL_MS = 3000L
    }
}
