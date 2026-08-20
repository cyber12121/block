package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.example.FocusGuardApp
import com.example.data.AppRepository
import com.example.data.model.BlockedTarget
import com.example.data.model.FocusSession
import com.example.data.model.PlantStatus
import com.example.data.model.PlantType
import com.example.data.model.Schedule
import com.example.data.model.TargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ActiveSessionState(
    val isActive: Boolean = false,
    val isStrictMode: Boolean = false,
    val isUltraStrict: Boolean = false,
    val sessionId: Long = 0,
    val title: String = "Focus Session",
    val startTimeMillis: Long = 0,
    val endTimeMillis: Long = 0,
    val durationMinutes: Int = 0,
    val remainingSeconds: Long = 0,
    val activeListNames: String = "",
    val isTampered: Boolean = false,
    val isAutoScheduled: Boolean = false,
    val isPomodoro: Boolean = false,
    val pomodoroRound: Int = 1,
    val pomodoroTotalRounds: Int = 4,
    val isPomodoroBreak: Boolean = false,
    val plantType: PlantType = PlantType.SPROUT
)

class FocusSessionManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _sessionState = MutableStateFlow(ActiveSessionState())
    val sessionStateFlow: StateFlow<ActiveSessionState> = _sessionState.asStateFlow()
    val sessionState: StateFlow<ActiveSessionState> = sessionStateFlow

    // Guards against the expiry path running more than once. updateTick() is driven by
    // both FocusForegroundService and MainViewModel (once per second each), so without
    // this the session would be "completed" repeatedly, double-recording stats.
    @Volatile private var isCompletingSession = false

    // Cached fast-lookup sets for the Accessibility / Monitoring Service
    @Volatile private var cachedBlockedPackages: Set<String> = emptySet()
    @Volatile private var cachedBlockedDomains: Set<String> = emptySet()
    @Volatile private var cachedBlockedKeywords: Set<String> = emptySet()

    init {
        restoreSessionFromPrefs()
    }

    fun startSession(
        title: String,
        durationMinutes: Int,
        isStrictMode: Boolean,
        activeListNames: List<String>,
        isAutoScheduled: Boolean = false,
        isPomodoro: Boolean = false,
        pomodoroRound: Int = 1,
        pomodoroTotalRounds: Int = 4,
        isPomodoroBreak: Boolean = false,
        isUltraStrict: Boolean = false,
        plantType: PlantType = when {
            durationMinutes >= 120 -> PlantType.ANCIENT_REDWOOD
            durationMinutes >= 90 -> PlantType.GOLDEN_LOTUS
            durationMinutes >= 60 -> PlantType.OAK_TREE
            durationMinutes >= 45 -> PlantType.CHERRY_BLOSSOM
            durationMinutes >= 25 -> PlantType.SUCCULENT
            else -> PlantType.SPROUT
        }
    ) {
        val app = context.applicationContext as? FocusGuardApp
        val repository = app?.repository ?: return
        startSession(
            repository = repository,
            title = title,
            durationMinutes = durationMinutes,
            isStrictMode = isStrictMode,
            activeLists = activeListNames,
            isAutoScheduled = isAutoScheduled,
            isPomodoro = isPomodoro,
            pomodoroRound = pomodoroRound,
            pomodoroTotalRounds = pomodoroTotalRounds,
            isPomodoroBreak = isPomodoroBreak,
            isUltraStrict = isUltraStrict,
            plantType = plantType
        )
    }

    fun startSession(
        repository: AppRepository,
        title: String,
        durationMinutes: Int,
        isStrictMode: Boolean,
        activeLists: List<String>,
        isAutoScheduled: Boolean = false,
        scheduleId: Long = -1L,
        isPomodoro: Boolean = false,
        pomodoroRound: Int = 1,
        pomodoroTotalRounds: Int = 4,
        isPomodoroBreak: Boolean = false,
        isUltraStrict: Boolean = false,
        plantType: PlantType = when {
            durationMinutes >= 120 -> PlantType.ANCIENT_REDWOOD
            durationMinutes >= 90 -> PlantType.GOLDEN_LOTUS
            durationMinutes >= 60 -> PlantType.OAK_TREE
            durationMinutes >= 45 -> PlantType.CHERRY_BLOSSOM
            durationMinutes >= 25 -> PlantType.SUCCULENT
            else -> PlantType.SPROUT
        }
    ) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000L)
        val elapsedRealtime = SystemClock.elapsedRealtime()

        // A new session supersedes any in-flight expiry teardown.
        isCompletingSession = false

        scope.launch {
            val session = FocusSession(
                title = title,
                startTimeMillis = now,
                scheduledEndTimeMillis = endTime,
                durationMinutes = durationMinutes,
                isStrictMode = isStrictMode,
                activeListNames = activeLists.joinToString(", ")
            )
            val id = repository.insertSession(session)

            if (!isPomodoroBreak) {
                repository.plantSeed(plantType, durationMinutes, title)
            }

            // Save to prefs with tamper-evident markers.
            // Also resets the per-session emergency-exit counter so the user gets
            // a fresh quota of 5 exits for every new session (not a permanent lifetime limit).
            prefs.edit()
                .putBoolean(KEY_IS_ACTIVE, true)
                .putBoolean(KEY_IS_STRICT, isStrictMode)
                .putBoolean(KEY_IS_ULTRA_STRICT, isUltraStrict)
                .putLong(KEY_SESSION_ID, id)
                .putString(KEY_TITLE, title)
                .putLong(KEY_START_TIME, now)
                .putLong(KEY_END_TIME, endTime)
                .putInt(KEY_DURATION_MINUTES, durationMinutes)
                .putLong(KEY_ELAPSED_BASELINE, elapsedRealtime)
                .putString(KEY_ACTIVE_LISTS, activeLists.joinToString(", "))
                .putBoolean(KEY_IS_AUTO_SCHEDULED, isAutoScheduled)
                .putBoolean(KEY_IS_POMODORO, isPomodoro)
                .putInt(KEY_POMODORO_ROUND, pomodoroRound)
                .putInt(KEY_POMODORO_TOTAL, pomodoroTotalRounds)
                .putBoolean(KEY_IS_POMODORO_BREAK, isPomodoroBreak)
                .putString(KEY_PLANT_TYPE, plantType.name)
                .putLong(KEY_SCHEDULE_ID, if (isAutoScheduled) scheduleId else -1L)
                .remove(KEY_SNOOZED_SCHEDULE_ID) // clear any lingering snooze on new session start
                .putInt(KEY_EMERGENCY_EXITS_USED, 0) // reset per-session exit quota
                .apply()

            refreshBlockedTargetsCache(repository)
            updateStateFromValues(
                isActive = true,
                isStrictMode = isStrictMode,
                isUltraStrict = isUltraStrict,
                sessionId = id,
                title = title,
                startTime = now,
                endTime = endTime,
                durationMinutes = durationMinutes,
                activeLists = activeLists.joinToString(", "),
                isAutoScheduled = isAutoScheduled,
                isPomodoro = isPomodoro,
                pomodoroRound = pomodoroRound,
                pomodoroTotalRounds = pomodoroTotalRounds,
                isPomodoroBreak = isPomodoroBreak,
                plantType = plantType
            )

            FocusTileService.requestTileUpdate(context)
        }
    }

    fun stopSession(earlyUnlocked: Boolean = false) {
        val app = context.applicationContext as? FocusGuardApp
        val repository = app?.repository ?: return
        endSession(repository, earlyUnlocked)
    }

    fun endSession(repository: AppRepository, earlyUnlocked: Boolean = false) {
        val currentState = _sessionState.value
        if (!currentState.isActive) return

        // In Strict Mode, if remaining time is > 0 and not forced earlyUnlocked, refuse unlock
        if (currentState.isStrictMode && !earlyUnlocked && getRemainingSeconds() > 0) {
            return
        }

        forceUnlockSession(repository, earlyUnlocked = earlyUnlocked)
    }

    fun forceUnlockSession(repository: AppRepository, earlyUnlocked: Boolean = true): Boolean {
        val currentState = _sessionState.value
        val now = System.currentTimeMillis()

        // Ultra Strict Lockdown Enforcement (Automated Schedules only):
        // If Ultra Strict Mode is active and remaining time > 0, exit is strictly forbidden
        // unless Developer Mode is active.
        if (earlyUnlocked && currentState.isUltraStrict && currentState.isAutoScheduled && getRemainingSeconds() > 0 && !isDeveloperModeActive()) {
            return false
        }

        if (earlyUnlocked) {
            val authManager = com.example.data.auth.AuthManager.getInstance(context)
            if (!authManager.consumeDailyExit()) {
                return false
            }
        }

        scope.launch {
            val session = repository.getActiveSession()
            if (session != null) {
                repository.updateSession(
                    session.copy(
                        isCompleted = !earlyUnlocked,
                        actualEndTimeMillis = now,
                        wasEarlyUnlocked = earlyUnlocked
                    )
                )
            }

            val growingPlant = repository.getCurrentGrowingPlant()
            if (growingPlant != null) {
                if (earlyUnlocked) {
                    repository.markPlantWithered(growingPlant)
                } else {
                    repository.markPlantBloomed(growingPlant)
                }
            }

            if (!earlyUnlocked && currentState.durationMinutes > 0) {
                repository.recordCompletedSession(currentState.durationMinutes)
            }

            // Read the schedule ID before clearing prefs (needed for the snooze below)
            val scheduledSessionId = prefs.getLong(KEY_SCHEDULE_ID, -1L)

            // Developer Emergency Unlock: Turn OFF all schedules to prevent immediate re-locking
            if (earlyUnlocked) {
                repository.disableAllSchedules()
            }

            // Clear prefs
            clearSessionPrefs()
            stopMinimalStrictLock()
            setMinimalLauncherActive(false)

            // Normal (non-emergency) end of a schedule-triggered session:
            // Cancel the lingering end-alarm and snooze this schedule for the rest
            // of the current window so checkAutomaticSchedules() won't immediately
            // re-start it.
            if (!earlyUnlocked && currentState.isAutoScheduled && scheduledSessionId >= 0) {
                prefs.edit().putLong(KEY_SNOOZED_SCHEDULE_ID, scheduledSessionId).apply()
                val allSchedules = repository.getEnabledSchedules()
                ScheduleAlarmManager.rescheduleAll(context, allSchedules)
            }

            _sessionState.value = ActiveSessionState()

            // Re-populate cache based on currently enabled block lists
            refreshBlockedTargetsCache(repository)
            FocusTileService.requestTileUpdate(context)
            isCompletingSession = false
        }
        return true
    }

    fun updateTick() {
        val currentState = _sessionState.value
        if (!currentState.isActive) return

        val remaining = getRemainingSeconds()
        if (remaining <= 0) {
            // Timer expired: run the full end-of-session teardown exactly once.
            //
            // Previously this branch only bloomed the plant and recorded stats inline; it
            // never marked the FocusSession row completed, never cleared the persisted
            // prefs and never reset _sessionState.isActive. The session therefore stayed
            // "active" forever (surviving restarts via restoreSessionFromPrefs) and, since
            // two callers tick every second, the plant/stats writes were repeated endlessly.
            if (isCompletingSession) return

            val repository = (context.applicationContext as? FocusGuardApp)?.repository
            if (repository == null) {
                _sessionState.value = currentState.copy(remainingSeconds = 0)
                return
            }

            isCompletingSession = true
            _sessionState.value = currentState.copy(remainingSeconds = 0)

            // earlyUnlocked = false -> marks the session completed, blooms the plant,
            // records the stats, clears prefs and refreshes the blocked-target cache.
            forceUnlockSession(repository, earlyUnlocked = false)
        } else {
            // Check for clock tampering:
            val elapsedSinceStart = (SystemClock.elapsedRealtime() - prefs.getLong(KEY_ELAPSED_BASELINE, 0L)) / 1000
            val expectedRemaining = (currentState.durationMinutes * 60L) - elapsedSinceStart
            val isClockManipulated = expectedRemaining > 0 && remaining <= 0

            _sessionState.value = currentState.copy(
                remainingSeconds = if (isClockManipulated) expectedRemaining.coerceAtLeast(0) else remaining,
                isTampered = isClockManipulated
            )
        }
    }

    suspend fun checkAutomaticSchedules(repository: AppRepository) {
        val enabledSchedules = repository.getEnabledSchedules()
        if (enabledSchedules.isEmpty()) return

        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon ... 7=Sat
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        var activeScheduleFound: Schedule? = null
        var calculatedRemainingMins = 0

        for (schedule in enabledSchedules) {
            val days = schedule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!days.contains(currentDay)) continue

            val startMinutes = schedule.startHour * 60 + schedule.startMinute
            val endMinutes = schedule.endHour * 60 + schedule.endMinute

            val isMatch = if (startMinutes < endMinutes) {
                // Same day window (e.g. 09:00 to 17:00 or 08:00 to 20:00)
                currentTotalMinutes in startMinutes until endMinutes
            } else {
                // Overnight window (e.g. 22:00 to 06:00)
                currentTotalMinutes >= startMinutes || currentTotalMinutes < endMinutes
            }

            if (isMatch) {
                val remainingMins = if (startMinutes < endMinutes) {
                    endMinutes - currentTotalMinutes
                } else {
                    if (currentTotalMinutes >= startMinutes) {
                        (1440 - currentTotalMinutes) + endMinutes
                    } else {
                        endMinutes - currentTotalMinutes
                    }
                }.coerceAtLeast(1)

                activeScheduleFound = schedule
                calculatedRemainingMins = remainingMins
                break
            }
        }

        if (activeScheduleFound != null) {
            val schedule = activeScheduleFound
            // If the user manually ended this schedule's session, skip re-triggering
            // for the rest of the current window. The snooze is cleared once the
            // window passes (see the else branch below).
            val snoozedId = prefs.getLong(KEY_SNOOZED_SCHEDULE_ID, -1L)
            if (schedule.id == snoozedId) return
            if (!_sessionState.value.isActive) {
                val activeLists = if (schedule.activeListNames.isNotBlank()) {
                    schedule.activeListNames.split(",").map { it.trim() }
                } else {
                    repository.getActiveLists().map { it.name }
                }

                startSession(
                    repository = repository,
                    title = "Schedule: ${schedule.name}",
                    durationMinutes = calculatedRemainingMins,
                    isStrictMode = schedule.isStrictMode,
                    isUltraStrict = schedule.isUltraStrict,
                    activeLists = activeLists,
                    isAutoScheduled = true,
                    scheduleId = schedule.id
                )
            }
        } else {
            // No active schedule window: clear any lingering snooze so the next
            // scheduled window fires correctly.
            if (prefs.getLong(KEY_SNOOZED_SCHEDULE_ID, -1L) >= 0) {
                prefs.edit().remove(KEY_SNOOZED_SCHEDULE_ID).apply()
            }
            if (_sessionState.value.isAutoScheduled) {
                // Auto-scheduled window has passed
                val remaining = getRemainingSeconds()
                if (remaining <= 0) {
                    endSession(repository, earlyUnlocked = false)
                }
            }
        }
    }

    private fun getRemainingSeconds(): Long {
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val now = System.currentTimeMillis()
        return ((endTime - now) / 1000).coerceAtLeast(0)
    }

    fun hasNoBlockedRules(): Boolean {
        return cachedBlockedPackages.isEmpty() && cachedBlockedDomains.isEmpty() && cachedBlockedKeywords.isEmpty()
    }

    suspend fun refreshBlockedTargetsCache(repository: AppRepository) {
        val isSessionActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val activeListNamesRaw = prefs.getString(KEY_ACTIVE_LISTS, "") ?: ""

        // Build the set of list IDs whose targets should be enforced.
        // During a session: use the session's selected lists (by name). If none match
        // (e.g. names changed), fall back to all enabled lists so blocking never silently
        // goes dark. Outside a session: always use all enabled lists so passive blocking
        // (website/keyword rules on always-on lists) still fires.
        val allLists = repository.getActiveLists()
        val enabledListIds = allLists.filter { it.isEnabled }.map { it.id }.toSet()

        val validListIds = if (isSessionActive && activeListNamesRaw.isNotBlank()) {
            val sessionListNames = activeListNamesRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            val matched = allLists.filter { it.name in sessionListNames && it.isEnabled }.map { it.id }.toSet()
            // Fall back to all enabled lists if session list names no longer match anything.
            if (matched.isNotEmpty()) matched else enabledListIds
        } else {
            // No session OR session started with no explicit list selection:
            // load all enabled lists so passive blocking is always warm.
            enabledListIds
        }

        val targets = repository.getAllEnabledTargets()
        val pkgSet = mutableSetOf<String>()
        val domainSet = mutableSetOf<String>()
        val keywordSet = mutableSetOf<String>()

        for (t in targets) {
            if (validListIds.contains(t.listId)) {
                when (t.targetType) {
                    TargetType.APP -> {
                        val cleanPkg = t.identifier.lowercase().trim()
                        if (cleanPkg.isNotBlank()) {
                            pkgSet.add(cleanPkg)
                        }
                    }
                    TargetType.WEBSITE -> {
                        val cleanDomain = t.identifier.lowercase().trim()
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .removePrefix("www.")
                            .trimEnd('/')
                        if (cleanDomain.isNotBlank()) {
                            domainSet.add(cleanDomain)
                        }
                    }
                    TargetType.KEYWORD -> {
                        val cleanKw = t.identifier.lowercase().trim()
                        if (cleanKw.isNotBlank()) {
                            keywordSet.add(cleanKw)
                        }
                    }
                }
            }
        }

        cachedBlockedPackages = pkgSet
        cachedBlockedDomains = domainSet
        cachedBlockedKeywords = keywordSet
    }

    fun isAppBlocked(packageName: String): Boolean {
        val pkgLower = packageName.lowercase().trim()
        if (pkgLower.isBlank()) return false
        if (cachedBlockedPackages.contains(pkgLower)) return true
        return cachedBlockedPackages.any { blocked ->
            if (blocked.contains('.')) {
                // Real package name: exact match or sub-package match only
                // (e.g. "com.google.android.youtube" also matches "com.google.android.youtube.tv")
                pkgLower == blocked || pkgLower.startsWith("$blocked.")
            } else {
                // Free-text entry like "instagram": substring match, but ignore tiny
                // fragments that would match half the phone.
                blocked.length >= 4 && pkgLower.contains(blocked)
            }
        }
    }

    fun isUrlOrKeywordBlocked(urlOrTitle: String): Pair<Boolean, String> {
        val lower = urlOrTitle.lowercase().trim()
        if (lower.isBlank()) return Pair(false, "")

        val cleanInput = lower
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")

        // 1. Check blocked website domains against the URL / search query text
        for (domain in cachedBlockedDomains) {
            if (domain.isNotBlank()) {
                val cleanDomain = domain.lowercase().trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")

                // Full domain appears in the URL (e.g. "youtube.com/watch?v=...")
                if (lower.contains(cleanDomain) || cleanInput.contains(cleanDomain)) {
                    return Pair(true, domain)
                }

                // Site name typed as a search query (e.g. "youtube cat videos").
                // Match as a whole word so "mytube.org" doesn't trigger "tube".
                val rootName = cleanDomain.substringBeforeLast(".")
                if (rootName.length >= 4) {
                    val wordRegex = Regex("(^|[^a-z0-9])${Regex.escape(rootName)}([^a-z0-9]|$)")
                    if (wordRegex.containsMatchIn(cleanInput)) {
                        return Pair(true, domain)
                    }
                }
            }
        }

        // 2. Check blocked keywords (use word boundary regex to avoid partial substring false triggers)
        for (kw in cachedBlockedKeywords) {
            val cleanKw = kw.lowercase().trim()
            if (cleanKw.length >= 2) {
                val kwRegex = Regex("(^|[^a-z0-9])${Regex.escape(cleanKw)}([^a-z0-9]|$)")
                if (kwRegex.containsMatchIn(lower) || kwRegex.containsMatchIn(cleanInput)) {
                    return Pair(true, kw)
                }
            }
        }
        return Pair(false, "")
    }

    private fun restoreSessionFromPrefs() {
        val isActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        if (!isActive) {
            val app = context.applicationContext as? FocusGuardApp
            app?.let {
                scope.launch {
                    refreshBlockedTargetsCache(it.repository)
                }
            }
            return
        }

        val isStrict = prefs.getBoolean(KEY_IS_STRICT, false)
        val isUltraStrict = prefs.getBoolean(KEY_IS_ULTRA_STRICT, false)
        val id = prefs.getLong(KEY_SESSION_ID, 0L)
        val title = prefs.getString(KEY_TITLE, "Focus Session") ?: "Focus Session"
        val start = prefs.getLong(KEY_START_TIME, 0L)
        val end = prefs.getLong(KEY_END_TIME, 0L)
        val duration = prefs.getInt(KEY_DURATION_MINUTES, 0)
        val activeLists = prefs.getString(KEY_ACTIVE_LISTS, "") ?: ""
        val isAutoScheduled = prefs.getBoolean(KEY_IS_AUTO_SCHEDULED, false)
        val isPomodoro = prefs.getBoolean(KEY_IS_POMODORO, false)
        val pomodoroRound = prefs.getInt(KEY_POMODORO_ROUND, 1)
        val pomodoroTotal = prefs.getInt(KEY_POMODORO_TOTAL, 4)
        val isPomodoroBreak = prefs.getBoolean(KEY_IS_POMODORO_BREAK, false)
        val plantTypeName = prefs.getString(KEY_PLANT_TYPE, PlantType.SPROUT.name) ?: PlantType.SPROUT.name
        val plantType = try { PlantType.valueOf(plantTypeName) } catch (_: Exception) { PlantType.SPROUT }

        val remaining = ((end - System.currentTimeMillis()) / 1000).coerceAtLeast(0)

        if (remaining <= 0) {
            updateStateFromValues(
                isActive = true,
                isStrictMode = isStrict,
                isUltraStrict = isUltraStrict,
                sessionId = id,
                title = title,
                startTime = start,
                endTime = end,
                durationMinutes = duration,
                activeLists = activeLists,
                isAutoScheduled = isAutoScheduled,
                isPomodoro = isPomodoro,
                pomodoroRound = pomodoroRound,
                pomodoroTotalRounds = pomodoroTotal,
                isPomodoroBreak = isPomodoroBreak,
                plantType = plantType
            )
            val app = context.applicationContext as? FocusGuardApp
            if (app != null) {
                isCompletingSession = true
                forceUnlockSession(app.repository, earlyUnlocked = false)
            } else {
                clearSessionPrefs()
                _sessionState.value = ActiveSessionState()
            }
            return
        }

        updateStateFromValues(
            isActive = true,
            isStrictMode = isStrict,
            isUltraStrict = isUltraStrict,
            sessionId = id,
            title = title,
            startTime = start,
            endTime = end,
            durationMinutes = duration,
            activeLists = activeLists,
            isAutoScheduled = isAutoScheduled,
            isPomodoro = isPomodoro,
            pomodoroRound = pomodoroRound,
            pomodoroTotalRounds = pomodoroTotal,
            isPomodoroBreak = isPomodoroBreak,
            plantType = plantType
        )

        val app = context.applicationContext as? FocusGuardApp
        app?.let {
            scope.launch {
                refreshBlockedTargetsCache(it.repository)
            }
        }
    }

    private fun updateStateFromValues(
        isActive: Boolean,
        isStrictMode: Boolean,
        isUltraStrict: Boolean = false,
        sessionId: Long,
        title: String,
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
        activeLists: String,
        isAutoScheduled: Boolean = false,
        isPomodoro: Boolean = false,
        pomodoroRound: Int = 1,
        pomodoroTotalRounds: Int = 4,
        isPomodoroBreak: Boolean = false,
        plantType: PlantType = PlantType.SPROUT
    ) {
        val remaining = ((endTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        _sessionState.value = ActiveSessionState(
            isActive = isActive,
            isStrictMode = isStrictMode,
            isUltraStrict = isUltraStrict,
            sessionId = sessionId,
            title = title,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            durationMinutes = durationMinutes,
            remainingSeconds = remaining,
            activeListNames = activeLists,
            isAutoScheduled = isAutoScheduled,
            isPomodoro = isPomodoro,
            pomodoroRound = pomodoroRound,
            pomodoroTotalRounds = pomodoroTotalRounds,
            isPomodoroBreak = isPomodoroBreak,
            plantType = plantType
        )
    }

    /**
     * Removes only the keys that describe the *current session*.
     *
     * Deliberately not `prefs.edit().clear()`: that also wiped the user's configured
     * essential apps and reset KEY_EMERGENCY_EXITS_USED, handing back the full
     * emergency-exit quota every time a session ended.
     */
    private fun clearSessionPrefs() {
        prefs.edit()
            .remove(KEY_IS_ACTIVE)
            .remove(KEY_IS_STRICT)
            .remove(KEY_IS_ULTRA_STRICT)
            .remove(KEY_SESSION_ID)
            .remove(KEY_TITLE)
            .remove(KEY_START_TIME)
            .remove(KEY_END_TIME)
            .remove(KEY_DURATION_MINUTES)
            .remove(KEY_ELAPSED_BASELINE)
            .remove(KEY_ACTIVE_LISTS)
            .remove(KEY_IS_AUTO_SCHEDULED)
            .remove(KEY_IS_POMODORO)
            .remove(KEY_POMODORO_ROUND)
            .remove(KEY_POMODORO_TOTAL)
            .remove(KEY_IS_POMODORO_BREAK)
            .remove(KEY_PLANT_TYPE)
            .remove(KEY_SCHEDULE_ID)
            .apply()
    }

    fun getCustomEssentialApps(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_ESSENTIAL_APPS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        val all = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }

        // One-time migration: strip packages that were hardcoded as defaults in older
        // builds (dialer, camera). Users who never explicitly chose these should not
        // see them in their essential-apps list. If the filtered list differs from the
        // stored list, persist the clean version immediately so this path only runs once.
        val legacy = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.camera",
            "org.codeaurora.snapcam",
            "com.samsung.android.app.camera"
        )
        val cleaned = all.filter { it.lowercase() !in legacy }.take(5)
        if (cleaned.size != all.size) {
            prefs.edit().putString(KEY_CUSTOM_ESSENTIAL_APPS, cleaned.joinToString(",")).apply()
        }
        return cleaned
    }

    fun saveCustomEssentialApps(packages: List<String>) {
        val clean = packages.take(5).joinToString(",")
        prefs.edit().putString(KEY_CUSTOM_ESSENTIAL_APPS, clean).apply()
    }

    fun isMinimalLauncherActive(): Boolean {
        if (isMinimalStrictLockActive() && !hasUsedAllMinimalStrictExits()) {
            return true
        }
        return prefs.getBoolean(KEY_IS_MINIMAL_ACTIVE, false)
    }

    fun setMinimalLauncherActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_IS_MINIMAL_ACTIVE, active).apply()
    }

    fun startMinimalStrictLock(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + durationMinutes * 60 * 1000L
        prefs.edit()
            .putLong(KEY_MINIMAL_STRICT_END_TIME, endTime)
            .putInt(KEY_MINIMAL_STRICT_DURATION_MINUTES, durationMinutes)
            .putInt(KEY_MINIMAL_STRICT_EXITS_USED, 0)
            .putBoolean(KEY_MINIMAL_STRICT_USED_EXIT, false)
            .putBoolean(KEY_IS_MINIMAL_ACTIVE, true)
            .apply()
    }

    fun isMinimalStrictLockActive(): Boolean {
        return getMinimalStrictLockRemainingMillis() > 0
    }

    fun getMinimalStrictLockRemainingMillis(): Long {
        val endTime = prefs.getLong(KEY_MINIMAL_STRICT_END_TIME, 0L)
        val remaining = endTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun getMinimalStrictExitsUsed(): Int {
        return prefs.getInt(KEY_MINIMAL_STRICT_EXITS_USED, 0)
    }

    fun getMinimalStrictExitsRemaining(): Int {
        if (isDeveloperModeActive()) return MAX_MINIMAL_STRICT_EXITS
        return (MAX_MINIMAL_STRICT_EXITS - getMinimalStrictExitsUsed()).coerceAtLeast(0)
    }

    fun hasUsedMinimalStrictExit(): Boolean {
        return getMinimalStrictExitsUsed() > 0
    }

    fun hasUsedAllMinimalStrictExits(): Boolean {
        return getMinimalStrictExitsRemaining() <= 0
    }

    fun useMinimalStrictExit(): Boolean {
        if (isDeveloperModeActive()) {
            stopMinimalStrictLock()
            setMinimalLauncherActive(false)
            return true
        }
        val remaining = getMinimalStrictExitsRemaining()
        if (remaining <= 0) {
            return false
        }
        val authManager = com.example.data.auth.AuthManager.getInstance(context)
        if (!authManager.consumeDailyExit()) {
            return false
        }
        val newUsed = getMinimalStrictExitsUsed() + 1
        prefs.edit().putInt(KEY_MINIMAL_STRICT_EXITS_USED, newUsed).apply()

        if (newUsed >= MAX_MINIMAL_STRICT_EXITS) {
            stopMinimalStrictLock()
        }
        setMinimalLauncherActive(false)
        return true
    }

    fun stopMinimalStrictLock() {
        prefs.edit()
            .putLong(KEY_MINIMAL_STRICT_END_TIME, 0L)
            .putInt(KEY_MINIMAL_STRICT_EXITS_USED, 0)
            .putBoolean(KEY_MINIMAL_STRICT_USED_EXIT, false)
            .putBoolean(KEY_IS_MINIMAL_ACTIVE, false)
            .apply()
    }

    fun getRemainingEmergencyExits(): Int {
        val authManager = com.example.data.auth.AuthManager.getInstance(context)
        authManager.refreshDailyExits()
        return authManager.dailyExitsRemaining.value
    }

    fun isDeveloperModeActive(): Boolean {
        return com.example.data.auth.AuthManager.getInstance(context).isDeveloperMode.value
    }

    fun useEmergencyExit(): Boolean {
        if (_sessionState.value.isUltraStrict && _sessionState.value.isAutoScheduled && !isDeveloperModeActive()) {
            return false
        }
        val authManager = com.example.data.auth.AuthManager.getInstance(context)
        if (!authManager.consumeDailyExit()) {
            return false
        }
        stopSession(earlyUnlocked = true)
        stopMinimalStrictLock()
        setMinimalLauncherActive(false)
        return true
    }

    companion object {
        private const val MAX_EMERGENCY_EXITS = 5
        private const val KEY_EMERGENCY_EXITS_USED = "key_emergency_exits_used"
        private const val KEY_IS_MINIMAL_ACTIVE = "key_is_minimal_active"
        private const val PREFS_NAME = "focus_guard_secure_state"
        private const val KEY_IS_ACTIVE = "key_is_active"
        private const val KEY_IS_STRICT = "key_is_strict"
        private const val KEY_IS_ULTRA_STRICT = "key_is_ultra_strict"
        private const val KEY_SESSION_ID = "key_session_id"
        private const val KEY_TITLE = "key_title"
        private const val KEY_START_TIME = "key_start_time"
        private const val KEY_END_TIME = "key_end_time"
        private const val KEY_DURATION_MINUTES = "key_duration_minutes"
        private const val KEY_ELAPSED_BASELINE = "key_elapsed_baseline"
        private const val KEY_ACTIVE_LISTS = "key_active_lists"
        private const val KEY_IS_AUTO_SCHEDULED = "key_is_auto_scheduled"
        private const val KEY_IS_POMODORO = "key_is_pomodoro"
        private const val KEY_POMODORO_ROUND = "key_pomodoro_round"
        private const val KEY_POMODORO_TOTAL = "key_pomodoro_total"
        private const val KEY_IS_POMODORO_BREAK = "key_is_pomodoro_break"
        private const val KEY_PLANT_TYPE = "key_plant_type"
        private const val KEY_SCHEDULE_ID = "key_schedule_id"
        private const val KEY_SNOOZED_SCHEDULE_ID = "key_snoozed_schedule_id"
        private const val KEY_CUSTOM_ESSENTIAL_APPS = "key_custom_essential_apps"
        private const val MAX_MINIMAL_STRICT_EXITS = 3
        private const val KEY_MINIMAL_STRICT_EXITS_USED = "key_minimal_strict_exits_used"
        private const val KEY_MINIMAL_STRICT_DURATION_MINUTES = "key_minimal_strict_duration_minutes"
        private const val KEY_MINIMAL_STRICT_END_TIME = "key_minimal_strict_end_time"
        private const val KEY_MINIMAL_STRICT_DURATION_HOURS = "key_minimal_strict_duration_hours"
        private const val KEY_MINIMAL_STRICT_USED_EXIT = "key_minimal_strict_used_exit"

        @Volatile
        private var INSTANCE: FocusSessionManager? = null

        fun getInstance(context: Context): FocusSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FocusSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
