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
        isPomodoro: Boolean = false,
        pomodoroRound: Int = 1,
        pomodoroTotalRounds: Int = 4,
        isPomodoroBreak: Boolean = false,
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

            // Save to prefs with tamper-evident markers
            prefs.edit()
                .putBoolean(KEY_IS_ACTIVE, true)
                .putBoolean(KEY_IS_STRICT, isStrictMode)
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
                .apply()

            refreshBlockedTargetsCache(repository)
            updateStateFromValues(
                isActive = true,
                isStrictMode = isStrictMode,
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

    fun forceUnlockSession(repository: AppRepository, earlyUnlocked: Boolean = true) {
        val currentState = _sessionState.value
        val now = System.currentTimeMillis()

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

            // Developer Emergency Unlock: Turn OFF all schedules to prevent immediate re-locking
            if (earlyUnlocked) {
                repository.disableAllSchedules()
            }

            // Clear prefs
            prefs.edit().clear().apply()

            _sessionState.value = ActiveSessionState()

            // Re-populate cache based on currently enabled block lists
            refreshBlockedTargetsCache(repository)
            FocusTileService.requestTileUpdate(context)
        }
    }

    fun updateTick() {
        val currentState = _sessionState.value
        if (!currentState.isActive) return

        val remaining = getRemainingSeconds()
        if (remaining <= 0) {
            // Auto complete session
            val app = context.applicationContext as? FocusGuardApp
            app?.let {
                scope.launch {
                    val growingPlant = it.repository.getCurrentGrowingPlant()
                    if (growingPlant != null) {
                        it.repository.markPlantBloomed(growingPlant)
                    }
                    if (currentState.durationMinutes > 0) {
                        it.repository.recordCompletedSession(currentState.durationMinutes)
                    }
                }
            }
            _sessionState.value = currentState.copy(remainingSeconds = 0)
            FocusTileService.requestTileUpdate(context)
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
                    activeLists = activeLists,
                    isAutoScheduled = true
                )
            }
        } else if (_sessionState.value.isAutoScheduled) {
            // Auto-scheduled window has passed
            val remaining = getRemainingSeconds()
            if (remaining <= 0) {
                endSession(repository, earlyUnlocked = false)
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
        val activeListNamesSet = if (isSessionActive && activeListNamesRaw.isNotBlank()) {
            activeListNamesRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        } else {
            emptySet()
        }

        val allLists = repository.getActiveLists()
        val validListIds = if (activeListNamesSet.isNotEmpty()) {
            allLists.filter { it.name in activeListNamesSet && it.isEnabled }.map { it.id }.toSet()
        } else {
            allLists.filter { it.isEnabled }.map { it.id }.toSet()
        }

        val targets = repository.getAllEnabledTargets()
        val pkgSet = mutableSetOf<String>()
        val domainSet = mutableSetOf<String>()
        val keywordSet = mutableSetOf<String>()

        for (t in targets) {
            if (validListIds.isEmpty() || validListIds.contains(t.listId)) {
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
        return cachedBlockedPackages.contains(pkgLower) || cachedBlockedPackages.any { pkgLower.contains(it) || it.contains(pkgLower) }
    }

    fun isUrlOrKeywordBlocked(urlOrTitle: String): Pair<Boolean, String> {
        val lower = urlOrTitle.lowercase().trim()
        if (lower.isBlank()) return Pair(false, "")

        val cleanInput = lower
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")

        // 1. Check blocked website domains
        for (domain in cachedBlockedDomains) {
            if (domain.isNotBlank()) {
                val cleanDomain = domain.lowercase().trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")

                if (lower.contains(cleanDomain) || cleanInput.contains(cleanDomain)) {
                    return Pair(true, domain)
                }

                val rootName = cleanDomain.substringBeforeLast(".")
                if (rootName.length >= 3 && (
                        lower.contains(rootName) ||
                        cleanInput.contains(rootName)
                    )) {
                    return Pair(true, domain)
                }
            }
        }

        // 2. Check blocked keywords
        for (kw in cachedBlockedKeywords) {
            val cleanKw = kw.lowercase().trim()
            if (cleanKw.isNotBlank() && lower.contains(cleanKw)) {
                return Pair(true, kw)
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

        // If not strict and expired, clean up
        if (remaining <= 0 && !isStrict) {
            prefs.edit().clear().apply()
            val app = context.applicationContext as? FocusGuardApp
            app?.let {
                scope.launch {
                    refreshBlockedTargetsCache(it.repository)
                }
            }
            return
        }

        updateStateFromValues(
            isActive = true,
            isStrictMode = isStrict,
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

    fun getCustomEssentialApps(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_ESSENTIAL_APPS, "") ?: ""
        if (raw.isBlank()) {
            return listOf(
                "com.android.dialer",
                "com.google.android.apps.messaging",
                "com.android.camera",
                "com.google.android.deskclock",
                "com.android.settings"
            )
        }
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(5)
    }

    fun saveCustomEssentialApps(packages: List<String>) {
        val clean = packages.take(5).joinToString(",")
        prefs.edit().putString(KEY_CUSTOM_ESSENTIAL_APPS, clean).apply()
    }

    fun isMinimalLauncherActive(): Boolean {
        return prefs.getBoolean(KEY_IS_MINIMAL_ACTIVE, false)
    }

    fun setMinimalLauncherActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_IS_MINIMAL_ACTIVE, active).apply()
    }

    fun getRemainingEmergencyExits(): Int {
        val used = prefs.getInt(KEY_EMERGENCY_EXITS_USED, 0)
        return (MAX_EMERGENCY_EXITS - used).coerceAtLeast(0)
    }

    fun useEmergencyExit(): Boolean {
        val remaining = getRemainingEmergencyExits()
        if (remaining <= 0) return false

        val used = prefs.getInt(KEY_EMERGENCY_EXITS_USED, 0)
        prefs.edit().putInt(KEY_EMERGENCY_EXITS_USED, used + 1).apply()
        stopSession(earlyUnlocked = true)
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
        private const val KEY_CUSTOM_ESSENTIAL_APPS = "key_custom_essential_apps"

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
