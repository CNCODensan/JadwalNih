package com.example.data

import android.net.Uri
import com.example.domain.AttendanceStatus
import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.NotificationCategory
import com.example.domain.NotificationLogEntry
import com.example.domain.PhotoRecap
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ReligionOption
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.TaskDraft
import com.example.domain.WorshipScheduleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class JadwalNihRepository(
    private val taskDao: ScheduleTaskDao,
    private val focusDao: FocusSessionDao,
    private val recapDao: PhotoRecapDao,
    private val imageHelper: ImageStorageHelper,
    private val backupManager: JsonBackupManager,
    private val notificationLogDao: NotificationLogDao? = null,
    private val worshipCheckinDao: WorshipCheckinDao? = null,
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

    val notificationLogsFlow: Flow<List<NotificationLogEntry>> =
        notificationLogDao?.observeAllLogs()?.map { list -> list.map { it.toDomain() } }
            ?: flowOf(emptyList())

    val worshipCheckinKeysFlow: Flow<Set<String>> =
        worshipCheckinDao?.observeAllCheckins()?.map { list ->
            list.filter { it.isCompleted }.map { it.dateItemKey }.toSet()
        } ?: flowOf(emptySet())

    suspend fun getAllTasksOnce(): List<ScheduleTask> = taskDao.getAllTasksOnce().map { it.toDomain() }

    suspend fun upsertTask(task: ScheduleTask): Long {
        val computedDuration = (task.endMinuteOfDay - task.startMinuteOfDay).coerceAtLeast(1)
        return taskDao.insertTask(task.copy(estimatedMinutes = computedDuration).toEntity())
    }

    suspend fun insertTasksBatch(tasks: List<ScheduleTask>): List<ScheduleTask> {
        if (tasks.isEmpty()) return emptyList()
        val entities = tasks.map { task ->
            val computedDuration = (task.endMinuteOfDay - task.startMinuteOfDay).coerceAtLeast(1)
            task.copy(estimatedMinutes = computedDuration).toEntity()
        }
        val ids = taskDao.insertTasks(entities)
        return tasks.mapIndexed { idx, task ->
            task.copy(id = ids.getOrElse(idx) { 0L })
        }
    }

    suspend fun restoreTasks(tasks: List<ScheduleTask>) {
        if (tasks.isEmpty()) return
        tasks.forEach { task ->
            val computedDuration = (task.endMinuteOfDay - task.startMinuteOfDay).coerceAtLeast(1)
            taskDao.insertTask(task.copy(estimatedMinutes = computedDuration).toEntity())
        }
    }

    suspend fun rescheduleTasksToDate(tasks: List<ScheduleTask>, targetDateIso: String): List<ScheduleTask> {
        if (tasks.isEmpty()) return emptyList()
        val updated = mutableListOf<ScheduleTask>()
        tasks.forEach { task ->
            val moved = task.copy(
                dateIso = targetDateIso,
                isCompleted = false,
                completedAtEpochMs = null,
                attendanceStatus = AttendanceStatus.UNCONFIRMED,
            )
            taskDao.updateTask(moved.toEntity())
            updated.add(moved)
        }
        return updated
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
            val nextDate = findNextNonSkippedDate(task.dateIso, task.recurrence, task.skippedOccurrenceDates)
            if (nextDate != null) {
                val all = taskDao.getAllTasksOnce().map { it.toDomain() }
                val alreadyExists = all.any {
                    it.dateIso == nextDate &&
                        it.title.trim().equals(task.title.trim(), ignoreCase = true) &&
                        it.startMinuteOfDay == task.startMinuteOfDay
                }
                if (!alreadyExists) {
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
        }
        return null
    }

    /**
     * Automatically creates due recurring task instances when their week/day arrives
     * up to [horizonEndIso] (typically today or current week end), without duplicating.
     */
    suspend fun materializeDueRecurrences(horizonEndIso: String): List<ScheduleTask> {
        val all = taskDao.getAllTasksOnce().map { it.toDomain() }
        val recurringParents = all
            .filter { it.recurrence != RecurrenceRule.NONE }
            .groupBy { "${it.title.trim().lowercase()}|${it.startMinuteOfDay}|${it.recurrence.name}" }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.dateIso } }

        if (recurringParents.isEmpty()) return emptyList()

        val existingSignatures = all.map {
            "${it.dateIso}|${it.title.trim().lowercase()}|${it.startMinuteOfDay}"
        }.toMutableSet()
        val allSkippedByGroup = all.groupBy {
            "${it.title.trim().lowercase()}|${it.startMinuteOfDay}|${it.recurrence.name}"
        }.mapValues { (_, list) -> list.flatMap { it.skippedOccurrenceDates }.toSet() }

        val created = mutableListOf<ScheduleTask>()
        val now = System.currentTimeMillis()

        recurringParents.forEach { parent ->
            val groupKey = "${parent.title.trim().lowercase()}|${parent.startMinuteOfDay}|${parent.recurrence.name}"
            val skippedSet = allSkippedByGroup[groupKey].orEmpty()
            var cursor = StatusEvaluator.nextOccurrenceDate(parent.dateIso, parent.recurrence)
            var guard = 0
            while (cursor != null && cursor <= horizonEndIso && guard < 12) {
                guard++
                val sig = "$cursor|${parent.title.trim().lowercase()}|${parent.startMinuteOfDay}"
                if (cursor !in skippedSet && sig !in existingSignatures) {
                    val nextTask = parent.copy(
                        id = 0L,
                        dateIso = cursor,
                        isCompleted = false,
                        completedAtEpochMs = null,
                        createdAtEpochMs = now + created.size,
                        attendanceStatus = AttendanceStatus.UNCONFIRMED,
                        subtasks = parent.subtasks.map { it.copy(isDone = false) },
                    )
                    val newId = taskDao.insertTask(nextTask.toEntity())
                    created.add(nextTask.copy(id = newId))
                    existingSignatures.add(sig)
                }
                cursor = StatusEvaluator.nextOccurrenceDate(cursor, parent.recurrence)
            }
        }
        return created
    }

    suspend fun materializeForeshadowEarly(parentTask: ScheduleTask, targetDateIso: String): ScheduleTask {
        val all = taskDao.getAllTasksOnce().map { it.toDomain() }
        val existing = all.firstOrNull {
            it.dateIso == targetDateIso &&
                it.title.trim().equals(parentTask.title.trim(), ignoreCase = true) &&
                it.startMinuteOfDay == parentTask.startMinuteOfDay
        }
        if (existing != null) return existing

        val newTask = parentTask.copy(
            id = 0L,
            dateIso = targetDateIso,
            isCompleted = false,
            completedAtEpochMs = null,
            createdAtEpochMs = System.currentTimeMillis(),
            attendanceStatus = AttendanceStatus.UNCONFIRMED,
            subtasks = parentTask.subtasks.map { it.copy(isDone = false) },
        )
        val newId = taskDao.insertTask(newTask.toEntity())
        return newTask.copy(id = newId)
    }

    suspend fun skipRecurrenceDate(parentTask: ScheduleTask, targetDateIso: String) {
        val all = taskDao.getAllTasksOnce().map { it.toDomain() }
        val matchingGroup = all.filter {
            it.recurrence == parentTask.recurrence &&
                it.title.trim().equals(parentTask.title.trim(), ignoreCase = true) &&
                it.startMinuteOfDay == parentTask.startMinuteOfDay
        }
        matchingGroup.forEach { t ->
            val updatedSkipped = t.skippedOccurrenceDates + targetDateIso
            taskDao.updateTask(t.copy(skippedOccurrenceDates = updatedSkipped).toEntity())
        }
    }

    suspend fun deleteRecurringSeries(task: ScheduleTask): List<ScheduleTask> {
        val all = taskDao.getAllTasksOnce().map { it.toDomain() }
        val series = all.filter {
            it.recurrence == task.recurrence &&
                it.title.trim().equals(task.title.trim(), ignoreCase = true) &&
                it.startMinuteOfDay == task.startMinuteOfDay
        }
        val toDelete = if (series.isEmpty()) listOf(task) else series
        toDelete.forEach { taskDao.deleteTaskById(it.id) }
        return toDelete
    }

    private fun findNextNonSkippedDate(
        startIso: String,
        rule: RecurrenceRule,
        skippedDates: Set<String>,
    ): String? {
        var cursor = StatusEvaluator.nextOccurrenceDate(startIso, rule)
        var guard = 0
        while (cursor != null && cursor in skippedDates && guard < 12) {
            cursor = StatusEvaluator.nextOccurrenceDate(cursor, rule)
            guard++
        }
        return cursor
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

    suspend fun saveDraftToNotificationCenter(draft: TaskDraft) {
        val dao = notificationLogDao ?: return
        if (!draft.hasMeaningfulContent()) return

        val titlePreview = when {
            draft.title.isNotBlank() -> draft.title.trim()
            draft.aiRawText.isNotBlank() -> "Impor AI yang belum disimpan"
            else -> "Draf Jadwal Baru"
        }
        val timeRange = DateTimeHelper.formatTimeRange(draft.startMinuteOfDay, draft.endMinuteOfDay)
        val draftJson = UserPreferencesStore.serializeDraft(draft)

        dao.insertLog(
            NotificationLogEntity(
                id = 0L,
                category = NotificationCategory.DRAFT.name,
                title = "Draf: $titlePreview",
                message = "${DateTimeHelper.formatShortDate(draft.dateIso)} • $timeRange • Ketuk untuk lanjut edit",
                createdAtEpochMs = System.currentTimeMillis(),
                isRead = false,
                draftJson = draftJson,
            ),
        )

        // Keep max 5 most recent draft entries
        val allDrafts = dao.getDraftLogsOnce()
        if (allDrafts.size > 5) {
            allDrafts.drop(5).forEach { old -> dao.deleteLogById(old.id) }
        }
    }

    suspend fun logNotification(
        category: NotificationCategory,
        title: String,
        message: String,
        relatedTaskId: Long? = null,
        isRead: Boolean = false,
    ) {
        val dao = notificationLogDao ?: return
        // Prevent duplicate holiday/worship entry on same day with same title
        val existing = dao.getAllLogsOnce()
        if (category == NotificationCategory.HOLIDAY || category == NotificationCategory.WORSHIP) {
            val cutoffToday = System.currentTimeMillis() - 12 * 3600_000L
            if (existing.any { it.category == category.name && it.title == title && it.createdAtEpochMs >= cutoffToday }) {
                return
            }
        }
        dao.insertLog(
            NotificationLogEntity(
                id = 0L,
                category = category.name,
                title = title,
                message = message,
                createdAtEpochMs = System.currentTimeMillis(),
                isRead = isRead,
                relatedTaskId = relatedTaskId,
            ),
        )
        pruneExpiredNotifications()
    }

    suspend fun pruneExpiredNotifications() {
        val cutoff = System.currentTimeMillis() - 30L * 24L * 3600_000L
        notificationLogDao?.deleteExpiredNonDrafts(cutoff)
    }

    suspend fun markAllNotificationsRead() {
        notificationLogDao?.markAllRead()
    }

    suspend fun deleteNotificationLog(id: Long) {
        notificationLogDao?.deleteLogById(id)
    }

    suspend fun restoreNotificationLogs(entries: List<NotificationLogEntry>) {
        val dao = notificationLogDao ?: return
        entries.forEach { dao.insertLog(it.toEntity()) }
    }

    suspend fun clearAllNotifications() {
        notificationLogDao?.clearAllLogs()
    }

    suspend fun toggleWorshipCheckin(item: WorshipScheduleItem, religion: ReligionOption): Boolean {
        val dao = worshipCheckinDao ?: return false
        val composite = "${item.dateIso}|${item.key}"
        val existing = dao.getByKeyOnce(composite)
        val newDone = !(existing?.isCompleted ?: item.isCompleted)
        dao.upsertCheckin(
            WorshipCheckinEntity(
                dateItemKey = composite,
                dateIso = item.dateIso,
                itemKey = item.key,
                title = item.title,
                religionName = religion.name,
                isCompleted = newDone,
                completedAtEpochMs = if (newDone) System.currentTimeMillis() else null,
            ),
        )
        return newDone
    }

    suspend fun addPhotosToRecap(
        dateIso: String,
        uris: List<Uri>,
        locationName: String = "",
        locationLat: Double? = null,
        locationLng: Double? = null,
    ): Result<Int> {
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
            locationName = locationName.ifBlank { existing?.locationName ?: "" },
            locationLat = locationLat ?: existing?.locationLat,
            locationLng = locationLng ?: existing?.locationLng,
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
        locationName: String = "",
        locationLat: Double? = null,
        locationLng: Double? = null,
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
            locationName = locationName.ifBlank { existing?.locationName ?: "" },
            locationLat = locationLat ?: existing?.locationLat,
            locationLng = locationLng ?: existing?.locationLng,
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

    suspend fun updateRecapMeta(
        dateIso: String,
        title: String,
        caption: String,
        locationName: String? = null,
    ) {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain()
        val cleanedTitle = title.trim()
        val cleanedCaption = caption.trim()
        val resolvedLocation = locationName?.trim() ?: (existing?.locationName ?: "")
        if (existing == null && cleanedTitle.isEmpty() && cleanedCaption.isEmpty() && resolvedLocation.isEmpty()) return

        val updated = PhotoRecap(
            dateIso = dateIso,
            title = cleanedTitle,
            caption = cleanedCaption,
            imagePaths = existing?.imagePaths ?: emptyList(),
            updatedAtEpochMs = System.currentTimeMillis(),
            locationName = resolvedLocation,
            locationLat = existing?.locationLat,
            locationLng = existing?.locationLng,
        )
        if (updated.imagePaths.isEmpty() && updated.title.isEmpty() && updated.caption.isEmpty()) {
            recapDao.deleteRecapByDate(dateIso)
        } else {
            recapDao.upsertRecap(updated.toEntity())
        }
    }

    suspend fun deleteRecap(dateIso: String, deleteImageFiles: Boolean = true) {
        val existing = recapDao.getRecapByDateOnce(dateIso)?.toDomain()
        if (deleteImageFiles) {
            existing?.imagePaths?.forEach { path ->
                imageHelper.deleteFileIfExists(path)
            }
        }
        recapDao.deleteRecapByDate(dateIso)
    }

    suspend fun restoreRecap(recap: PhotoRecap) {
        recapDao.upsertRecap(recap.toEntity())
    }

    suspend fun exportRecapCollage(dateIso: String, recap: PhotoRecap): Result<ImageStorageHelper.CollageExportResult> {
        return imageHelper.exportCollageToGalleryAndShareUri(
            dateIso = dateIso,
            title = recap.title,
            imagePaths = recap.imagePaths,
        )
    }

    suspend fun exportJson(uri: Uri): Result<String> = backupManager.exportToUri(uri)

    suspend fun inspectBackup(uri: Uri): Result<JsonBackupManager.BackupPreviewInfo> = backupManager.inspectBackup(uri)

    suspend fun executeImport(
        uri: Uri,
        strategy: JsonBackupManager.ImportStrategy,
    ): Result<JsonBackupManager.ImportResult> = backupManager.executeImport(uri, strategy)

    suspend fun importJson(uri: Uri): Result<String> = backupManager.importFromUri(uri)

    suspend fun resetAllData() {
        taskDao.clearAllTasks()
        focusDao.clearAllSessions()
        recapDao.clearAllRecaps()
        notificationLogDao?.clearAllLogs()
        worshipCheckinDao?.clearAllCheckins()
        imageHelper.clearAllRecapImages()
    }
}
