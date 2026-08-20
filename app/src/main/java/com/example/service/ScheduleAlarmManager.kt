package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Schedule
import java.util.Calendar

/**
 * Central helper for scheduling exact AlarmManager alarms for each [Schedule].
 *
 * Request code partitioning:
 *   10_000 + scheduleId.toInt()  → START alarm
 *   20_000 + scheduleId.toInt()  → END   alarm
 *
 * Alarms do not survive device reboot — [BootCompletedReceiver] calls
 * [rescheduleAll] after every boot to re-arm them.
 */
object ScheduleAlarmManager {

    const val ACTION_SCHEDULE_START = "com.example.focusguard.SCHEDULE_START"
    const val ACTION_SCHEDULE_END   = "com.example.focusguard.SCHEDULE_END"
    const val EXTRA_SCHEDULE_ID     = "schedule_id"

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Cancel every existing schedule alarm and re-arm alarms for all [schedules]
     * that are currently enabled.  Call this whenever the schedule list changes
     * (create / update / delete / toggle) and on every boot.
     */
    fun rescheduleAll(context: Context, schedules: List<Schedule>) {
        val am = alarmManager(context)
        // Cancel all known alarms first (enabled or disabled)
        schedules.forEach { cancel(context, it, am) }
        // Arm alarms for enabled schedules only
        schedules.filter { it.isEnabled }.forEach { arm(context, it, am) }
    }

    /** Cancel both alarms for a single [schedule]. */
    fun cancel(context: Context, schedule: Schedule) {
        cancel(context, schedule, alarmManager(context))
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun cancel(context: Context, schedule: Schedule, am: AlarmManager) {
        am.cancel(startPendingIntent(context, schedule.id))
        am.cancel(endPendingIntent(context, schedule.id))
    }

    private fun arm(context: Context, schedule: Schedule, am: AlarmManager) {
        val (startMs, endMs) = computeNextWindow(schedule) ?: return

        setAlarm(am, startMs, startPendingIntent(context, schedule.id))
        setAlarm(am, endMs,   endPendingIntent(context, schedule.id))
    }

    /**
     * Choose the appropriate AlarmManager method based on what the OS allows:
     * - API 31+ requires SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission.
     *   If not granted we fall back to [setAndAllowWhileIdle] (~1 min accuracy).
     * - Below API 31 we always use [setExactAndAllowWhileIdle].
     */
    private fun setAlarm(am: AlarmManager, triggerAtMs: Long, pi: PendingIntent) {
        if (triggerAtMs <= System.currentTimeMillis()) return  // already in the past

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                // Fallback: within ~1 minute; good enough for a schedule trigger
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Next-trigger computation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Given a [schedule], find the next (startMs, endMs) pair in wall-clock time,
     * searching up to 7 days ahead.
     *
     * Handles:
     *  - Same-day windows  (startHour < endHour, e.g. 09:00 → 17:00)
     *  - Overnight windows (startHour > endHour, e.g. 22:00 → 06:00)
     *  - Already-started windows (start in the past, end in the future)
     */
    internal fun computeNextWindow(schedule: Schedule): Pair<Long, Long>? {
        val days = schedule.daysOfWeek
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        if (days.isEmpty()) return null

        val startTotalMins = schedule.startHour * 60 + schedule.startMinute
        val endTotalMins   = schedule.endHour   * 60 + schedule.endMinute
        val overnight      = startTotalMins >= endTotalMins   // e.g. 22:00 → 06:00

        val now = Calendar.getInstance()

        repeat(8) { dayOffset ->
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)  // 1=Sun…7=Sat

            // ── Same-day window ──────────────────────────────────────────────
            if (!overnight && dayOfWeek in days) {
                val startCal = candidate.clone() as Calendar
                startCal.set(Calendar.HOUR_OF_DAY, schedule.startHour)
                startCal.set(Calendar.MINUTE, schedule.startMinute)

                val endCal = candidate.clone() as Calendar
                endCal.set(Calendar.HOUR_OF_DAY, schedule.endHour)
                endCal.set(Calendar.MINUTE, schedule.endMinute)

                // If we're inside the window right now, return (now, endMs)
                // so the session starts immediately but the end alarm is still set.
                if (now.after(startCal) && now.before(endCal)) {
                    return Pair(now.timeInMillis, endCal.timeInMillis)
                }

                // Future window
                if (startCal.after(now)) {
                    return Pair(startCal.timeInMillis, endCal.timeInMillis)
                }
            }

            // ── Overnight window ─────────────────────────────────────────────
            // Start day must be in daysOfWeek; end falls on the *next* calendar day.
            if (overnight && dayOfWeek in days) {
                val startCal = candidate.clone() as Calendar
                startCal.set(Calendar.HOUR_OF_DAY, schedule.startHour)
                startCal.set(Calendar.MINUTE, schedule.startMinute)

                val endCal = candidate.clone() as Calendar
                endCal.add(Calendar.DAY_OF_YEAR, 1)
                endCal.set(Calendar.HOUR_OF_DAY, schedule.endHour)
                endCal.set(Calendar.MINUTE, schedule.endMinute)

                // Inside window (past start, before next-day end)?
                if (now.after(startCal) && now.before(endCal)) {
                    return Pair(now.timeInMillis, endCal.timeInMillis)
                }

                if (startCal.after(now)) {
                    return Pair(startCal.timeInMillis, endCal.timeInMillis)
                }
            }

            // ── Overnight end: we might be in the *end* portion (post-midnight) ──
            // The start day was yesterday; check if the previous day is in daysOfWeek.
            if (overnight) {
                val prevDay = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset - 1)
                }
                val prevDayOfWeek = prevDay.get(Calendar.DAY_OF_WEEK)
                if (prevDayOfWeek in days) {
                    val endCal = candidate.clone() as Calendar
                    endCal.set(Calendar.HOUR_OF_DAY, schedule.endHour)
                    endCal.set(Calendar.MINUTE, schedule.endMinute)

                    if (endCal.after(now)) {
                        // We're already inside this overnight window; start=now, end=endCal
                        return Pair(now.timeInMillis, endCal.timeInMillis)
                    }
                }
            }
        }

        return null  // No window found in the next 7 days
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PendingIntent factories
    // ──────────────────────────────────────────────────────────────────────────

    private fun startPendingIntent(context: Context, scheduleId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (10_000 + scheduleId).toInt(),
            Intent(ACTION_SCHEDULE_START).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun endPendingIntent(context: Context, scheduleId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (20_000 + scheduleId).toInt(),
            Intent(ACTION_SCHEDULE_END).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
