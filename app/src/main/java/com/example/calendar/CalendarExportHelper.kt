package com.example.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.domain.DateTimeHelper
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask

object CalendarExportHelper {
    fun launchCalendarInsert(context: Context, task: ScheduleTask): Boolean {
        val startMillis = DateTimeHelper.epochMillisFor(task.dateIso, task.startMinuteOfDay)
        val endMillis = DateTimeHelper.epochMillisFor(task.dateIso, task.endMinuteOfDay)
            .coerceAtLeast(startMillis + 60_000L)

        val description = buildString {
            append("Kuadran: ${task.quadrant.titleId}\n")
            append("Estimasi durasi: ${DateTimeHelper.formatDuration(task.estimatedMinutes)}\n")
            if (task.isFlexible) append("Catatan: Waktu aman / fleksibel\n")
            append("Diekspor dari JadwalNih")
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, task.title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            when (task.recurrence) {
                RecurrenceRule.DAILY -> putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
                RecurrenceRule.WEEKLY -> putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY")
                RecurrenceRule.MONTHLY -> putExtra(CalendarContract.Events.RRULE, "FREQ=MONTHLY")
                RecurrenceRule.YEARLY -> putExtra(CalendarContract.Events.RRULE, "FREQ=YEARLY")
                RecurrenceRule.NONE -> Unit
            }
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
