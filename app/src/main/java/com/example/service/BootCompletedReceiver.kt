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

            CoroutineScope(Dispatchers.IO).launch {
                // Restore live blocking enforcement immediately after boot
                sessionManager.refreshBlockedTargetsCache(app.repository)

                // AlarmManager alarms don't survive reboot — re-arm all schedule
                // alarms so they fire at the correct wall-clock times.
                val schedules = app.repository.getEnabledSchedules()
                ScheduleAlarmManager.rescheduleAll(context, schedules)
            }
        }
    }
}
