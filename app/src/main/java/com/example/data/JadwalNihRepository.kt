package com.example.data

import android.net.Uri
import com.example.domain.AttendanceStatus
import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.PhotoRecap
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
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

    suspend fun upsertTask(task: ScheduleTask): Long {
        val computedDuration = (task.endMinuteOfDay - task.startMinuteOfDay).coerceAtLeast(1)
        return taskDao.insertTask(task.copy(estimatedMinutes = computedDuration).toEntity())
    }

    suspend fun toggleTaskComplete(task: ScheduleTask): ScheduleTask? {
        val now = System.currentTimeMillis()
        val nowCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = nowCompleted,
            completedAtEpochMs = if (nowCompleted) now else null,
            attendanceStatus = if (nowCompleted && task.attendanceStatus == AttendanceStatus.UNCONFIRMED) {
                AttendanceStatus.CONFIRMED_ATTENDING
            } else if (!nowCompleted && task.attendanceStatus == AttendanceStatus.CONFIRMED_OVERRIDE) {
                AttendanceStatus.UNCONFIRMED
            } else {
                task.attendanceStatus
            },
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
                    attendanceStatus = AttendanceStatus.UNCONFIRMED,
                )
                val newId = taskDao.insertTask(nextTask.toEntity())
                return nextTask.copy(id = newId)
            }
        }
        return null
    }

    suspend fun updateAttendance(task: ScheduleTask, status: AttendanceStatus, markCompleted: Boolean = false) {
        val now = System.currentTimeMillis()
        val updated = task.copy(
            attendanceStatus = status,
            isCompleted = if (markCompleted) true else task.isCompleted,
            completedAtEpochMs = if (markCompleted) (task.completedAtEpochMs ?: now) else task.completedAtEpochMs,
        )
        taskDao.updateTask(updated.toEntity())
    }

    suspend fun toggleSubTask(task: ScheduleTask, subTaskId: String): Boolean {
        val updatedSubs = task.subtasks.map { sub ->
            if (sub.id == subTaskId) sub.copy(isDone = !sub.isDone) else sub
        }
        val wasAllDone = task.subtasks.isNotEmpty() && task.subtasks.all { it.isDone }
        val isNowAllDone = updatedSubs.isNotEmpty() && updatedSubs.all { it.isDone }
        taskDao.updateTask(task.copy(subtasks = updatedSubs).toEntity())
        return !wasAllDone && isNowAllDone
    }

    suspend fun moveTaskToTomorrow(task: ScheduleTask): ScheduleTask {
        val tomorrowIso = DateTimeHelper.addDays(DateTimeHelper.todayIso(), 1)
        val targetDate = if (task.dateIso >= DateTimeHelper.todayIso()) {
            DateTimeHelper.addDays(task.dateIso, 1)
        } else {
            tomorrowIso
        }
        val moved = task.copy(
            dateIso = targetDate,
            isCompleted = false,
            completedAtEpochMs = null,
            attendanceStatus = AttendanceStatus.UNCONFIRMED,
        )
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
        var firstError: Throwable? = null
        for (uri in uris.take(remainingSlots)) {
            val savedResult = imageHelper.saveCompressedFromUri(uri)
            if (savedResult.isSuccess) {
                currentPaths.add(savedResult.getOrThrow())
                addedCount++
            } else if (firstError == null) {
                firstError = savedResult.exceptionOrNull()
            }
        }
        if (addedCount == 0) {
            return Result.failure(
                firstError ?: IllegalStateException("Gagal memproses gambar yang dipilih."),
            )
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

    suspend fun addStyledPhotoToRecap(
        dateIso: String,
        uri: Uri,
        textStyle: PhotoTextStyle,
        textAlignment: PhotoTextAlignment,
        overlayTimeLabel: String,
        title: String,
        caption: String,
    ): Result<Unit> {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain()
        val currentPaths = existing?.imagePaths?.toMutableList() ?: mutableListOf()
        if (currentPaths.size >= 4) {
            return Result.failure(IllegalStateException("Maksimal 4 foto per tanggal sudah tercapai."))
        }

        val cleanedTitle = title.trim()
        val cleanedCaption = caption.trim()
        val savedPath = imageHelper.saveCompressedFromUri(
            uri = uri,
            textStyle = textStyle,
            textAlignment = textAlignment,
            overlayTimeLabel = overlayTimeLabel,
            overlayTitle = cleanedTitle,
        ).getOrElse { return Result.failure(it) }

        currentPaths.add(savedPath)
        val updated = PhotoRecap(
            dateIso = dateIso,
            title = cleanedTitle.ifBlank { existing?.title ?: "" },
            caption = cleanedCaption.ifBlank { existing?.caption ?: "" },
            imagePaths = currentPaths,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        recapDao.upsertRecap(updated.toEntity())
        return Result.success(Unit)
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

    suspend fun deleteRecap(dateIso: String) {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain()
        existing?.imagePaths?.forEach { path ->
            imageHelper.deleteFileIfExists(path)
        }
        recapDao.deleteRecapByDate(dateIso)
    }

    suspend fun exportRecapCollage(dateIso: String, recap: PhotoRecap): Result<ImageStorageHelper.CollageExportResult> {
        return imageHelper.exportCollageToGalleryAndShareUri(
            dateIso = dateIso,
            title = recap.title,
            imagePaths = recap.imagePaths,
        )
    }

    suspend fun exportJson(uri: Uri): Result<String> = backupManager.exportToUri(uri)

    suspend fun importJson(uri: Uri): Result<String> = backupManager.importFromUri(uri)

    suspend fun resetAllData() {
        taskDao.clearAllTasks()
        focusDao.clearAllSessions()
        recapDao.clearAllRecaps()
        imageHelper.clearAllRecapImages()
    }
}
