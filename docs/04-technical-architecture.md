# Technical Architecture — FocusGuard

## Layered Architecture
```
┌─────────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose)                                   │
│  MainActivity / MainAppContent                                │
│  Screens: Dashboard, Apps, BlockLists, Schedules, Insights,  │
│           Settings, MinimalLauncher, Session, Garden         │
│  Components: Dialogs, BlockedOverlayActivity, StrictMode...  │
│  Minimal Launcher: MinimalLauncherScreen + MinimalLauncher   │
│   Dialogs (setup/status/locked/use-exit/dev-exit, essential  │
│   apps, emergency exit)                                      │
└───────────────────────────┬─────────────────────────────────┘
                            │ StateFlow (collectAsStateWithLifecycle)
┌───────────────────────────┴─────────────────────────────────┐
│ Presentation / State (ViewModel)                              │
│  MainViewModel + MainViewModelFactory                         │
│  Exposes sessionState, blockLists, schedules, stats, garden  │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────┐
│ Domain / Control Layer                                        │
│  FocusSessionManager (singleton, in-memory + Prefs state)     │
│  AuthManager (singleton)                                      │
│  ScheduleAlarmManager, PermissionUtils, ScheduleUtils         │
│  InstalledAppsCache, FocusSoundEngine                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────┐
│ Data Layer (Room + SharedPreferences)                         │
│  AppDatabase + DAOs + Entities                                │
│  AppRepository (single facade)                                │
│  FocusGuardApp (owns DB + Repository + SessionManager)        │
└─────────────────────────────────────────────────────────────┘

System Services (always-on):
  FocusForegroundService  → 1s monitor loop (tick + schedule check + notification)
  FocusAccessibilityService → real-time enforcement + app/URL interception
  FocusTileService → QS tile
  BootCompletedReceiver, ScheduleAlarmReceiver, MinimalStrictLockWatchdogReceiver, FocusDeviceAdminReceiver
```

## Application Entry Point
- `FocusGuardApp` (extends `Application`): lazily builds `AppDatabase`, `AppRepository`, and `FocusSessionManager` (`getInstance`). On `onCreate`, starts `FocusForegroundService`, seeds defaults, refreshes block cache, and loads installed apps.

## State Management
- **UI state** is driven by Kotlin `StateFlow`s collected with `collectAsStateWithLifecycle`.
- **Session/schedule truth** lives in `FocusSessionManager`:
  - `_sessionState: MutableStateFlow<ActiveSessionState>` (active, strict, ultra-strict, remaining seconds, plant type, pomodoro info).
  - `_activeSchedulesState: MutableStateFlow<ActiveSchedulesState>`.
  - Persisted to `SharedPreferences` (`focus_guard_secure_state`) with a tamper-evident end-time baseline for clock-tamper detection (`KEY_ELAPSED_BASELINE`).
- **Cached block sets** (`cachedBlockedPackages / Domains / Keywords`) are `@Volatile` fast-lookup sets refreshed on app switches / changes.

## Data Model (Room Entities)
| Entity | Table | Notes |
|--------|-------|-------|
| `BlockList` | `block_lists` | category of targets; `isDefault`, `isEnabled` |
| `BlockedTarget` | `blocked_targets` | FK→BlockList; `targetType` APP/WEBSITE/KEYWORD |
| `FocusSession` | `focus_sessions` | records session lifecycle, completion, early-unlock |
| `DailyStat` | `daily_stats` | per-day blocked attempts + completed minutes |
| `Schedule` | `schedules` | recurring windows; strictness; active lists |
| `GardenPlant` | `garden_plants` | GROWING/BLOOMED/WITHERED; type by duration |
| `AuthUser` | (prefs) | signed-in user / developer flag |

All DB access goes through `AppRepository` (single facade over 6 DAOs). Flows exposed: `allBlockLists`, `allTargetsFlow`, `allSchedules`, `totalMinutes`, `completedSessionsCount`, `totalBlockedAttempts`, `recentDailyStats`, garden flows.

