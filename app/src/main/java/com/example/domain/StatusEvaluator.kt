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

    /**
     * Generates translucent foreshadow ghost occurrences up to [horizonDays] (default 28 days / 4 weeks)
     * so recurring schedules (especially WEEKLY on the same weekday) remain visible in upcoming weeks.
     */
    fun buildForeshadowOccurrences(
        tasks: List<ScheduleTask>,
        todayIso: String = DateTimeHelper.todayIso(),
        horizonDays: Int = 28,
    ): List<ForeshadowOccurrence> {
        val recurringParents = tasks
            .filter { it.recurrence != RecurrenceRule.NONE }
            .groupBy { "${it.title.trim().lowercase()}|${it.startMinuteOfDay}|${it.recurrence.name}" }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.dateIso } }

        if (recurringParents.isEmpty()) return emptyList()

        val horizonEndIso = DateTimeHelper.addDays(todayIso, horizonDays)
        val existingSignatures = tasks.map {
            "${it.dateIso}|${it.title.trim().lowercase()}|${it.startMinuteOfDay}"
        }.toSet()
        val allSkipped = tasks.groupBy {
            "${it.title.trim().lowercase()}|${it.startMinuteOfDay}|${it.recurrence.name}"
        }.mapValues { (_, list) ->
            list.flatMap { it.skippedOccurrenceDates }.toSet()
        }

        val foreshadows = mutableListOf<ForeshadowOccurrence>()

        recurringParents.forEach { parent ->
            val groupKey = "${parent.title.trim().lowercase()}|${parent.startMinuteOfDay}|${parent.recurrence.name}"
            val skippedSet = allSkipped[groupKey].orEmpty()
            var cursor = nextOccurrenceDate(parent.dateIso, parent.recurrence)
            var guard = 0
            while (cursor != null && cursor <= horizonEndIso && guard < 35) {
                guard++
                if (cursor > todayIso) {
                    val sig = "$cursor|${parent.title.trim().lowercase()}|${parent.startMinuteOfDay}"
                    if (sig !in existingSignatures && cursor !in skippedSet) {
                        val weekOffset = when (parent.recurrence) {
                            RecurrenceRule.WEEKLY -> guard
                            RecurrenceRule.DAILY -> (guard + 6) / 7
                            RecurrenceRule.MONTHLY -> 4
                            RecurrenceRule.NONE -> 0
                        }
                        foreshadows.add(
                            ForeshadowOccurrence(
                                parentTask = parent,
                                occurrenceDateIso = cursor,
                                weekOffset = weekOffset,
                            ),
                        )
                    }
                }
                cursor = nextOccurrenceDate(cursor, parent.recurrence)
            }
        }
        return foreshadows.sortedWith(compareBy({ it.occurrenceDateIso }, { it.parentTask.startMinuteOfDay }))
    }

    fun evaluateDurationCategory(durationMinutes: Int): DurationCategory {
        return when {
            durationMinutes <= 30 -> DurationCategory.SHORT
            durationMinutes <= 90 -> DurationCategory.MEDIUM
            else -> DurationCategory.LONG
        }
    }

    fun evaluateDurationProgress(
        task: ScheduleTask,
        todayIso: String = DateTimeHelper.todayIso(),
        nowMinuteOfDay: Int = DateTimeHelper.nowMinuteOfDay(),
    ): DurationProgressInfo {
        val totalMinutes = (task.endMinuteOfDay - task.startMinuteOfDay)
            .coerceAtLeast(task.estimatedMinutes)
            .coerceAtLeast(1)
        val category = evaluateDurationCategory(totalMinutes)

        val isActiveNow = !task.isCompleted &&
            task.attendanceStatus != AttendanceStatus.CONFIRMED_OVERRIDE &&
            task.dateIso == todayIso &&
            nowMinuteOfDay in task.startMinuteOfDay..task.endMinuteOfDay

        if (!isActiveNow) {
            return DurationProgressInfo(
                category = category,
                totalMinutes = totalMinutes,
                isOngoing = false,
                elapsedFraction = 0f,
                remainingMinutes = totalMinutes,
                statusLabel = "${DateTimeHelper.formatDuration(totalMinutes)} • ${category.labelId}",
            )
        }

        val span = (task.endMinuteOfDay - task.startMinuteOfDay).coerceAtLeast(1)
        val elapsed = (nowMinuteOfDay - task.startMinuteOfDay).coerceIn(0, span)
        val remaining = (task.endMinuteOfDay - nowMinuteOfDay).coerceAtLeast(0)
        val fraction = (elapsed.toFloat() / span.toFloat()).coerceIn(0f, 1f)
        val label = if (remaining == 0) {
            "Berlangsung • Hampir selesai"
        } else {
            "Berlangsung • Sisa ${DateTimeHelper.formatDuration(remaining)}"
        }

        return DurationProgressInfo(
            category = category,
            totalMinutes = totalMinutes,
            isOngoing = true,
            elapsedFraction = fraction,
            remainingMinutes = remaining,
            statusLabel = label,
        )
    }
}
