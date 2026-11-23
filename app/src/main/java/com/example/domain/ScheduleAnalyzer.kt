package com.example.domain

import java.util.Calendar

object ScheduleAnalyzer {

    fun detectConflicts(tasks: List<ScheduleTask>): Map<Long, ConflictInfo> {
        val activeTasks = tasks.filter { !it.isCompleted && it.endMinuteOfDay > it.startMinuteOfDay }
        val byDate = activeTasks.groupBy { it.dateIso }
        val result = mutableMapOf<Long, ConflictInfo>()

        byDate.values.forEach { dayTasks ->
            if (dayTasks.size < 2) return@forEach
            for (i in dayTasks.indices) {
                val a = dayTasks[i]
                val colliding = mutableListOf<String>()
                for (j in dayTasks.indices) {
                    if (i == j) continue
                    val b = dayTasks[j]
                    if (intervalsOverlap(a.startMinuteOfDay, a.endMinuteOfDay, b.startMinuteOfDay, b.endMinuteOfDay)) {
                        colliding.add(b.title)
                    }
                }
                if (colliding.isNotEmpty()) {
                    val summary = if (colliding.size == 1) {
                        "Bentrok: ${colliding.first()}"
                    } else {
                        "Bentrok ${colliding.size} jadwal"
                    }
                    result[a.id] = ConflictInfo(
                        conflictingTitles = colliding,
                        summaryText = summary,
                    )
                }
            }
        }
        return result
    }

    fun detectConflictsForDraft(
        dateIso: String,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        excludeTaskId: Long,
        existingTasks: List<ScheduleTask>,
    ): List<ScheduleTask> {
        if (endMinuteOfDay <= startMinuteOfDay) return emptyList()
        return existingTasks.filter { other ->
            other.id != excludeTaskId &&
                !other.isCompleted &&
                other.dateIso == dateIso &&
                intervalsOverlap(startMinuteOfDay, endMinuteOfDay, other.startMinuteOfDay, other.endMinuteOfDay)
        }.sortedBy { it.startMinuteOfDay }
    }

    fun intervalsOverlap(startA: Int, endA: Int, startB: Int, endB: Int): Boolean {
        return startA < endB && startB < endA
    }

    fun dailyScheduledMinutes(tasks: List<ScheduleTask>, dateIso: String): Int {
        return tasks
            .filter { it.dateIso == dateIso }
            .sumOf { (it.endMinuteOfDay - it.startMinuteOfDay).coerceAtLeast(0) }
    }

    fun buildHourlyTimeline(
        dayTasks: List<TaskWithStatus>,
        defaultStartHour: Int = 6,
        defaultEndHour: Int = 22,
    ): List<DailyTimelineEntry> {
        val sorted = dayTasks.sortedBy { it.task.startMinuteOfDay }
        val minHour = minOf(
            defaultStartHour,
            sorted.minOfOrNull { (it.task.startMinuteOfDay / 60).coerceIn(0, 23) } ?: defaultStartHour,
        )
        val maxHour = maxOf(
            defaultEndHour,
            sorted.maxOfOrNull { ((it.task.endMinuteOfDay - 1).coerceAtLeast(0) / 60).coerceIn(0, 23) } ?: defaultEndHour,
        )

        val entries = mutableListOf<DailyTimelineEntry>()
        var lastRenderedTaskEnd: Int? = null

        for (hour in minHour..maxHour) {
            val slotStartMin = hour * 60
            val slotEndMin = slotStartMin + 60
            val startingInHour = sorted.filter { (it.task.startMinuteOfDay / 60) == hour }
            val spansIntoHour = sorted.any {
                it.task.startMinuteOfDay < slotStartMin && it.task.endMinuteOfDay > slotStartMin
            }

            if (startingInHour.isEmpty()) {
                if (!spansIntoHour) {
                    entries.add(
                        DailyTimelineEntry.HourSlotEmpty(
                            hour = hour,
                            label = DateTimeHelper.formatHourSlot(hour),
                        ),
                    )
                    lastRenderedTaskEnd = null
                }
            } else {
                startingInHour.forEach { item ->
                    val prevEnd = lastRenderedTaskEnd
                    if (prevEnd != null && item.task.startMinuteOfDay > prevEnd) {
                        val gap = item.task.startMinuteOfDay - prevEnd
                        if (gap in 5..59) {
                            entries.add(
                                DailyTimelineEntry.GapBreak(
                                    startMinuteOfDay = prevEnd,
                                    endMinuteOfDay = item.task.startMinuteOfDay,
                                    durationMinutes = gap,
                                ),
                            )
                        }
                    }
                    val duration = (item.task.endMinuteOfDay - item.task.startMinuteOfDay).coerceAtLeast(1)
                    entries.add(
                        DailyTimelineEntry.TaskBlock(
                            item = item,
                            durationMinutes = duration,
                        ),
                    )
                    lastRenderedTaskEnd = maxOf(lastRenderedTaskEnd ?: 0, item.task.endMinuteOfDay)
                }
                if ((lastRenderedTaskEnd ?: 0) >= slotEndMin) {
                    // Task spans into next hour cleanly
                }
            }
        }
        return entries
    }

