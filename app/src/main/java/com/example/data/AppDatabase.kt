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
        NotificationLogEntity::class,
        WorshipCheckinEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleTaskDao(): ScheduleTaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun photoRecapDao(): PhotoRecapDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun worshipCheckinDao(): WorshipCheckinDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN iconKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN locationLat REAL")
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN locationLng REAL")
                db.execSQL("ALTER TABLE schedule_tasks ADD COLUMN skippedOccurrenceDates TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE photo_recaps ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE photo_recaps ADD COLUMN locationLat REAL")
                db.execSQL("ALTER TABLE photo_recaps ADD COLUMN locationLng REAL")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        relatedTaskId INTEGER,
                        draftJson TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS worship_checkins (
                        dateItemKey TEXT NOT NULL PRIMARY KEY,
                        dateIso TEXT NOT NULL,
                        itemKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        religionName TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAtEpochMs INTEGER
                    )
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jadwalnih_db",
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
