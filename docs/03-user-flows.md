# User Flows — FocusGuard

Key user journeys through the app, mapped to code components.

## Flow 1: First Launch & Authorization
```
App install → FocusGuardApp.onCreate()
  → MainActivity renders
    → AuthManager.currentUser / isDeveloperMode checked
    → NOT authorized → MandatoryLoginGateScreen
        → Google Sign-In (AuthManager.signInWithGoogle)
        → OR Developer Mode PIN (2026)
    → authorized → main app UI
```
Guard: `MainActivity` line `if (!isAuthorized) { MandatoryLoginGateScreen(...) ; return }`.

## Flow 2: Create Block List + Add Targets
```
Lists tab → BlockListsScreen
  → CreateListDialog → MainViewModel.createBlockList → repository.insertBlockList
  → select list → AddTargetDialog → addBulkTargets(listId, type, items, category)
      → repository.insertTargets → sessionManager.refreshBlockedTargetsCache
```

## Flow 3: Start a Manual Focus Session (Normal)
```
Dashboard → onStartSessionClick → StartSessionDialog
  → choose duration / lists / strictness
  → MainViewModel.startFocusSession
      → sessionManager.startSession (persists to Prefs + DB FocusSession + plantSeed)
      → FocusForegroundService tick begins; Dashboard shows live countdown
  → tap session → SessionScreen (isLiveSessionFullscreen)
  → end normally → onEndNormalSession → viewModel.endCurrentSession → plant BLOOMED, stats recorded
```

## Flow 4: Start a Strict / Ultra-Strict Session (No Easy Exit)
```
StartSessionDialog → isStrict / isUltraStrict = true
  → startSession → isStrictMode/isUltraStrict persisted
  → Accessibility service enforces:
      - blocked apps → Block Shield
      - Settings/App-Info about FocusGuard → Anti-Uninstall Defense
  → Early exit attempts:
      - Strict: consumeDailyExit (quota 10/day) → forceUnlockSession → plant WITHERED
      - Ultra-Strict with time remaining: exit FORBIDDEN (forceUnlockSession returns false)
```

## Flow 5: Open a Blocked App During a Session
```
User taps Instagram
  → FocusAccessibilityService.onAccessibilityEvent(targetPkg)
      → isAnyBlockingActive && !isEssential && isAppBlocked(targetPkg)
      → triggerBlockShield → launches BlockedOverlayActivity
      → repository.recordBlockedAttempt()
```

## Flow 6: Open a Blocked Website in Browser
```
User searches "reddit" in Chrome
  → onAccessibilityEvent → isBrowserApp(targetPkg)
      → findUrlBarText → isUrlOrKeywordBlocked(urlText)
      → match → triggerBlockShield(isWebsite=true)
          → browser auto-navigates to about:blank (redirect-loop guard)
          → Block Shield overlay shown
```

## Flow 7: Automated Schedule Window
```
ScheduleAlarmReceiver (SCHEDULE_START) → ScheduleAlarmManager
  → sessionManager.checkAutomaticSchedules
      → ActiveSchedulesState.isActive + strictness set
      → refreshBlockedTargetsCache → blocking enforced by accessibility service
  → window ends → SCHEDULE_END / tick expiry → session ended, schedule snoozed for remainder of window
```

## Flow 8: Minimalist Strict Lock
```
Dashboard "Open Minimal Launcher" / StartSession autoLaunchMinimal
  → MinimalLauncherScreen → MinimalStrictLockSetupDialog
      → choose duration (1m–12h) + level (1/2/3)
      → sessionManager.startMinimalStrictLock(duration, level)
  → MinimalStrictLockWatchdogReceiver scheduled; launcher locked
  → user tries non-essential app
      → accessibility service bounces to launcher or shows Block Shield
  → Exit attempt (handleExitRequest):
      - Not locked: stopMinimalStrictLock + setMinimalLauncherActive(false)
      - Locked + Dev Mode: DevExitConfirmDialog → disarm
      - Locked + Level 1: MinimalStrictUseExitDialog (15s reflection) → exit
      - Locked + Level 2: MinimalStrictUseExitDialog (60s reflection) → useMinimalStrictExit consumes 1 daily exit
      - Locked + Level 3 / exits exhausted: MinimalStrictLockedDialog (no exit)
```

## Flow 8b: Minimal Launcher Daily Use (no session)
```
Dashboard → "Open Minimal Launcher" (no active session)
  → MinimalLauncherScreen home: hero clock, date, intention, quote
  → Open App Drawer: search all installed apps, A–Z rail, pin/unpin essentials (max 6)
  → Preferences (Tune): switch clock style, edit intention, open scratchpad
  → Exit: handleExitRequest → stopMinimalStrictLock + setMinimalLauncherActive(false) + onExitLauncher
```
Preferences persisted in `minimal_launcher_prefs` SharedPreferences (user_intention, scratchpad_notes). Clock style is in-memory per session.

## Flow 9: Emergency Exit (Daily Quota)
```
Strict session active → EmergencyUnlockDialog / Settings
  → viewModel.forceEmergencyUnlock → sessionManager.forceUnlockSession
      → AuthManager.consumeDailyExit (10/day, unlimited in Dev Mode)
      → if allowed: session ends early, plant WITHERED, all schedules disabled
```

## Flow 10: App Restart / Reboot Survival
```
Process killed or device rebooted
  → FocusGuardApp.onCreate → FocusForegroundService.startService
  → BootCompletedReceiver → restarts protection
  → FocusSessionManager.restoreSessionFromPrefs → rebuilds ActiveSessionState + cache
```