    fun buildHeatmap35Days(
        tasks: List<ScheduleTask>,
        sessions: List<FocusSession>,
        todayIso: String,
    ): List<HeatmapDayCell> {
        val completedByDate = tasks
            .filter { it.isCompleted || it.attendanceStatus == AttendanceStatus.CONFIRMED_OVERRIDE }
            .groupingBy { it.dateIso }
            .eachCount()

        val focusSecondsByDate = mutableMapOf<String, Int>()
        sessions.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startedAtEpochMs }
            val iso = DateTimeHelper.calendarToIso(cal)
            focusSecondsByDate[iso] = (focusSecondsByDate[iso] ?: 0) + session.actualSeconds
        }

        val currentWeekDays = DateTimeHelper.weekDaysFor(todayIso)
        val thisMondayIso = currentWeekDays.first()
        val gridStartIso = DateTimeHelper.addDays(thisMondayIso, -28)

        return (0 until 35).map { offset ->
            val dayIso = DateTimeHelper.addDays(gridStartIso, offset)
            val isFuture = dayIso > todayIso
            val doneCount = if (isFuture) 0 else (completedByDate[dayIso] ?: 0)
            val focusMins = if (isFuture) 0 else ((focusSecondsByDate[dayIso] ?: 0) / 60)
            val score = doneCount * 2 + (focusMins / 20)
            val level = when {
                isFuture -> 0
                score <= 0 -> 0
                score <= 2 -> 1
                score <= 4 -> 2
                score <= 7 -> 3
                else -> 4
            }
            HeatmapDayCell(
                dateIso = dayIso,
                completedCount = doneCount,
                focusMinutes = focusMins,
                level = level,
                isFuture = isFuture,
            )
        }
    }

    fun computeStreakDays(
        tasks: List<ScheduleTask>,
        sessions: List<FocusSession>,
        todayIso: String,
    ): Int {
        val activeDates = mutableSetOf<String>()
        tasks.forEach { task ->
            if (task.isCompleted || task.attendanceStatus == AttendanceStatus.CONFIRMED_OVERRIDE) {
                activeDates.add(task.dateIso)
            }
        }
        sessions.forEach { session ->
            if (session.actualSeconds >= 60) {
                val cal = Calendar.getInstance().apply { timeInMillis = session.startedAtEpochMs }
                activeDates.add(DateTimeHelper.calendarToIso(cal))
            }
        }
        if (activeDates.isEmpty()) return 0

        var cursor = if (todayIso in activeDates) {
            todayIso
        } else {
            val yesterday = DateTimeHelper.addDays(todayIso, -1)
            if (yesterday in activeDates) yesterday else return 0
        }

        var streak = 0
        while (cursor in activeDates && streak < 365) {
            streak++
            cursor = DateTimeHelper.addDays(cursor, -1)
        }
        return streak
    }

    fun computeWorshipAnalytics(
        religion: ReligionOption,
        enabled: Boolean,
        todayIso: String,
        completedDateItemKeys: Set<String>,
        todayWorshipItems: List<WorshipScheduleItem>,
    ): WorshipAnalyticsSummary {
        val weekDays = DateTimeHelper.weekDaysFor(todayIso).toSet()
        val completedThisWeek = completedDateItemKeys.count { composite ->
            val datePart = composite.substringBefore("|")
            datePart in weekDays
        }
        val expected = PrayerTimesService.computeExpectedWeeklyWorshipCount(religion)
        return WorshipAnalyticsSummary(
            religion = religion,
            enabled = enabled,
            completedThisWeek = completedThisWeek,
            expectedThisWeek = expected,
            completedToday = todayWorshipItems.count { it.isCompleted },
            totalToday = todayWorshipItems.size,
        )
    }

    fun buildGreeting(
        userName: String = "",
        hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        todayCompleted: Int,
        todayTotal: Int,
        streakDays: Int,
    ): GreetingState {
        val trimmedName = userName.trim()
        val greeting = when (hourOfDay) {
            in 4..10 -> if (trimmedName.isNotEmpty()) "Pagi, $trimmedName!" else "Pagi! Siap jalani hari ini?"
            in 11..14 -> if (trimmedName.isNotEmpty()) "Siang, $trimmedName!" else "Siang! Istirahat sebentar, lanjut lagi"
            in 15..17 -> if (trimmedName.isNotEmpty()) "Sore, $trimmedName!" else "Sore! Sedikit lagi beres semua"
            else -> if (trimmedName.isNotEmpty()) "Malam, $trimmedName!" else "Malam! Waktunya rekap santai"
        }

        val encouragement = when {
            todayTotal == 0 -> "Belum ada jadwal hari ini. Tambah 1 kegiatan ringan yuk!"
            todayCompleted >= todayTotal -> "Keren! Semua jadwal hari ini selesai 100%."
            todayCompleted * 2 >= todayTotal -> "Udah lewat setengah jalan, lanjut dikit lagi!"
            streakDays >= 2 -> "Streak $streakDays hari lagi jalan—pertahanin ya!"
            else -> "Mulai dari satu kegiatan paling ringan biar langsung jalan."
        }
        return GreetingState(
            greetingText = greeting,
            encouragementText = encouragement,
        )
    }

    fun buildWidgetPrioritySummary(
        tasks: List<ScheduleTask>,
        filter: WidgetQuadrantFilter,
        todayIso: String = DateTimeHelper.todayIso(),
        nowMinuteOfDay: Int = DateTimeHelper.nowMinuteOfDay(),
        nextWorship: WorshipScheduleItem? = null,
        worshipCountDownLabel: String = "",
        worshipEnabled: Boolean = false,
    ): WidgetPrioritySummary {
        val todayTasks = tasks
            .filter { it.dateIso == todayIso }
            .map { task ->
                TaskWithStatus(
                    task = task,
                    status = StatusEvaluator.evaluate(task, todayIso, nowMinuteOfDay),
                )
            }

        val matching = todayTasks.filter { filter.matches(it.task.quadrant) }
        val activeTasks = matching
            .filterNot { it.status == TaskStatus.COMPLETED_GREEN }
            .sortedWith(
                compareBy<TaskWithStatus>(
                    { it.task.quadrant.ordinal },
                    { it.task.startMinuteOfDay },
                    { it.task.id },
                ),
            )
        val completedInFilterCount = matching.count { it.status == TaskStatus.COMPLETED_GREEN }
        val totalTodayCompletedCount = todayTasks.count { it.status == TaskStatus.COMPLETED_GREEN }

        return WidgetPrioritySummary(
            todayDateIso = todayIso,
            filter = filter,
            activeTasks = activeTasks,
            completedInFilterCount = completedInFilterCount,
            totalTodayCount = todayTasks.size,
            totalTodayCompletedCount = totalTodayCompletedCount,
            nextWorship = nextWorship,
            worshipCountDownLabel = worshipCountDownLabel,
            worshipEnabled = worshipEnabled,
        )
    }
}
