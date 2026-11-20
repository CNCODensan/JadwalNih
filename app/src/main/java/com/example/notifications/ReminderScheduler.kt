package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.domain.DateTimeHelper
import com.example.domain.ScheduleTask

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleForTask(task: ScheduleTask) {
        cancelForTask(task.id)
        if (!task.reminderEnabled || task.isCompleted || task.id <= 0L) return

        TaskReminderReceiver.ensureChannel(context)

        val startMillis = DateTimeHelper.epochMillisFor(task.dateIso, task.startMinuteOfDay)
        val triggerMillis = startMillis - (task.reminderLeadMinutes * 60_000L)
        if (triggerMillis <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(task)
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent,
            )
        }
    }

    fun cancelForTask(taskId: Long) {
        if (taskId <= 0L) return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun buildPendingIntent(task: ScheduleTask): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(
                TaskReminderReceiver.EXTRA_TIME_LABEL,
                DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay),
            )
            putExtra(TaskReminderReceiver.EXTRA_QUADRANT_LABEL, task.quadrant.titleId)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
