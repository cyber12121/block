package com.example.util

import com.example.data.model.BlockList
import com.example.data.model.Schedule
import java.util.Calendar

object ScheduleUtils {

    /**
     * Parses comma-separated day numbers (1=Sun, 2=Mon ... 7=Sat) safely.
     */
    fun parseDaysOfWeek(daysString: String): Set<Int> {
        if (daysString.isBlank()) return emptySet()
        return daysString.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    /**
     * Checks if a schedule's time window is currently active at the provided [calendar] instant.
     * Supports:
     * - Same-day windows (e.g. 09:00 to 17:00)
     * - Overnight windows (e.g. 22:00 to 06:00)
     * - 24-hour windows (e.g. 00:00 to 00:00 / start == end)
     */
    fun isScheduleActiveAt(schedule: Schedule, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!schedule.isEnabled) return false
        val days = parseDaysOfWeek(schedule.daysOfWeek)
        if (days.isEmpty()) return false

        val currentDay = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun ... 7=Sat
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        val startMinutes = schedule.startHour * 60 + schedule.startMinute
        val endMinutes = schedule.endHour * 60 + schedule.endMinute

        return if (startMinutes < endMinutes) {
            days.contains(currentDay) && (currentTotalMinutes in startMinutes until endMinutes)
        } else if (startMinutes > endMinutes) {
            // Overnight window: spans across midnight
            if (currentTotalMinutes >= startMinutes) {
                // Today evening before midnight
                days.contains(currentDay)
            } else if (currentTotalMinutes < endMinutes) {
                // Today morning after midnight (the start was yesterday)
                val yesterdayDay = (currentDay - 2 + 7) % 7 + 1
                days.contains(yesterdayDay)
            } else {
                false
            }
        } else {
            // start == end: 24-hour active block on selected days
            days.contains(currentDay)
        }
    }

    private fun normalizeName(name: String): String {
        return name.lowercase()
            .replace("&", "and")
            .replace("blocker", "")
            .replace("content", "")
            .replace("nsfw", "adult")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    /**
     * Checks if a [blockList] matches the schedule's active guards.
     * Matches by:
     * 1. Exact list ID (e.g., "1, 2, 5")
     * 2. Exact list name (case-insensitive)
     * 3. Normalized keyword matching (e.g., "Adult Content" matches "Adult & NSFW Blocker")
     */
    fun isListGuardedBySchedule(schedule: Schedule, blockList: BlockList): Boolean {
        return isListGuardedByTokens(schedule.activeListNames, blockList, checkListEnabled = false)
    }

    /**
     * Checks if a [blockList] matches a raw active list token string (from schedule or session).
     */
    fun isListGuardedByTokens(
        activeTokensRaw: String,
        blockList: BlockList,
        checkListEnabled: Boolean = false
    ): Boolean {
        if (checkListEnabled && !blockList.isEnabled) return false
        if (activeTokensRaw.isBlank()) return true // default: all lists
        val tokens = activeTokensRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true

        val listIdStr = blockList.id.toString()
        val listNameLower = blockList.name.lowercase().trim()
        val normListName = normalizeName(blockList.name)

        for (token in tokens) {
            val tokenLower = token.lowercase().trim()
            val normToken = normalizeName(token)

            if (token == listIdStr ||
                tokenLower == listNameLower ||
                (normToken.isNotEmpty() && (normListName.contains(normToken) || normToken.contains(normListName)))
            ) {
                return true
            }
        }
        return false
    }
}
