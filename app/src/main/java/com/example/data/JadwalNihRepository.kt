package com.example.data

import android.net.Uri
import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.PhotoRecap
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JadwalNihRepository(
    private val taskDao: ScheduleTaskDao,
    private val focusDao: FocusSessionDao,
    private val recapDao: PhotoRecapDao,
    private val prefsManager: PreferencesManager,
    private val imageHelper: ImageStorageHelper,
    private val backupManager: JsonBackupManager,
) {
    val tasksFlow: Flow<List<ScheduleTask>> = taskDao.observeAllTasks().map { list ->
        list.map { it.toDomain() }
    }

    val focusSessionsFlow: Flow<List<FocusSession>> = focusDao.observeAllSessions().map { list ->
        list.map { it.toDomain() }
    }

    val photoRecapsFlow: Flow<List<PhotoRecap>> = recapDao.observeAllRecaps().map { list ->
        list.map { it.toDomain() }
    }

    val preferencesFlow: Flow<UserPreferences> = prefsManager.preferencesFlow

    suspend fun setDarkMode(enabled: Boolean) = prefsManager.setDarkMode(enabled)

    suspend fun setLastFocusMinutes(minutes: Int) = prefsManager.setLastFocusMinutes(minutes)

    suspend fun upsertTask(task: ScheduleTask): Long {
        return taskDao.insertTask(task.toEntity())
    }

    suspend fun toggleTaskComplete(task: ScheduleTask): ScheduleTask? {
        val now = System.currentTimeMillis()
        val nowCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = nowCompleted,
            completedAtEpochMs = if (nowCompleted) now else null,
        )
        taskDao.updateTask(updated.toEntity())

        if (nowCompleted && task.recurrence != RecurrenceRule.NONE) {
            val nextDate = StatusEvaluator.nextOccurrenceDate(task.dateIso, task.recurrence)
            if (nextDate != null) {
                val nextTask = task.copy(
                    id = 0L,
                    dateIso = nextDate,
                    isCompleted = false,
                    completedAtEpochMs = null,
                    createdAtEpochMs = now,
                )
                val newId = taskDao.insertTask(nextTask.toEntity())
                return nextTask.copy(id = newId)
            }
        }
        return null
    }

    suspend fun moveTaskToTomorrow(task: ScheduleTask): ScheduleTask {
        val tomorrowIso = DateTimeHelper.addDays(DateTimeHelper.todayIso(), 1)
        val targetDate = if (task.dateIso >= DateTimeHelper.todayIso()) {
            DateTimeHelper.addDays(task.dateIso, 1)
        } else {
            tomorrowIso
        }
        val moved = task.copy(dateIso = targetDate, isCompleted = false, completedAtEpochMs = null)
        taskDao.updateTask(moved.toEntity())
        return moved
    }

    suspend fun updateTaskQuadrant(task: ScheduleTask, quadrant: PriorityQuadrant) {
        taskDao.updateTask(task.copy(quadrant = quadrant).toEntity())
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun saveFocusSession(session: FocusSession) {
        focusDao.insertSession(session.toEntity())
    }

    suspend fun addPhotosToRecap(dateIso: String, uris: List<Uri>): Result<Int> {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain()
        val currentPaths = existing?.imagePaths?.toMutableList() ?: mutableListOf()
        val remainingSlots = (4 - currentPaths.size).coerceAtLeast(0)
        if (remainingSlots == 0) {
            return Result.failure(IllegalStateException("Maksimal 4 foto per tanggal sudah tercapai."))
        }

        var addedCount = 0
        for (uri in uris.take(remainingSlots)) {
            val savedResult = imageHelper.saveCompressedFromUri(uri)
            if (savedResult.isSuccess) {
                currentPaths.add(savedResult.getOrThrow())
                addedCount++
            } else {
                val err = savedResult.exceptionOrNull()
                if (addedCount == 0) {
                    return Result.failure(
                        err ?: IllegalStateException("Gagal menyimpan foto. Periksa ruang penyimpanan perangkat."),
                    )
                }
            }
        }

        val updated = PhotoRecap(
            dateIso = dateIso,
            title = existing?.title ?: "",
            caption = existing?.caption ?: "",
            imagePaths = currentPaths,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        recapDao.upsertRecap(updated.toEntity())
        return Result.success(addedCount)
    }

    suspend fun removePhotoFromRecap(dateIso: String, pathToRemove: String) {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain() ?: return
        imageHelper.deleteFileIfExists(pathToRemove)
        val remaining = existing.imagePaths.filterNot { it == pathToRemove }
        if (remaining.isEmpty() && existing.title.isBlank() && existing.caption.isBlank()) {
            recapDao.deleteRecapByDate(dateIso)
        } else {
            recapDao.upsertRecap(
                existing.copy(
                    imagePaths = remaining,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ).toEntity(),
            )
        }
    }

    suspend fun updateRecapMeta(dateIso: String, title: String, caption: String) {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain()
        val cleanedTitle = title.trim()
        val cleanedCaption = caption.trim()
        if (existing == null && cleanedTitle.isEmpty() && cleanedCaption.isEmpty()) return

        val updated = PhotoRecap(
            dateIso = dateIso,
            title = cleanedTitle,
            caption = cleanedCaption,
            imagePaths = existing?.imagePaths ?: emptyList(),
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        if (updated.imagePaths.isEmpty() && updated.title.isEmpty() && updated.caption.isEmpty()) {
            recapDao.deleteRecapByDate(dateIso)
        } else {
            recapDao.upsertRecap(updated.toEntity())
        }
    }

    suspend fun exportJson(uri: Uri): Result<String> = backupManager.exportToUri(uri)

    suspend fun importJson(uri: Uri): Result<String> = backupManager.importFromUri(uri)

    suspend fun resetAllData() {
        taskDao.clearAllTasks()
        focusDao.clearAllSessions()
        recapDao.clearAllRecaps()
        imageHelper.clearAllRecapImages()
        prefsManager.setHasSeededInitialData(true)
    }

    suspend fun seedInitialDataIfNeeded() {
        val prefs = prefsManager.getPreferencesOnce()
        if (prefs.hasSeededInitialData) return

        val today = DateTimeHelper.todayIso()
        val yesterday = DateTimeHelper.addDays(today, -1)
        val tomorrow = DateTimeHelper.addDays(today, 1)
        val plus3Days = DateTimeHelper.addDays(today, 3)
        val nowMin = DateTimeHelper.nowMinuteOfDay()

        // Create realistic Indonesian sample items spanning statuses and quadrants
        val nearStart = (nowMin + 12).coerceAtMost(22 * 60)
        val nearEnd = (nearStart + 45).coerceAtMost(23 * 60)
        val soonStart = (nowMin + 45).coerceAtMost(22 * 60 + 30)
        val soonEnd = (soonStart + 60).coerceAtMost(23 * 60 + 30)
        val morningDoneStart = 7 * 60 + 30
        val morningDoneEnd = 8 * 60 + 15
        val eveningFlexStart = (nowMin + 150).coerceAtMost(21 * 60)
        val eveningFlexEnd = (eveningFlexStart + 60).coerceAtMost(22 * 60)

        val sampleTasks = listOf(
            ScheduleTaskEntity(
                title = "Standup Harian & Sinkronisasi Tim",
                dateIso = today,
                startMinuteOfDay = morningDoneStart,
                endMinuteOfDay = morningDoneEnd,
                estimatedMinutes = 45,
                quadrantName = PriorityQuadrant.Q1_IMPORTANT_URGENT.name,
                isCompleted = true,
                isFlexible = false,
                recurrenceName = RecurrenceRule.DAILY.name,
                reminderEnabled = true,
                reminderLeadMinutes = 10,
                completedAtEpochMs = System.currentTimeMillis() - 3_600_000L,
                createdAtEpochMs = System.currentTimeMillis() - 7_200_000L,
            ),
            ScheduleTaskEntity(
                title = "Finalisasi Presentasi Klien Q4",
                dateIso = today,
                startMinuteOfDay = nearStart,
                endMinuteOfDay = nearEnd,
                estimatedMinutes = 45,
                quadrantName = PriorityQuadrant.Q1_IMPORTANT_URGENT.name,
                isCompleted = false,
                isFlexible = false,
                recurrenceName = RecurrenceRule.NONE.name,
                reminderEnabled = true,
                reminderLeadMinutes = 10,
                completedAtEpochMs = null,
                createdAtEpochMs = System.currentTimeMillis() - 5_000_000L,
            ),
            ScheduleTaskEntity(
                title = "Review Proposal Anggaran Mingguan",
                dateIso = today,
                startMinuteOfDay = soonStart,
                endMinuteOfDay = soonEnd,
                estimatedMinutes = 60,
                quadrantName = PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT.name,
                isCompleted = false,
                isFlexible = false,
                recurrenceName = RecurrenceRule.WEEKLY.name,
                reminderEnabled = false,
                reminderLeadMinutes = 15,
                completedAtEpochMs = null,
                createdAtEpochMs = System.currentTimeMillis() - 4_000_000L,
            ),
            ScheduleTaskEntity(
                title = "Belajar Materi Arsitektur Compose",
                dateIso = today,
                startMinuteOfDay = eveningFlexStart,
                endMinuteOfDay = eveningFlexEnd,
                estimatedMinutes = 60,
                quadrantName = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT.name,
                isCompleted = false,
                isFlexible = true,
                recurrenceName = RecurrenceRule.NONE.name,
                reminderEnabled = false,
                reminderLeadMinutes = 10,
                completedAtEpochMs = null,
                createdAtEpochMs = System.currentTimeMillis() - 3_000_000L,
            ),
            ScheduleTaskEntity(
                title = "Rapikan Arsip Dokumen Proyek",
                dateIso = yesterday,
                startMinuteOfDay = 16 * 60,
                endMinuteOfDay = 17 * 60,
                estimatedMinutes = 60,
                quadrantName = PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT.name,
                isCompleted = false,
                isFlexible = false,
                recurrenceName = RecurrenceRule.NONE.name,
                reminderEnabled = false,
                reminderLeadMinutes = 10,
                completedAtEpochMs = null,
                createdAtEpochMs = System.currentTimeMillis() - 86_400_000L,
            ),
            ScheduleTaskEntity(
                title = "Olahraga Lari Pagi 5K",
                dateIso = tomorrow,
                startMinuteOfDay = 6 * 60,
                endMinuteOfDay = 7 * 60,
                estimatedMinutes = 60,
                quadrantName = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT.name,
                isCompleted = false,
                isFlexible = true,
                recurrenceName = RecurrenceRule.WEEKLY.name,
                reminderEnabled = true,
                reminderLeadMinutes = 15,
                completedAtEpochMs = null,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
            ScheduleTaskEntity(
                title = "Evaluasi Tagihan & Catatan Keuangan",
                dateIso = plus3Days,
                startMinuteOfDay = 19 * 60,
                endMinuteOfDay = 20 * 60,
                estimatedMinutes = 45,
                quadrantName = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT.name,
                isCompleted = false,
                isFlexible = false,
                recurrenceName = RecurrenceRule.MONTHLY.name,
                reminderEnabled = true,
                reminderLeadMinutes = 30,
                completedAtEpochMs = null,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        taskDao.insertTasks(sampleTasks)

        val now = System.currentTimeMillis()
        val sampleSessions = listOf(
            FocusSessionEntity(
                linkedTaskId = null,
                linkedTaskTitle = "Standup Harian & Sinkronisasi Tim",
                plannedMinutes = 35,
                actualSeconds = 35 * 60,
                completedNaturally = true,
                startedAtEpochMs = now - 7_200_000L,
                endedAtEpochMs = now - 5_100_000L,
            ),
            FocusSessionEntity(
                linkedTaskId = null,
                linkedTaskTitle = "Riset Referensi Desain",
                plannedMinutes = 40,
                actualSeconds = 40 * 60,
                completedNaturally = true,
                startedAtEpochMs = now - 90_000_000L,
                endedAtEpochMs = now - 87_600_000L,
            ),
        )
        focusDao.insertSessions(sampleSessions)

        prefsManager.setHasSeededInitialData(true)
    }
}
