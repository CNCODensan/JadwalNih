package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTaskDao {
    @Query("SELECT * FROM schedule_tasks ORDER BY dateIso ASC, startMinuteOfDay ASC")
    fun observeAllTasks(): Flow<List<ScheduleTaskEntity>>

    @Query("SELECT * FROM schedule_tasks ORDER BY dateIso ASC, startMinuteOfDay ASC")
    suspend fun getAllTasksOnce(): List<ScheduleTaskEntity>

    @Query("SELECT * FROM schedule_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskByIdOnce(taskId: Long): ScheduleTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduleTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ScheduleTaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: ScheduleTaskEntity)

    @Query("DELETE FROM schedule_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM schedule_tasks")
    suspend fun clearAllTasks()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
    suspend fun getAllSessionsOnce(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllSessions()
}

@Dao
interface PhotoRecapDao {
    @Query("SELECT * FROM photo_recaps ORDER BY dateIso DESC")
    fun observeAllRecaps(): Flow<List<PhotoRecapEntity>>

    @Query("SELECT * FROM photo_recaps ORDER BY dateIso DESC")
    suspend fun getAllRecapsOnce(): List<PhotoRecapEntity>

    @Query("SELECT * FROM photo_recaps WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getRecapByDateOnce(dateIso: String): PhotoRecapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecap(recap: PhotoRecapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecaps(recaps: List<PhotoRecapEntity>)

    @Query("DELETE FROM photo_recaps WHERE dateIso = :dateIso")
    suspend fun deleteRecapByDate(dateIso: String)

    @Query("DELETE FROM photo_recaps")
    suspend fun clearAllRecaps()
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY createdAtEpochMs DESC")
    fun observeAllLogs(): Flow<List<NotificationLogEntity>>

    @Query("SELECT * FROM notification_logs ORDER BY createdAtEpochMs DESC")
    suspend fun getAllLogsOnce(): List<NotificationLogEntity>

    @Query("SELECT * FROM notification_logs WHERE category = 'DRAFT' ORDER BY createdAtEpochMs DESC")
    suspend fun getDraftLogsOnce(): List<NotificationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<NotificationLogEntity>)

    @Query("UPDATE notification_logs SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notification_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM notification_logs WHERE createdAtEpochMs < :cutoffEpochMs AND category != 'DRAFT'")
    suspend fun deleteExpiredNonDrafts(cutoffEpochMs: Long)

    @Query("DELETE FROM notification_logs WHERE category = 'DRAFT'")
    suspend fun clearAllDraftLogs()

    @Query("DELETE FROM notification_logs")
    suspend fun clearAllLogs()
}

@Dao
interface WorshipCheckinDao {
    @Query("SELECT * FROM worship_checkins ORDER BY dateIso DESC")
    fun observeAllCheckins(): Flow<List<WorshipCheckinEntity>>

    @Query("SELECT * FROM worship_checkins ORDER BY dateIso DESC")
    suspend fun getAllCheckinsOnce(): List<WorshipCheckinEntity>

    @Query("SELECT * FROM worship_checkins WHERE dateItemKey = :key LIMIT 1")
    suspend fun getByKeyOnce(key: String): WorshipCheckinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckin(entity: WorshipCheckinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckins(entities: List<WorshipCheckinEntity>)

    @Query("DELETE FROM worship_checkins WHERE dateItemKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM worship_checkins")
    suspend fun clearAllCheckins()
}
