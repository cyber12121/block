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
            cancel(context)
            return
        }

        // Only drag the user back if they escaped FocusGuard AND are not using an essential app
        if (sessionManager.isLockEscaped()) {
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
            com.example.util.PermissionUtils.lockScreen(context)
        }

        // Re-arm the next check while the lock is still live.
        schedule(context)
    }
}
