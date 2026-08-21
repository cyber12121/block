package com.example.data

import com.example.data.dao.BlockListDao
import com.example.data.dao.BlockedTargetDao
import com.example.data.dao.DailyStatDao
import com.example.data.dao.FocusSessionDao
import com.example.data.dao.GardenPlantDao
import com.example.data.dao.ScheduleDao
import com.example.data.model.BlockList
import com.example.data.model.BlockedTarget
import com.example.data.model.DailyStat
import com.example.data.model.FocusSession
import com.example.data.model.GardenPlant
import com.example.data.model.PlantStatus
import com.example.data.model.PlantType
import com.example.data.model.Schedule
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRepository(
    private val blockListDao: BlockListDao,
    private val blockedTargetDao: BlockedTargetDao,
    private val focusSessionDao: FocusSessionDao,
    private val dailyStatDao: DailyStatDao,
    private val scheduleDao: ScheduleDao,
    private val gardenPlantDao: GardenPlantDao
) {
    // Block Lists
    val allBlockLists: Flow<List<BlockList>> = blockListDao.getAllLists()
    suspend fun getListById(id: Long) = blockListDao.getListById(id)
    suspend fun getAllListsOnce(): List<BlockList> = blockListDao.getAllListsOnce()
    suspend fun getActiveLists(): List<BlockList> = blockListDao.getActiveLists()
    suspend fun insertBlockList(list: BlockList) = blockListDao.insertList(list)
    suspend fun updateBlockList(list: BlockList) = blockListDao.updateList(list)
    suspend fun deleteBlockList(list: BlockList) = blockListDao.deleteList(list)

    /**
     * Seeds built-in lists/targets/schedules (and the welcome plant) if the
     * database is empty, and backfills the Adult & NSFW list when an older
     * install is missing it. Safe to call from both the Room onCreate
     * callback and Application.onCreate — see [DefaultData.seedInto].
     */
    suspend fun ensureDefaultData() = DefaultData.seedInto(
        listDao = blockListDao,
        targetDao = blockedTargetDao,
        scheduleDao = scheduleDao,
        gardenDao = gardenPlantDao
    )

    // Targets
    fun getTargetsForList(listId: Long): Flow<List<BlockedTarget>> = blockedTargetDao.getTargetsForList(listId)
    val allTargetsFlow: Flow<List<BlockedTarget>> = blockedTargetDao.getAllTargetsFlow()
    suspend fun getAllEnabledTargets(): List<BlockedTarget> = blockedTargetDao.getAllEnabledTargets()
    suspend fun insertTarget(target: BlockedTarget) = blockedTargetDao.insertTarget(target)
    suspend fun insertTargets(targets: List<BlockedTarget>) = blockedTargetDao.insertTargets(targets)
    suspend fun updateTarget(target: BlockedTarget) = blockedTargetDao.updateTarget(target)
    suspend fun deleteTarget(target: BlockedTarget) = blockedTargetDao.deleteTarget(target)
    suspend fun deleteAllTargetsForList(listId: Long) = blockedTargetDao.deleteAllTargetsForList(listId)

    // Sessions
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()
    val activeSessionFlow: Flow<FocusSession?> = focusSessionDao.getActiveSessionFlow()
    suspend fun getActiveSession(): FocusSession? = focusSessionDao.getActiveSession()
    suspend fun insertSession(session: FocusSession): Long = focusSessionDao.insertSession(session)
    suspend fun updateSession(session: FocusSession) = focusSessionDao.updateSession(session)

    // Schedules
    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    suspend fun getAllSchedulesOnce(): List<Schedule> = scheduleDao.getAllSchedulesOnce()
    suspend fun getEnabledSchedules(): List<Schedule> = scheduleDao.getEnabledSchedules()
    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insertSchedule(schedule)
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    suspend fun disableAllSchedules() = scheduleDao.disableAllSchedules()
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)

    // Garden Plants
    val allGardenPlants: Flow<List<GardenPlant>> = gardenPlantDao.getAllPlants()
    val bloomedGardenPlants: Flow<List<GardenPlant>> = gardenPlantDao.getBloomedPlants()
    val bloomedPlantsCount: Flow<Int> = gardenPlantDao.getBloomedCount()
    val witheredPlantsCount: Flow<Int> = gardenPlantDao.getWitheredCount()
    suspend fun getCurrentGrowingPlant(): GardenPlant? = gardenPlantDao.getCurrentGrowingPlant()
    suspend fun plantSeed(plantType: PlantType, durationMinutes: Int, sessionTitle: String): Long {
        return gardenPlantDao.insertPlant(
            GardenPlant(
                plantType = plantType,
                sessionMinutes = durationMinutes,
                plantedAtMillis = System.currentTimeMillis(),
                status = PlantStatus.GROWING,
                associatedSessionTitle = sessionTitle
            )
        )
    }
    suspend fun markPlantBloomed(plant: GardenPlant) {
        gardenPlantDao.updatePlant(
            plant.copy(
                status = PlantStatus.BLOOMED,
                completedAtMillis = System.currentTimeMillis()
            )
        )
    }
    suspend fun markPlantWithered(plant: GardenPlant) {
        gardenPlantDao.updatePlant(
            plant.copy(
                status = PlantStatus.WITHERED,
                completedAtMillis = System.currentTimeMillis()
            )
        )
    }

    // Stats
    val totalMinutes: Flow<Int?> = focusSessionDao.getTotalCompletedMinutes()
    val completedSessionsCount: Flow<Int> = focusSessionDao.getCompletedSessionsCount()
    val totalBlockedAttempts: Flow<Int?> = focusSessionDao.getTotalBlockedAttempts()
    val recentDailyStats: Flow<List<DailyStat>> = dailyStatDao.getRecentStats()

    suspend fun recordBlockedAttempt() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Atomic: INSERT OR IGNORE ensures row exists, then increments — no race condition
        dailyStatDao.atomicIncrementBlockedCount(today)
    }

    suspend fun recordCompletedSession(minutes: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Atomic: INSERT OR IGNORE ensures row exists, then updates both columns
        dailyStatDao.atomicRecordCompletedSession(today, minutes)
    }

    suspend fun resetDatabaseToDefaults() {
        // Clear all user created tables
        blockedTargetDao.clearAll()
        blockListDao.clearAll()
        scheduleDao.clearAll()
        focusSessionDao.clearAll()
        dailyStatDao.clearAll()
        gardenPlantDao.clearGarden()

        // Re-seed original default lists, targets, schedules, and garden
        DefaultData.seedInto(
            listDao = blockListDao,
            targetDao = blockedTargetDao,
            scheduleDao = scheduleDao,
            gardenDao = gardenPlantDao
        )
    }
}
