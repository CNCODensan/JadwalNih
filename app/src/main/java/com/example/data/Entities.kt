package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.AttendanceStatus
import com.example.domain.FocusSession
import com.example.domain.PhotoRecap
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask
import com.example.domain.SubTaskItem
import org.json.JSONArray
import org.json.JSONObject

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
    val subtasksJson: String = "[]",
    val attendanceStatusName: String = AttendanceStatus.UNCONFIRMED.name,
    val isSubtasksTodoMode: Boolean = false,
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

fun serializeSubTasks(subtasks: List<SubTaskItem>): String {
    val arr = JSONArray()
    subtasks.forEach { item ->
        arr.put(
            JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("isDone", item.isDone)
            },
        )
    }
    return arr.toString()
}

fun deserializeSubTasks(json: String?): List<SubTaskItem> {
    if (json.isNullOrBlank()) return emptyList()
    val list = mutableListOf<SubTaskItem>()
    runCatching {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val title = obj.optString("title").trim()
            if (title.isEmpty()) continue
            val id = obj.optString("id").ifBlank { "sub_${i}_${title.hashCode()}" }
            list.add(
                SubTaskItem(
                    id = id,
                    title = title,
                    isDone = obj.optBoolean("isDone", false),
                ),
            )
        }
    }
    return list
}

fun ScheduleTaskEntity.toDomain(): ScheduleTask = ScheduleTask(
    id = id,
    title = title,
    dateIso = dateIso,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    estimatedMinutes = (endMinuteOfDay - startMinuteOfDay).coerceAtLeast(1),
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
    subtasks = deserializeSubTasks(subtasksJson),
    attendanceStatus = runCatching { AttendanceStatus.valueOf(attendanceStatusName) }
        .getOrDefault(AttendanceStatus.UNCONFIRMED),
    isSubtasksTodoMode = isSubtasksTodoMode,
)

fun ScheduleTask.toEntity(): ScheduleTaskEntity = ScheduleTaskEntity(
    id = id,
    title = title,
    dateIso = dateIso,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    estimatedMinutes = (endMinuteOfDay - startMinuteOfDay).coerceAtLeast(1),
    quadrantName = quadrant.name,
    isCompleted = isCompleted,
    isFlexible = isFlexible,
    recurrenceName = recurrence.name,
    reminderEnabled = reminderEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    subtasksJson = serializeSubTasks(subtasks),
    attendanceStatusName = attendanceStatus.name,
    isSubtasksTodoMode = isSubtasksTodoMode,
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
