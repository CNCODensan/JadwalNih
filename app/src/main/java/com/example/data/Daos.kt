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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduleTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ScheduleTaskEntity>)

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
