package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduleTaskEntity::class,
        FocusSessionEntity::class,
        PhotoRecapEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleTaskDao(): ScheduleTaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun photoRecapDao(): PhotoRecapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN subtasksJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM schedule_tasks")
                db.execSQL("DELETE FROM focus_sessions")
                db.execSQL("DELETE FROM photo_recaps")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN attendanceStatusName TEXT NOT NULL DEFAULT 'UNCONFIRMED'")
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN isSubtasksTodoMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jadwalnih_db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
