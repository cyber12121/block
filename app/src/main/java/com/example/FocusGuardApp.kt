package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.DefaultData
import com.example.service.FocusForegroundService
import com.example.service.FocusSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusGuardApp : Application() {
    val applicationScope = CoroutineScope(Dispatchers.IO)

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this, applicationScope)
    }

    val repository: AppRepository by lazy {
        AppRepository(
            blockListDao = database.blockListDao(),
            blockedTargetDao = database.blockedTargetDao(),
            focusSessionDao = database.focusSessionDao(),
            dailyStatDao = database.dailyStatDao(),
            scheduleDao = database.scheduleDao(),
            gardenPlantDao = database.gardenPlantDao()
        )
    }

    lateinit var sessionManager: FocusSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(this)

        // Start ongoing foreground service to keep background monitoring alive
        try {
            FocusForegroundService.startService(this)
        } catch (_: Exception) {}

        applicationScope.launch {
            try {
                if (repository.getActiveLists().isEmpty()) {
                    for (list in DefaultData.defaultLists) {
                        repository.insertBlockList(list)
                    }
                    repository.insertTargets(DefaultData.defaultTargets)
                    for (sched in DefaultData.defaultSchedules) {
                        repository.insertSchedule(sched)
                    }
                } else {
                    // Ensure list 6 (Adult & NSFW blocker) exists even if user already has other lists
                    val existing = repository.getActiveLists()
                    if (existing.none { it.id == 6L || it.name.contains("Adult", ignoreCase = true) }) {
                        val adultList = DefaultData.defaultLists.first { it.id == 6L }
                        repository.insertBlockList(adultList)
                        val adultTargets = DefaultData.defaultTargets.filter { it.listId == 6L }
                        repository.insertTargets(adultTargets)
                    }
                }
            } catch (_: Exception) {}
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }
}
