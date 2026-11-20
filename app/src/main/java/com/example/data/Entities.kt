package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.FocusSession
import com.example.domain.PhotoRecap
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask
import org.json.JSONArray

@Entity(tableName = "schedule_tasks")
data class ScheduleTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val dateIso: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val estimatedMinutes: Int,
    val quadrantName: String,
    val isCompleted: Boolean,
    val isFlexible: Boolean,
    val recurrenceName: String,
    val reminderEnabled: Boolean,
    val reminderLeadMinutes: Int,
    val completedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val linkedTaskId: Long?,
    val linkedTaskTitle: String?,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val completedNaturally: Boolean,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
)

@Entity(tableName = "photo_recaps")
data class PhotoRecapEntity(
    @PrimaryKey val dateIso: String,
    val title: String,
    val caption: String,
    val imagePathsJson: String,
    val updatedAtEpochMs: Long,
)

fun ScheduleTaskEntity.toDomain(): ScheduleTask = ScheduleTask(
    id = id,
    title = title,
    dateIso = dateIso,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    estimatedMinutes = estimatedMinutes,
    quadrant = runCatching { PriorityQuadrant.valueOf(quadrantName) }
        .getOrDefault(PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT),
    isCompleted = isCompleted,
    isFlexible = isFlexible,
    recurrence = runCatching { RecurrenceRule.valueOf(recurrenceName) }
        .getOrDefault(RecurrenceRule.NONE),
    reminderEnabled = reminderEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
)

fun ScheduleTask.toEntity(): ScheduleTaskEntity = ScheduleTaskEntity(
    id = id,
    title = title,
    dateIso = dateIso,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    estimatedMinutes = estimatedMinutes,
    quadrantName = quadrant.name,
    isCompleted = isCompleted,
    isFlexible = isFlexible,
    recurrenceName = recurrence.name,
    reminderEnabled = reminderEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
)

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    linkedTaskId = linkedTaskId,
    linkedTaskTitle = linkedTaskTitle,
    plannedMinutes = plannedMinutes,
    actualSeconds = actualSeconds,
    completedNaturally = completedNaturally,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    linkedTaskId = linkedTaskId,
    linkedTaskTitle = linkedTaskTitle,
    plannedMinutes = plannedMinutes,
    actualSeconds = actualSeconds,
    completedNaturally = completedNaturally,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
)

fun PhotoRecapEntity.toDomain(): PhotoRecap {
    val paths = mutableListOf<String>()
    runCatching {
        val arr = JSONArray(imagePathsJson)
        for (i in 0 until arr.length()) {
            val p = arr.optString(i)
            if (p.isNotBlank()) paths.add(p)
        }
    }
    return PhotoRecap(
        dateIso = dateIso,
        title = title,
        caption = caption,
        imagePaths = paths,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun PhotoRecap.toEntity(): PhotoRecapEntity {
    val arr = JSONArray()
    imagePaths.take(4).forEach { arr.put(it) }
    return PhotoRecapEntity(
        dateIso = dateIso,
        title = title,
        caption = caption,
        imagePathsJson = arr.toString(),
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
