# Feature Inventory — FocusGuard

Inventory of user-facing and system features found in the codebase.

## 1. Block Lists (`BlockList`)
- User-defined categories grouping blocked targets (e.g., Social, Adult & NSFW, Work Distractions).
- CRUD via `BlockListsScreen` / `CreateListDialog` / `CreateScheduleDialog` components.
- Toggle enable/disable; cannot disable an enabled list while blocking is active (`MainViewModel.toggleBlockList`).
- Seeded with defaults by `DefaultData.seedInto` (`AppRepository.ensureDefaultData`).

## 2. Blocked Targets (`BlockedTarget`)
- Three target types (`TargetType`): `APP` (package name), `WEBSITE` (domain), `KEYWORD` (text).
- Bulk add via `AddTargetDialog` / `AddTargetDialog` (`MainViewModel.addBulkTargets`).
- Edit / delete / toggle via `EditTargetDialog`, `DeleteTarget`.
- Mutations refresh the cached block sets in `FocusSessionManager.refreshBlockedTargetsCache`.

## 3. Focus Sessions (Manual)
- Started from Dashboard (`StartSessionDialog`) or quick-start.
- Configurable duration, strictness, active lists, Pomodoro mode.
- Plant selection auto-derived from duration (`SPROUT` → `ANCIENT_REDWOOD`).
- State persisted in `SharedPreferences` so sessions survive process death (`restoreSessionFromPrefs`).

## 4. Pomodoro Sessions
- Focus/break cycles with configurable rounds (`pomodoroRound`, `pomodoroTotalRounds`).
- Transitions handled in `SessionScreen.onTransitionPomodoro` → `MainViewModel.transitionPomodoroStage`.

## 5. Schedules (Automated Blocking)
- Recurring time windows (`Schedule`: start/end, days-of-week, strictness, active lists).
- Driven by exact alarms (`ScheduleAlarmManager` + `ScheduleAlarmReceiver`).
- Enforced through `FocusSessionManager.checkAutomaticSchedules`.
- Snooze support: finishing a schedule-triggered window early snoozes that schedule for the rest of its window.

## 6. Minimal Launcher
- Custom home screen (`MinimalLauncherScreen`, `ui/screens`) shown during sessions / strict locks or standalone via Dashboard "Open Minimal Launcher".
- Replaces the device launcher while active (`MainActivity.EXTRA_OPEN_MINIMAL_LAUNCHER`); bounce-back enforced by the accessibility service.
- **App Drawer & Search** — full installed-app list with live search, A–Z quick-jump rail, and per-app BLOCKED badges. During an active session / strict lock, the drawer shows only allowed essential apps.
- **Essential App Pinning** — up to 6 user-selected essential apps (`saveCustomEssentialApps`, `getCustomEssentialApps`) pinned from the drawer or via `EditEssentialAppsDialog`; allowed even during strict locks.
- **Launcher Customization** (persisted in `minimal_launcher_prefs` SharedPreferences):
  - **Clock styles** (`MinimalClockStyle`): `MODERN_CLEAN` (Modern Sans), `MINIMAL_MONO` (Digital Mono), `EDITORIAL_SERIF` (Editorial Serif).
  - **User Intention** — editable focus intention line shown on the home screen.
  - **Scratchpad** — quick notes stored locally.
  - **Focus Quotes** — rotating zen quotes (`FOCUS_QUOTES`).
- **Live hero clock + date + active-session countdown strip** with progress bar.

## 7. Minimalist Strict Lock
- Time-boxed lock (`startMinimalStrictLock`, `getMinimalStrictLevel`) with 3 levels (`MinimalStrictLockSetupDialog`):
  - **Level 1 (Soft Strict):** flexible exits with a 15-second reflection timer.
  - **Level 2 (Standard, default):** 1 emergency exit per day with a mandatory 1-minute reflection timer (`MinimalStrictUseExitDialog`, `MAX_MINIMAL_STRICT_EXITS = 1`).
  - **Level 3 (Ultra Strict):** zero early exits permitted until countdown ends.
- Duration options from 1 min up to 12 hours (`StrictDurationOption`).
- Reflection countdown enforced in `MinimalStrictUseExitDialog` (15s/60s) before exit is allowed.
- Status / locked / dev-disarm dialogs: `MinimalStrictLockStatusDialog`, `MinimalStrictLockedDialog`, `DevExitConfirmDialog`.
- Watchdog (`MinimalStrictLockWatchdogReceiver`) keeps it alive after process death; level 3 enforced in `FocusSessionManager.useMinimalStrictExit` (always returns false).

## 8. Block Shield Overlay (`BlockedOverlayActivity`)
- Full-screen blocking screen shown when a blocked app/website is opened.
- Shows target name + reason; in browsers auto-navigates to `about:blank` to break redirect loops.

## 9. Anti-Tamper / Anti-Uninstall
- Accessibility service detects and blocks access to Settings / App Info / uninstall / force-stop / disable actions targeting FocusGuard during Strict or Ultra-Strict modes (`triggerBlockShield` "Anti-Uninstall Defense").
- Ultra-Strict intercepts System UI quick-settings/airplane toggles.

## 10. Focus Garden (Gamification)
- Each session plants a seed (`GardenPlant`); completes bloom it, early exits wither it.
- `GardenScreen` displays bloomed/withered plants; counts in Dashboard.

## 11. Insights & Stats (`DailyStat`, `FocusSession`)
- Total focus minutes, completed sessions, total blocked attempts.
- Daily history via `recentDailyStats`; visualized in `InsightsScreen`.

## 12. Authentication & Access Control
- Google Sign-In (`AuthManager.signInWithGoogle`) via Credential Manager + Firebase Auth (offline fallback).
- **Developer Mode** (PIN `2026`) bypasses login and grants unlimited exits.
- Mandatory login gate (`MandatoryLoginGateScreen`) before app use.
- Daily emergency-exit quota (`STANDARD_DAILY_EXIT_LIMIT = 10`).

## 13. Quick Settings Tile (`FocusTileService`)
- Live status indicator; requests updates on session start/end/change.

## 14. Device Admin (`FocusDeviceAdminReceiver`)
- Registered device-admin receiver for privileged protections.

## 15. Background Resilience
- Foreground service (`START_STICKY`, `onTaskRemoved` restart).
- `BootCompletedReceiver` re-initializes protection after reboot.

## 16. Sound (`FocusSoundEngine`)
- Focus sound engine for session ambiance/feedback.

## 17. Essential Apps
- Hardcoded system essentials (dialer, SMS, camera, clock, calculator) + up to 5 user-selected custom essentials allowed during strict locks (`isEssentialApp`, `saveCustomEssentialApps`).
