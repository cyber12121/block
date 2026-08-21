package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.MainActivity

/**
 * BlockIT-style watchdog for the Minimalist Strict Lock ONLY.
 *
 * While a Minimalist Strict Lock is active this receiver re-arms itself every
 * [INTERVAL_MS] via AlarmManager. On each fire it bounces the user back into the
 * Minimal Launcher (and, by re-launching MainActivity, re-pins the screen) so the
 * lock can't be escaped by killing the app or leaving via Home. If the lock has
 * expired it cancels itself. It is intentionally NOT used for normal sessions or
 * the plain Minimal Launcher configuration mode.
 */
class MinimalStrictLockWatchdogReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION = "com.example.MINIMAL_STRICT_LOCK_WATCHDOG"
        private const val REQUEST_CODE = 0x4D53 // "MS"
        private const val INTERVAL_MS = 5_000L

        // Require 2 consecutive "escaped" ticks before locking the screen.
        // A single tick where the OEM launcher is briefly foregrounded (e.g.
        // due to accessibility-event delay after pressing Home) must not trigger
        // a spurious lock — this counter prevents that false-positive.
        @Volatile private var consecutiveEscapeCount = 0
        private const val ESCAPE_THRESHOLD = 2

        fun schedule(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, MinimalStrictLockWatchdogReceiver::class.java).apply {
                action = ACTION
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS
            runCatching {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pi
                )
            }.onFailure {
                // Fallback: inexact but still wakes the device if exact alarm is denied.
                runCatching {
                    am.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pi
                    )
                }
            }
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, MinimalStrictLockWatchdogReceiver::class.java).apply {
                action = ACTION
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val sessionManager = FocusSessionManager.getInstance(context)

        // Lock expired (or was released) -> stop the watchdog entirely.
        if (!sessionManager.isMinimalStrictLockActive()) {
            consecutiveEscapeCount = 0
            cancel(context)
            return
        }

        // Only drag the user back if they escaped FocusGuard AND are not using an essential app.
        // isLockEscaped() already excludes OEM launchers and stale foreground data.
        if (sessionManager.isLockEscaped()) {
            consecutiveEscapeCount++

            // Bounce back to Minimalist Launcher immediately on first confirmed escape.
            runCatching {
                val relaunch = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                    putExtra(MainActivity.EXTRA_OPEN_MINIMAL_LAUNCHER, true)
                }
                context.startActivity(relaunch)
            }

            // Only lock the screen after ESCAPE_THRESHOLD consecutive ticks of confirmed
            // escape — this prevents a transient launcher flash from triggering a lock.
            // Also guard with device-admin check to avoid SecurityException on Android 13+.
            if (consecutiveEscapeCount >= ESCAPE_THRESHOLD) {
                com.example.util.PermissionUtils.lockScreen(context)
            }
        } else {
            // User is in FocusGuard or an essential app — reset the counter.
            consecutiveEscapeCount = 0
        }

        // Re-arm the next check while the lock is still live.
        schedule(context)
    }
}
