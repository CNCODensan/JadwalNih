package com.example.domain

object StatusEvaluator {
    fun evaluate(
        task: ScheduleTask,
        todayIso: String = DateTimeHelper.todayIso(),
        nowMinuteOfDay: Int = DateTimeHelper.nowMinuteOfDay(),
    ): TaskStatus {
        if (task.isCompleted || task.attendanceStatus == AttendanceStatus.CONFIRMED_OVERRIDE) {
            return TaskStatus.COMPLETED_GREEN
        }

        val isPastWindow = task.dateIso < todayIso ||
            (task.dateIso == todayIso && nowMinuteOfDay > task.endMinuteOfDay)

        if (isPastWindow) {
            return if (task.attendanceStatus == AttendanceStatus.CONFIRMED_ATTENDING) {
                TaskStatus.COMPLETED_GREEN
            } else {
                TaskStatus.OVERDUE_DARK
            }
        }

        return when {
            task.dateIso > todayIso -> {
                if (task.isFlexible) TaskStatus.FLEXIBLE_BLUE else TaskStatus.PENDING_GRAY
            }

            else -> {
                val minutesUntilEnd = task.endMinuteOfDay - nowMinuteOfDay
                val minutesUntilStart = task.startMinuteOfDay - nowMinuteOfDay
                val inActiveWindow = nowMinuteOfDay in task.startMinuteOfDay..task.endMinuteOfDay

                when {
                    task.attendanceStatus == AttendanceStatus.CONFIRMED_ATTENDING -> TaskStatus.FLEXIBLE_BLUE
                    task.isFlexible && minutesUntilEnd > 60 -> TaskStatus.FLEXIBLE_BLUE
                    minutesUntilEnd <= 20 || (inActiveWindow && minutesUntilEnd <= 30) -> TaskStatus.CRITICAL_RED
                    minutesUntilStart in 0..20 && task.quadrant == PriorityQuadrant.Q1_IMPORTANT_URGENT -> TaskStatus.CRITICAL_RED
                    minutesUntilStart <= 60 || minutesUntilEnd <= 60 -> TaskStatus.APPROACHING_YELLOW
                    task.isFlexible -> TaskStatus.FLEXIBLE_BLUE
                    else -> TaskStatus.PENDING_GRAY
                }
            }
        }
    }

    fun needsCheckInPrompt(
        task: ScheduleTask,
        todayIso: String = DateTimeHelper.todayIso(),
        nowMinuteOfDay: Int = DateTimeHelper.nowMinuteOfDay(),
    ): Boolean {
        if (task.isCompleted) return false
        if (task.dateIso != todayIso) return false
        if (task.attendanceStatus == AttendanceStatus.CONFIRMED_ATTENDING ||
            task.attendanceStatus == AttendanceStatus.CONFIRMED_OVERRIDE
        ) {
            return false
        }
        val windowStart = (task.startMinuteOfDay - 15).coerceAtLeast(0)
        return nowMinuteOfDay in windowStart..task.endMinuteOfDay
    }

    fun nextOccurrenceDate(currentDateIso: String, rule: RecurrenceRule): String? {
        return when (rule) {
            RecurrenceRule.NONE -> null
            RecurrenceRule.DAILY -> DateTimeHelper.addDays(currentDateIso, 1)
            RecurrenceRule.WEEKLY -> DateTimeHelper.addDays(currentDateIso, 7)
            RecurrenceRule.MONTHLY -> DateTimeHelper.addMonths(currentDateIso, 1)
        }
    }
}
