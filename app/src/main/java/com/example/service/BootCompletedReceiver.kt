package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.FocusGuardApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? FocusGuardApp ?: return
            val sessionManager = FocusSessionManager.getInstance(context)

            try {
                FocusForegroundService.startService(context)
            } catch (_: Exception) {}

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Restore live blocking enforcement immediately after boot
                    sessionManager.refreshBlockedTargetsCache(app.repository)

                    // BlockIT-style: if a Minimalist Strict Lock survived the reboot,
                    // re-arm its watchdog so the lock can't be escaped post-boot.
                    if (sessionManager.isMinimalStrictLockActive()) {
                        MinimalStrictLockWatchdogReceiver.schedule(context)
                    }

                    // AlarmManager alarms don't survive reboot — re-arm all schedule
                    // alarms so they fire at the correct wall-clock times.
                    val schedules = app.repository.getEnabledSchedules()
                    ScheduleAlarmManager.rescheduleAll(context, schedules)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