## Enforcement Path
1. `FocusForegroundService` ticks every second → `updateTick()` advances `remainingSeconds`, detects clock tampering, fires completion once (`isCompletingSession` guard).
2. `FocusAccessibilityService.onAccessibilityEvent`:
   - Reports foreground package to watchdog (`reportForeground`).
   - Bounce-back to Minimal Launcher when Minimalist Strict Lock active.
   - App-block check → `isAppBlocked`.
   - Browser URL/keyword scan (`findUrlBarText` → `isUrlOrKeywordBlocked`).
   - Anti-uninstall / settings tamper via accessibility-tree text extraction.
   - On match → `triggerBlockShield` → `BlockedOverlayActivity` + `recordBlockedAttempt()`.
3. Throttling: cache refresh ≤ every 3s, browser URL scan ≤ every 500ms, redirect-loop grace 2.5s, repeat-trigger de-dup 1.5s.

## Scheduling
- `ScheduleAlarmManager.rescheduleAll` uses exact alarms (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`); falls back to `setAndAllowWhileIdle`.
- `ScheduleAlarmReceiver` fires `SCHEDULE_START`/`SCHEDULE_END` → `checkAutomaticSchedules`.
- Foreground loop also polls schedules every 2s for real-time sync.

## Concurrency
- `CoroutineScope(Dispatchers.IO)` in `FocusGuardApp`, `FocusSessionManager`, `FocusForegroundService`, `AuthManager`.
- UI collects on lifecycle-aware scopes via `viewModelScope` + `SharingStarted.WhileSubscribed`.

## Permissions & Components (Manifest)
- Permissions: `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` (+ `SPECIAL_USE`), `WAKE_LOCK`, `SYSTEM_ALERT_WINDOW`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`.
- Components: `MainActivity` (LAUNCHER, singleTop), `BlockedOverlayActivity` (excludeFromRecents), `FocusForegroundService` (specialUse), `FocusAccessibilityService` (BIND_ACCESSIBILITY_SERVICE), `FocusTileService` (QS tile), `BootCompletedReceiver`, `FocusDeviceAdminReceiver`, `ScheduleAlarmReceiver`, `MinimalStrictLockWatchdogReceiver`.

## Tech Stack
- Kotlin, Jetpack Compose (Material 3), AndroidX Lifecycle/ViewModel.
- Room (SQLite), Kotlin Coroutines & Flow.
- Google Identity Credential Manager + Firebase Auth (`com.google.firebase:firebase-auth`).
- Manual dependency injection via `MainViewModelFactory` (no Hilt/Dagger).
- Custom bottom-nav (no Navigation Compose `NavHost`).
- Testing: Robolectric + Roborazzi (screenshot tests) in `androidTest`/`test`.

## Notable Design Decisions / Comments in Code
- Screen pinning removed; protection via Default Home Launcher + Accessibility + Overlay.
- Duplicate tick loops removed (foreground service owns the loop) to avoid double DB reads.
- Session-preemption guard (`isCompletingSession`) prevents repeated completion writes.
- `clearSessionPrefs` intentionally does NOT `clear()` all prefs (preserves essential-apps + exit quota).
- Strict-Blocker escape detection via `isLockEscaped` + stale foreground timestamp (10s).
- Minimal Launcher preferences (`user_intention`, `scratchpad_notes`) persisted in a separate `minimal_launcher_prefs` SharedPreferences store; clock style (`MinimalClockStyle`) is in-memory per launcher session.
- Launcher ships its own dialog set in `ui/components/MinimalLauncherDialogs.kt` (setup, status, locked, use-exit reflection timer, dev-exit, essential-apps editor, emergency exit) plus UI theme tokens `CrimsonStrict`, `CyanAccent`, `EmeraldSuccess`.
