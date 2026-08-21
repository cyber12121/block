# Bug / Improvement Backlog — FocusGuard

Observed issues and suggested improvements, with code references where applicable.

## Bugs / Risks

### B1. Hardcoded offline auth fallback account (Privacy/Security)
`AuthManager.signInWithGoogle` falls back to a hardcoded real-looking account
(`uid = "google_pandagre_vinay_gmail_com"`, `email = "pandagre.vinay@gmail.com"`)
when Credential Manager throws. This silently "signs in" a specific user and
bypasses real authentication. Should fail gracefully and surface the error instead.
- File: `data/auth/AuthManager.kt:322-335`

### B2. Hardcoded Google server client ID
Client ID `682855234582-focusguard.apps.googleusercontent.com` is hardcoded in
`AuthManager.signInWithGoogle` rather than sourced from `local.properties` /
`BuildConfig` / `secrets` plugin (a `secrets` plugin is already declared in the
top-level build file).
- File: `data/auth/AuthManager.kt:239`

### B3. Dead / mismatched constants
- `MAX_MINIMAL_STRICT_EXITS = 1` but `getMinimalStrictExitsRemaining()` returns
  `999` for Level 1, making the constant misleading.
- `KEY_MINIMAL_STRICT_DURATION_HOURS` is declared in the companion object but never used.
- File: `service/FocusSessionManager.kt:966,971`

### B4. Pomodoro total rounds reset to 4
`MainViewModel.transitionPomodoroStage` hardcodes `pomodoroTotalRounds = 4`
instead of preserving the originally configured round count, so sessions started
with a different round count drift.
- File: `ui/MainViewModel.kt:184`

### B5. Collected-but-unused state
`MainViewModel.bloomedGardenPlants` is collected in `MainActivity` but never
passed to any screen — dead state / unnecessary DB observer.
- Files: `ui/MainViewModel.kt:60-61`, `MainActivity.kt:116`

### B6. Over-broad keyword/URL stripping
`isUrlOrKeywordBlocked` strips the last TLD segment for single-token hostnames
to avoid false matches on `.in`/`.me`, but this can weaken legitimate keyword
blocking on bare domains. Edge-case false negatives possible.
- File: `service/FocusSessionManager.kt:601-603`

### B7. Browser URL-bar coverage gaps
`findUrlBarText` relies on a fixed `urlBarIdSuffixes` list; browsers not matching
any known id/suffix fall back to a capped 60-node scan, which may miss the bar or
return wrong text on unusual layouts.
- File: `service/FocusAccessibilityService.kt:361-431`

### B8. Snooze can be defeated by toggling schedule
Toggling a schedule off then on clears the snooze (`MainViewModel.toggleSchedule`
calls `clearSnooze()`), letting a user re-trigger an "ended" window immediately.
- File: `ui/MainViewModel.kt:294`

## Improvements

### I1. Externalize secrets
Move Google client ID and Developer PIN default into `BuildConfig` / `gradle.properties`
and rely on the already-configured `secrets` Gradle plugin. (See B2.)

### I2. Remove silent auth fallback
Replace the hardcoded fallback account (B1) with a proper error path surfaced to
`AuthManager.errorMessage` / UI.

### I3. Add Room migrations
Confirm a migration strategy exists for `AppDatabase`; destructive rebuilds on
schema change would wipe block lists, schedules, garden, and stats.

### I4. Centralize magic numbers
Extract literals (exit quotas, throttle intervals, minimal-lock levels, browser
scan throttle 500ms, redirect grace 2.5s, cache refresh 3s) into a single
`Constants` object for maintainability.

### I5. Strengthen tests
Add unit tests for `FocusSessionManager` (clock-tamper detection, exit-quota edge
cases, schedule matching) and Compose tests for the 6 tabs. Robolectric/Roborazzi
are already wired.

### I6. Improve accessibility-tree efficiency
Cache resolved URL-bar node paths per browser package to avoid repeated tree walks.

### I7. UX: clarify "Security" tab
The 6th tab is labeled "Security" with a shield icon but renders `SettingsScreen`.
Rename/relabel for consistency.

### I8. Persist user-selected essential apps count semantics
`getCustomEssentialApps` caps at 5 and migrates legacy packages; document this
limit in the UI so users understand why only 5 essentials are allowed.

### I9. Make Pomodoro rounds configurable end-to-end
Propagate the chosen `pomodoroTotalRounds` through `transitionPomodoroStage` (B4).

### I10. Battery / privacy audit
Accessibility service inspects window content broadly; add a visible disclosure and
consider narrowing event types to reduce overhead on low-end devices.
