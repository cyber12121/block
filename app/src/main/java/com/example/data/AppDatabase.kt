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
                // onCreate can fire during the first open, which may be before
                // getDatabase() assigns INSTANCE. Fall back to Application.onCreate
                // via AppRepository.ensureDefaultData() if we lose that race —
                // seedInto is idempotent either way.
                scope.launch(Dispatchers.IO) {
                    val database = INSTANCE ?: return@launch
                    DefaultData.seedInto(
                        listDao = database.blockListDao(),
                        targetDao = database.blockedTargetDao(),
                        scheduleDao = database.scheduleDao(),
                        gardenDao = database.gardenPlantDao()
                    )
                }
            }
        }
    }
}
