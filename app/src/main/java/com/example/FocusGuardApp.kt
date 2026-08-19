package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppRepository
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
                // Must use getAllListsOnce() (all rows), not getActiveLists()
                // (enabled only). See AppRepository.ensureDefaultData().
                repository.ensureDefaultData()
            } catch (_: Exception) {}
            sessionManager.refreshBlockedTargetsCache(repository)
        }
    }
}
