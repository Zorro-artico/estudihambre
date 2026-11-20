package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ContainerDao
import com.example.data.local.dao.DailyLogDao
import com.example.data.local.dao.MealDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WeightDao
import com.example.data.local.entity.ContainerEntity
import com.example.data.local.entity.DailyLogEntity
import com.example.data.local.entity.MealEntryEntity
import com.example.data.local.entity.RoomConverters
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeightEntryEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ContainerEntity::class,
        DailyLogEntity::class,
        MealEntryEntity::class,
        WeightEntryEntity::class,
        SyncQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun mealDao(): MealDao
    abstract fun containerDao(): ContainerDao
    abstract fun weightDao(): WeightDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN dailyRoutineNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN routineAdaptationSummary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN routineBonusKcal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isSoundEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN biweeklyBudget REAL NOT NULL DEFAULT 1200.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN examSurvivalMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN waterGlasses INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN waterTargetGlasses INTEGER NOT NULL DEFAULT 8")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "estudihambre_life.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
