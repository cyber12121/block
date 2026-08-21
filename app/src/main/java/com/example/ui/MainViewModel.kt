package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.model.BlockList
import com.example.data.model.BlockedTarget
import com.example.data.model.DailyStat
import com.example.data.model.GardenPlant
import com.example.data.model.PlantType
import com.example.data.model.Schedule
import com.example.data.model.TargetType
import com.example.service.ActiveSchedulesState
import com.example.service.ActiveSessionState
import com.example.service.FocusSessionManager
import com.example.service.ScheduleAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppRepository,
    private val sessionManager: FocusSessionManager,
    private val application: Application
) : ViewModel() {

    val sessionState: StateFlow<ActiveSessionState> = sessionManager.sessionState
    val activeSchedulesState: StateFlow<ActiveSchedulesState> = sessionManager.activeSchedulesState

    val blockLists: StateFlow<List<BlockList>> = repository.allBlockLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTargets: StateFlow<List<BlockedTarget>> = repository.allTargetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<Schedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMinutes: StateFlow<Int?> = repository.totalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedSessionsCount: StateFlow<Int> = repository.completedSessionsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalBlockedAttempts: StateFlow<Int?> = repository.totalBlockedAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentStats: StateFlow<List<DailyStat>> = repository.recentDailyStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Garden State
    val allGardenPlants: StateFlow<List<GardenPlant>> = repository.allGardenPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bloomedGardenPlants: StateFlow<List<GardenPlant>> = repository.bloomedGardenPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bloomedPlantsCount: StateFlow<Int> = repository.bloomedPlantsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val witheredPlantsCount: StateFlow<Int> = repository.witheredPlantsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Dialog & Interaction State
    private val _isStartSessionDialogOpen = MutableStateFlow(false)
    val isStartSessionDialogOpen: StateFlow<Boolean> = _isStartSessionDialogOpen.asStateFlow()

    private val _isCreateListDialogOpen = MutableStateFlow(false)
    val isCreateListDialogOpen: StateFlow<Boolean> = _isCreateListDialogOpen.asStateFlow()

    private val _isCreateScheduleDialogOpen = MutableStateFlow(false)
    val isCreateScheduleDialogOpen: StateFlow<Boolean> = _isCreateScheduleDialogOpen.asStateFlow()

    private val _scheduleToEdit = MutableStateFlow<Schedule?>(null)
    val scheduleToEdit: StateFlow<Schedule?> = _scheduleToEdit.asStateFlow()

    private val _selectedListForAddTarget = MutableStateFlow<BlockList?>(null)
    val selectedListForAddTarget: StateFlow<BlockList?> = _selectedListForAddTarget.asStateFlow()

    init {
        // NOTE: The FocusForegroundService owns the 1-second tick loop (updateTick +
        // checkAutomaticSchedules). Running a second loop here caused double DB reads
        // every 15 s and double updateTick() calls while the UI was visible.
        // Removing the duplicate loop here; the service keeps enforcement alive even
        // when the UI is not in the foreground.
    }

    fun openStartSessionDialog() {
        _isStartSessionDialogOpen.value = true
    }

    fun closeStartSessionDialog() {
        _isStartSessionDialogOpen.value = false
    }

    fun openCreateListDialog() {
        _isCreateListDialogOpen.value = true
    }

    fun closeCreateListDialog() {
        _isCreateListDialogOpen.value = false
    }

    fun openCreateScheduleDialog() {
        _scheduleToEdit.value = null
        _isCreateScheduleDialogOpen.value = true
    }

    fun openEditScheduleDialog(schedule: Schedule) {
        _scheduleToEdit.value = schedule
        _isCreateScheduleDialogOpen.value = true
    }

    fun closeCreateScheduleDialog() {
        _scheduleToEdit.value = null
        _isCreateScheduleDialogOpen.value = false
    }

    fun openAddTargetDialog(list: BlockList) {
        _selectedListForAddTarget.value = list
    }

    fun closeAddTargetDialog() {
        _selectedListForAddTarget.value = null
    }

    fun startFocusSession(
        title: String,
        durationMinutes: Int,
        isStrictMode: Boolean,
        activeListNames: List<String>,
        isPomodoro: Boolean = false,
        pomodoroRound: Int = 1,
        pomodoroTotalRounds: Int = 4,
        isPomodoroBreak: Boolean = false,
        isUltraStrict: Boolean = false
    ) {
        val plant = when {
            durationMinutes >= 120 -> PlantType.ANCIENT_REDWOOD
            durationMinutes >= 90 -> PlantType.GOLDEN_LOTUS
            durationMinutes >= 60 -> PlantType.OAK_TREE
            durationMinutes >= 45 -> PlantType.CHERRY_BLOSSOM
            durationMinutes >= 25 -> PlantType.SUCCULENT
            else -> PlantType.SPROUT
        }

        sessionManager.startSession(
            repository = repository,
            title = title.ifBlank { "Deep Focus Session" },
            durationMinutes = durationMinutes,
            isStrictMode = isStrictMode,
            isUltraStrict = isUltraStrict,
            activeLists = activeListNames,
            isPomodoro = isPomodoro,
            pomodoroRound = pomodoroRound,
            pomodoroTotalRounds = pomodoroTotalRounds,
            isPomodoroBreak = isPomodoroBreak,
            plantType = plant
        )
        closeStartSessionDialog()
    }

    fun transitionPomodoroStage(
        nextIsBreak: Boolean,
        nextRound: Int,
        durationMinutes: Int,
        isStrict: Boolean,
        activeListNames: List<String>
    ) {
        val currentTitle = if (nextIsBreak) "Pomodoro Break #$nextRound" else "Pomodoro Focus #$nextRound"
        sessionManager.startSession(
            repository = repository,
            title = currentTitle,
            durationMinutes = durationMinutes,
            isStrictMode = if (nextIsBreak) false else isStrict,
            activeLists = activeListNames,
            isPomodoro = true,
            pomodoroRound = nextRound,
            pomodoroTotalRounds = 4,
            isPomodoroBreak = nextIsBreak
        )
    }

    fun endCurrentSession(earlyUnlocked: Boolean = false) {
        sessionManager.endSession(repository, earlyUnlocked = earlyUnlocked)
    }

    fun forceEmergencyUnlock() {
        sessionManager.forceUnlockSession(repository, earlyUnlocked = true)
    }

    fun toggleBlockList(list: BlockList) {
        if (list.isEnabled && sessionManager.isSessionOrScheduleActive()) {
            // Cannot disable/unblock active block lists during an active focus or schedule session in any mode
            return
        }
        viewModelScope.launch {
            repository.updateBlockList(list.copy(isEnabled = !list.isEnabled))
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }

    fun toggleTarget(target: BlockedTarget) {
        if (target.isEnabled && sessionManager.isSessionOrScheduleActive()) {
            // Cannot disable/unblock active target rules during an active focus or schedule session in any mode
            return
        }
        viewModelScope.launch {
            repository.updateTarget(target.copy(isEnabled = !target.isEnabled))
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }

    fun updateTarget(target: BlockedTarget) {
        if (sessionManager.isSessionOrScheduleActive()) {
            // Cannot alter existing target identifiers during an active focus or schedule session
            return
        }
        viewModelScope.launch {
            repository.updateTarget(target)
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }

    fun addBulkTargets(listId: Long, type: TargetType, items: List<String>, category: String) {
        viewModelScope.launch {
            val targets = items.map { item ->
                val clean = item.trim()
                BlockedTarget(
                    listId = listId,
                    targetType = type,
                    identifier = clean,
                    displayName = clean.removePrefix("https://").removePrefix("http://").removePrefix("www."),
                    category = category.ifBlank { "Custom" }
                )
            }
            repository.insertTargets(targets)
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }

    fun deleteTarget(target: BlockedTarget) {
        if (sessionManager.isSessionOrScheduleActive()) {
            // Cannot delete targets during an active focus or schedule session
            return
        }
        viewModelScope.launch {
            repository.deleteTarget(target)
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }

    fun createBlockList(name: String, description: String, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            repository.insertBlockList(
                BlockList(
                    name = name.trim(),
                    description = description.trim(),
                    iconName = iconName,
                    colorHex = colorHex,
                    isEnabled = true,
                    isDefault = false
                )
            )
            sessionManager.refreshBlockedTargetsCache(repository)
        }
        closeCreateListDialog()
    }

    fun deleteBlockList(list: BlockList) {
        if (sessionManager.isStrictActive() || (sessionManager.isSessionOrScheduleActive() && list.isEnabled)) {
            // Cannot delete active or strict block lists during an active session
            return
        }
        viewModelScope.launch {
            repository.deleteBlockList(list)
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }

    fun toggleSchedule(schedule: Schedule) {
        val isActivelyRunning = com.example.util.ScheduleUtils.isScheduleActiveAt(schedule)
        val isStrictLock = sessionManager.isStrictActive() || sessionManager.isUltraStrictActive() ||
                (isActivelyRunning && (schedule.isStrictMode || schedule.isUltraStrict))

        if (schedule.isEnabled && isStrictLock) {
            // Cannot turn off schedule while running in strict or ultra strict mode
            return
        }
        viewModelScope.launch {
            val updated = schedule.copy(isEnabled = !schedule.isEnabled)
            repository.updateSchedule(updated)
            sessionManager.clearSnooze(schedule.id)
            val allSchedules = repository.getAllSchedulesOnce()
            ScheduleAlarmManager.rescheduleAll(application, allSchedules)
            sessionManager.checkAutomaticSchedules(repository)
        }
    }

    fun saveOrUpdateSchedule(
        existingSchedule: Schedule?,
        name: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeek: String,
        isStrictMode: Boolean,
        isUltraStrict: Boolean,
        activeListNames: String
    ) {
        val isActivelyRunning = existingSchedule?.let { com.example.util.ScheduleUtils.isScheduleActiveAt(it) } ?: false
        val isStrictLock = sessionManager.isStrictActive() || sessionManager.isUltraStrictActive() ||
                (isActivelyRunning && existingSchedule?.isEnabled == true && (existingSchedule.isStrictMode || existingSchedule.isUltraStrict))

        if (isStrictLock) {
            // Locked during active strict session
            closeCreateScheduleDialog()
            return
        }
        viewModelScope.launch {
            if (existingSchedule != null) {
                val updated = existingSchedule.copy(
                    name = name.trim(),
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek,
                    isStrictMode = if (isUltraStrict) false else isStrictMode,
                    isUltraStrict = isUltraStrict,
                    activeListNames = activeListNames
                )
                repository.updateSchedule(updated)
            } else {
                repository.insertSchedule(
                    Schedule(
                        name = name.trim(),
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        daysOfWeek = daysOfWeek,
                        isStrictMode = if (isUltraStrict) false else isStrictMode,
                        isUltraStrict = isUltraStrict,
                        activeListNames = activeListNames,
                        isEnabled = true
                    )
                )
            }
            val allSchedules = repository.getAllSchedulesOnce()
            ScheduleAlarmManager.rescheduleAll(application, allSchedules)
            sessionManager.checkAutomaticSchedules(repository)
        }
        closeCreateScheduleDialog()
    }

    fun deleteSchedule(schedule: Schedule) {
        val isActivelyRunning = com.example.util.ScheduleUtils.isScheduleActiveAt(schedule)
        val isStrictLock = sessionManager.isStrictActive() || sessionManager.isUltraStrictActive() ||
                (isActivelyRunning && schedule.isEnabled && (schedule.isStrictMode || schedule.isUltraStrict))

        if (isStrictLock) {
            // Cannot delete active strict schedule
            return
        }
        viewModelScope.launch {
            ScheduleAlarmManager.cancel(application, schedule)
            repository.deleteSchedule(schedule)
            val allSchedules = repository.getAllSchedulesOnce()
            ScheduleAlarmManager.rescheduleAll(application, allSchedules)
            sessionManager.checkAutomaticSchedules(repository)
        }
    }
}

class MainViewModelFactory(
    private val repository: AppRepository,
    private val sessionManager: FocusSessionManager,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, sessionManager, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
