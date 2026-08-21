# App Overview — FocusGuard

## What is FocusGuard?
FocusGuard is an Android digital-wellbeing / focus app that helps users block distracting apps and websites during intentional focus time. It combines an **accessibility-service enforcement layer**, an **always-on foreground monitor**, a **minimal launcher**, and **gamified focus tracking** (a growing garden) to keep users off distractions.

- **Package:** `com.example`
- **Application class:** `com.example.FocusGuardApp`
- **Launcher activity:** `com.example.MainActivity` (single activity, Jetpack Compose)
- **Platform requirements:** Android (foreground service, accessibility service, exact alarms, system alert window).

## Core Value Proposition
1. **Decide what to block** — build block lists of apps, websites, and keywords.
2. **Start a focus session** — manually, or via recurring schedules.
3. **Stay protected** — the app actively intercepts and blocks distracting apps/websites and bounces the user back.
4. **Grow a garden** — every completed session grows a plant; early exits wither it.
5. **Track progress** — insights show total focus minutes, completed sessions, and blocked attempts.

## Session Modes
| Mode | Behavior |
|------|----------|
| **Normal** | Blocks configured targets; user can end the session early. |
| **Strict** | Locked to a running timer; early exit consumes a daily emergency-exit quota. |
| **Ultra-Strict** | Hard lockdown; no early exit allowed for the entire duration under any circumstance. |
| **Minimalist Strict Lock** | Time-boxed lock that confines the user to the custom Minimal Launcher (Levels 1–3: L1 = flexible 15s-reflection exits, L2 = 1 daily exit with 1-min reflection, L3 = no exits until countdown ends). |

## Primary Surfaces
- **6-tab bottom navigation:** Home (Dashboard), Apps, Lists, Schedules, Insights, Security (Settings).
- **Minimal Launcher:** a stripped-down, customizable home screen (hero clock with selectable typography, focus intention, scratchpad, rotating quotes, app drawer with search and essential-app pinning) shown during sessions / strict locks or standalone.
- **Block Shield overlay:** full-screen screen shown when a blocked app or website is opened.
- **Quick Settings Tile:** `FocusTileService` reflects live session state.
- **Mandatory login gate:** app requires Google sign-in **or** Developer Mode before use.

## Enforcement Stack (high level)
- **`FocusForegroundService`** — persistent 1-second tick loop; keeps blocking cache warm and drives countdown + notification.
- **`FocusAccessibilityService`** — real-time enforcement: intercepts app launches, browser URLs/search queries, and settings tampering.
- **`FocusSessionManager`** — single source of truth for active session/schedule state (in-memory + tamper-evident `SharedPreferences`).
- **`ScheduleAlarmManager` / `ScheduleAlarmReceiver`** — exact alarms for automated schedule windows.
- **`MinimalStrictLockWatchdogReceiver`** — keeps the strict lock alive across process death.
- **`BootCompletedReceiver`** — restarts protection after reboot.

## Key Dependencies
- Jetpack Compose + Material 3, Room, Kotlin Coroutines/Flow.
- Google Identity / Credential Manager + Firebase Auth (with offline fallback).
- Roborazzi / Robolectric for screenshot & unit testing.
