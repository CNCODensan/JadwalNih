package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.domain.DateTimeHelper
import com.example.domain.ScheduleTask
import com.example.domain.WorshipScheduleItem

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleForTask(task: ScheduleTask, checkInNotificationsEnabled: Boolean = true) {
        cancelForTask(task.id)
        if (task.isCompleted || task.id <= 0L) return

        TaskReminderReceiver.ensureChannel(context)
        val startMillis = DateTimeHelper.epochMillisFor(task.dateIso, task.startMinuteOfDay)
        val now = System.currentTimeMillis()

        val alreadyConfirmed = task.attendanceStatus == com.example.domain.AttendanceStatus.CONFIRMED_ATTENDING ||
            task.attendanceStatus == com.example.domain.AttendanceStatus.CONFIRMED_OVERRIDE

        if (task.reminderEnabled && !alreadyConfirmed) {
            val offsets = computeCascadingReminderOffsets(task.reminderLeadMinutes)
            offsets.take(MAX_CASCADING_SLOTS).forEachIndexed { idx, offsetMin ->
                val triggerAt = startMillis - (offsetMin * 60_000L)
                if (triggerAt > now) {
                    runCatching {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            buildPendingIntent(
                                task = task,
                                slotIndex = idx,
                                isCheckIn = false,
                                isLateFollowUp = false,
                                leadOffsetMinutes = offsetMin,
                            ),
                        )
                    }
                }
            }
        }

        if (checkInNotificationsEnabled && !alreadyConfirmed && (task.reminderEnabled || startMillis > now)) {
            if (startMillis > now) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startMillis,
                        buildPendingIntent(
                            task = task,
                            slotIndex = SLOT_CHECK_IN,
                            isCheckIn = true,
                            isLateFollowUp = false,
                            leadOffsetMinutes = 0,
                        ),
                    )
                }
            }
            val lateMillis = startMillis + LATE_FOLLOWUP_DELAY_MS
            if (lateMillis > now) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        lateMillis,
                        buildPendingIntent(
                            task = task,
                            slotIndex = SLOT_LATE_FOLLOWUP,
                            isCheckIn = true,
                            isLateFollowUp = true,
                            leadOffsetMinutes = 0,
                        ),
                    )
                }
            }
        }
    }

    fun cancelForTask(taskId: Long) {
        if (taskId <= 0L) return
        val base = taskRequestBase(taskId)
        for (slot in 0..SLOT_LATE_FOLLOWUP) {
            cancelRequestCode(base + slot)
        }
        // Also cancel any legacy request codes if previously scheduled
        cancelRequestCode(taskId.toInt())
        cancelRequestCode(taskId.toInt() + 100_000)
    }

    private fun cancelRequestCode(requestCode: Int) {
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    fun scheduleWorshipReminders(
        items: List<WorshipScheduleItem>,
        enabled: Boolean,
        soundEnabled: Boolean,
    ) {
        cancelWorshipReminders()
        if (!enabled) return

        TaskReminderReceiver.ensureChannel(context)
        val now = System.currentTimeMillis()
        items.take(8).forEachIndexed { idx, item ->
            if (item.isCompleted) return@forEachIndexed
            val triggerMillis = DateTimeHelper.epochMillisFor(item.dateIso, item.minuteOfDay)
            if (triggerMillis > now) {
                val requestCode = WORSHIP_REQ_BASE + idx
                val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                    putExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L)
                    putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, item.title)
                    putExtra(
                        TaskReminderReceiver.EXTRA_TIME_LABEL,
                        DateTimeHelper.formatMinuteOfDay(item.minuteOfDay),
                    )
                    putExtra(TaskReminderReceiver.EXTRA_QUADRANT_LABEL, item.subtitle)
                    putExtra(TaskReminderReceiver.EXTRA_IS_CHECK_IN, false)
                    putExtra(TaskReminderReceiver.EXTRA_IS_LATE_FOLLOWUP, false)
                    putExtra(TaskReminderReceiver.EXTRA_IS_WORSHIP, true)
                    putExtra(TaskReminderReceiver.EXTRA_WORSHIP_SOUND, soundEnabled)
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pending,
                    )
                }
            }
        }
    }

    fun cancelWorshipReminders() {
        for (idx in 0 until 8) {
            cancelRequestCode(WORSHIP_REQ_BASE + idx)
        }
    }

    private fun buildPendingIntent(
        task: ScheduleTask,
        slotIndex: Int,
        isCheckIn: Boolean,
        isLateFollowUp: Boolean,
        leadOffsetMinutes: Int,
    ): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(
                TaskReminderReceiver.EXTRA_TIME_LABEL,
                DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay),
            )
            putExtra(TaskReminderReceiver.EXTRA_QUADRANT_LABEL, task.quadrant.titleId)
            putExtra(TaskReminderReceiver.EXTRA_IS_CHECK_IN, isCheckIn)
            putExtra(TaskReminderReceiver.EXTRA_IS_LATE_FOLLOWUP, isLateFollowUp)
            putExtra(TaskReminderReceiver.EXTRA_LEAD_OFFSET_MINUTES, leadOffsetMinutes)
            putExtra(TaskReminderReceiver.EXTRA_IS_WORSHIP, false)
        }
        return PendingIntent.getBroadcast(
            context,
            taskRequestBase(task.id) + slotIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun taskRequestBase(taskId: Long): Int {
        return (taskId.toInt().and(0x7FFF) + 1) * TOTAL_SLOTS_PER_TASK
    }

    companion object {
        private const val MAX_CASCADING_SLOTS = 10
        private const val SLOT_CHECK_IN = 10
        private const val SLOT_LATE_FOLLOWUP = 11
        private const val TOTAL_SLOTS_PER_TASK = 16
        private const val LATE_FOLLOWUP_DELAY_MS = 5 * 60_000L
        private const val WORSHIP_REQ_BASE = 700_000

        /**
         * Builds progressive countdown reminder offsets (minutes before start) such as:
         * 60 -> [60, 40, 20, 10, 5]
         * 30 -> [30, 20, 10, 5]
         * 15 -> [15, 10, 5]
         * 5  -> [5]
         */
        fun computeCascadingReminderOffsets(leadMinutes: Int): List<Int> {
            if (leadMinutes <= 0) return emptyList()
            if (leadMinutes <= 5) return listOf(leadMinutes)

            val result = mutableListOf<Int>()
            var current = leadMinutes
            while (current > 5 && result.size < MAX_CASCADING_SLOTS) {
                result.add(current)
                current = when {
                    current > 90 -> current - 30
                    current > 60 -> 60
                    current > 45 -> 40
                    current > 34 -> 30
                    current > 24 -> 20
                    current > 15 -> 10
                    current > 10 -> 10
                    else -> 5
                }
            }
            if (result.lastOrNull() != 5 && result.size < MAX_CASCADING_SLOTS) {
                result.add(5)
            }
            return result.distinct()
        }
    }
}
