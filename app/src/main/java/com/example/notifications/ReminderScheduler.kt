package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.domain.DateTimeHelper
import com.example.domain.ScheduleTask

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleForTask(task: ScheduleTask, checkInNotificationsEnabled: Boolean = true) {
        cancelForTask(task.id)
        if (task.isCompleted || task.id <= 0L) return

        TaskReminderReceiver.ensureChannel(context)
        val startMillis = DateTimeHelper.epochMillisFor(task.dateIso, task.startMinuteOfDay)
        val now = System.currentTimeMillis()

        if (task.reminderEnabled) {
            val leadTrigger = startMillis - (task.reminderLeadMinutes * 60_000L)
            if (leadTrigger > now) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        leadTrigger,
                        buildPendingIntent(task, requestCodeOffset = 0, isCheckIn = false),
                    )
                }
            }
        }

        if (checkInNotificationsEnabled && (task.reminderEnabled || startMillis > now)) {
            if (startMillis > now) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startMillis,
                        buildPendingIntent(task, requestCodeOffset = 100_000, isCheckIn = true),
                    )
                }
            }
        }
    }

    fun cancelForTask(taskId: Long) {
        if (taskId <= 0L) return
        listOf(0, 100_000).forEach { offset ->
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                taskId.toInt() + offset,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun buildPendingIntent(
        task: ScheduleTask,
        requestCodeOffset: Int,
        isCheckIn: Boolean,
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
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt() + requestCodeOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
