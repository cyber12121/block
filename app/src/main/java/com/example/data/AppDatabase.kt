package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BlockList::class,
        BlockedTarget::class,
        FocusSession::class,
        DailyStat::class,
        Schedule::class,
        GardenPlant::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockListDao(): BlockListDao
    abstract fun blockedTargetDao(): BlockedTargetDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun dailyStatDao(): DailyStatDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun gardenPlantDao(): GardenPlantDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_guard_master_db"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val listDao = database.blockListDao()
            val targetDao = database.blockedTargetDao()
            val scheduleDao = database.scheduleDao()
            val gardenDao = database.gardenPlantDao()

            for (list in DefaultData.defaultLists) {
                listDao.insertList(list)
            }
            targetDao.insertTargets(DefaultData.defaultTargets)
            for (schedule in DefaultData.defaultSchedules) {
                scheduleDao.insertSchedule(schedule)
            }

            // Seed initial starter bloomed plant in garden
            gardenDao.insertPlant(
                GardenPlant(
                    plantType = PlantType.SPROUT,
                    sessionMinutes = 25,
                    plantedAtMillis = System.currentTimeMillis() - 86400000L,
                    completedAtMillis = System.currentTimeMillis() - 85000000L,
                    status = PlantStatus.BLOOMED,
                    associatedSessionTitle = "Welcome Sprint",
                    slotIndex = 0
                )
            )
        }
    }
}
