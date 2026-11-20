package com.example.domain

object StatusEvaluator {
    fun evaluate(
        task: ScheduleTask,
        todayIso: String = DateTimeHelper.todayIso(),
        nowMinuteOfDay: Int = DateTimeHelper.nowMinuteOfDay(),
    ): TaskStatus {
        if (task.isCompleted) {
            return TaskStatus.COMPLETED_GREEN
        }

        return when {
            task.dateIso < todayIso -> TaskStatus.OVERDUE_DARK
            task.dateIso > todayIso -> {
                if (task.isFlexible) TaskStatus.FLEXIBLE_BLUE else TaskStatus.PENDING_GRAY
            }
            else -> {
                if (nowMinuteOfDay > task.endMinuteOfDay) {
                    TaskStatus.OVERDUE_DARK
                } else {
                    val minutesUntilEnd = task.endMinuteOfDay - nowMinuteOfDay
                    val minutesUntilStart = task.startMinuteOfDay - nowMinuteOfDay
                    val inActiveWindow = nowMinuteOfDay in task.startMinuteOfDay..task.endMinuteOfDay

                    when {
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
