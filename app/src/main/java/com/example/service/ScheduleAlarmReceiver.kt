package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.FocusGuardApp
import com.example.data.model.PlantType
import com.example.service.ScheduleAlarmManager.ACTION_SCHEDULE_END
import com.example.service.ScheduleAlarmManager.ACTION_SCHEDULE_START
import com.example.service.ScheduleAlarmManager.EXTRA_SCHEDULE_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives exact AlarmManager broadcasts for schedule start and end events.
 *
 * Uses [goAsync] so we can safely do DB I/O inside the broadcast budget without
 * blocking the main thread.  After acting on the alarm we re-arm the *next*
 * occurrence of the same schedule so it fires again next week.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId < 0) return

        val action = intent.action ?: return
        if (action != ACTION_SCHEDULE_START && action != ACTION_SCHEDULE_END) return

        val app = context.applicationContext as? FocusGuardApp ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = app.repository
                val sessionManager = FocusSessionManager.getInstance(context)

                // Load the schedule that triggered this alarm
                val schedules = repository.getEnabledSchedules()
                val schedule = schedules.firstOrNull { it.id == scheduleId }

                when (action) {
                    ACTION_SCHEDULE_START, ACTION_SCHEDULE_END -> {
                        sessionManager.checkAutomaticSchedules(repository)
                    }
                }

                // Re-arm this schedule's alarm for its next occurrence (next week).
                // We call rescheduleAll so all schedules stay in sync.
                val allEnabledSchedules = repository.getEnabledSchedules()
                ScheduleAlarmManager.rescheduleAll(context, allEnabledSchedules)

                // Ensure the foreground service is alive for blocking enforcement
                FocusForegroundService.startService(context)

            } finally {
                pendingResult.finish()
            }
        }
    }
}
