package com.example

import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JadwalNihDomainTest {
    private val today = "2026-11-20"

    private fun sampleTask(
        id: Long = 1L,
        title: String = "Tes Kegiatan",
        dateIso: String = today,
        startMin: Int = 10 * 60,
        endMin: Int = 11 * 60,
        isCompleted: Boolean = false,
        isFlexible: Boolean = false,
        quadrant: PriorityQuadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
    ) = ScheduleTask(
        id = id,
        title = title,
        dateIso = dateIso,
        startMinuteOfDay = startMin,
        endMinuteOfDay = endMin,
        estimatedMinutes = endMin - startMin,
        quadrant = quadrant,
        isCompleted = isCompleted,
        isFlexible = isFlexible,
    )

    @Test
    fun statusEvaluator_coversAllSixSemanticStatuses() {
        val now = 9 * 60 // 09:00

        assertEquals(
            TaskStatus.COMPLETED_GREEN,
            StatusEvaluator.evaluate(sampleTask(isCompleted = true), today, now),
        )
        assertEquals(
            TaskStatus.OVERDUE_DARK,
            StatusEvaluator.evaluate(sampleTask(startMin = 7 * 60, endMin = 8 * 60), today, now),
        )
        assertEquals(
            TaskStatus.CRITICAL_RED,
            StatusEvaluator.evaluate(sampleTask(startMin = 8 * 60 + 30, endMin = 9 * 60 + 15), today, now),
        )
        assertEquals(
            TaskStatus.APPROACHING_YELLOW,
            StatusEvaluator.evaluate(sampleTask(startMin = 9 * 60 + 40, endMin = 10 * 60 + 40), today, now),
        )
        assertEquals(
            TaskStatus.FLEXIBLE_BLUE,
            StatusEvaluator.evaluate(sampleTask(startMin = 14 * 60, endMin = 15 * 60, isFlexible = true), today, now),
        )
        assertEquals(
            TaskStatus.PENDING_GRAY,
            StatusEvaluator.evaluate(sampleTask(startMin = 14 * 60, endMin = 15 * 60, isFlexible = false), today, now),
        )
    }

    @Test
    fun recurrence_advancesDatesCorrectly() {
        assertEquals("2026-11-21", StatusEvaluator.nextOccurrenceDate("2026-11-20", RecurrenceRule.DAILY))
        assertEquals("2026-11-27", StatusEvaluator.nextOccurrenceDate("2026-11-20", RecurrenceRule.WEEKLY))
        assertEquals("2026-12-20", StatusEvaluator.nextOccurrenceDate("2026-11-20", RecurrenceRule.MONTHLY))
        assertEquals(null, StatusEvaluator.nextOccurrenceDate("2026-11-20", RecurrenceRule.NONE))
    }

    @Test
    fun dateTimeHelper_formatsIndonesianDurationAndCountdown() {
        assertEquals("45 mnt", DateTimeHelper.formatDuration(45))
        assertEquals("1j 30m", DateTimeHelper.formatDuration(90))
        assertEquals("25:00", DateTimeHelper.formatCountdown(25 * 60))
        assertEquals("01:05:00", DateTimeHelper.formatCountdown(65 * 60))
    }

    @Test
    fun scheduleAnalyzer_detectsOverlapsAndDailyLoad() {
        val t1 = sampleTask(id = 1L, title = "Rapat A", startMin = 9 * 60, endMin = 10 * 60)
        val t2 = sampleTask(id = 2L, title = "Rapat B", startMin = 9 * 60 + 30, endMin = 10 * 60 + 30)
        val t3 = sampleTask(id = 3L, title = "Olahraga", startMin = 11 * 60, endMin = 12 * 60)
        val t4Done = sampleTask(id = 4L, title = "Selesai", startMin = 9 * 60, endMin = 10 * 60, isCompleted = true)

        val conflicts = ScheduleAnalyzer.detectConflicts(listOf(t1, t2, t3, t4Done))
        assertEquals(2, conflicts.size)
        assertTrue(conflicts.containsKey(1L))
        assertTrue(conflicts.containsKey(2L))
        assertEquals(listOf("Rapat B"), conflicts[1L]?.conflictingTitles)

        assertEquals(240, ScheduleAnalyzer.dailyScheduledMinutes(listOf(t1, t2, t3, t4Done), today))
    }

    @Test
    fun scheduleAnalyzer_computesStreakAndHeatmap35Days() {
        val tToday = sampleTask(id = 1L, dateIso = today, isCompleted = true)
        val tYesterday = sampleTask(id = 2L, dateIso = "2026-11-19", isCompleted = true)
        val tDayBefore = sampleTask(id = 3L, dateIso = "2026-11-18", isCompleted = true)

        val streak = ScheduleAnalyzer.computeStreakDays(
            tasks = listOf(tToday, tYesterday, tDayBefore),
            sessions = emptyList<FocusSession>(),
            todayIso = today,
        )
        assertEquals(3, streak)

        val cells = ScheduleAnalyzer.buildHeatmap35Days(
            tasks = listOf(tToday, tYesterday, tDayBefore),
            sessions = emptyList(),
            todayIso = today,
        )
        assertEquals(35, cells.size)
        assertTrue(cells.any { it.dateIso == today && it.completedCount == 1 && it.level >= 1 })
    }

    @Test
    fun scheduleAnalyzer_buildsGreetingByHourAndProgress() {
        val morning = ScheduleAnalyzer.buildGreeting(hourOfDay = 8, todayCompleted = 2, todayTotal = 2, streakDays = 3)
        assertTrue(morning.greetingText.contains("Pagi"))
        assertTrue(morning.encouragementText.contains("100%"))

        val night = ScheduleAnalyzer.buildGreeting(hourOfDay = 21, todayCompleted = 0, todayTotal = 3, streakDays = 0)
        assertTrue(night.greetingText.contains("Malam"))
    }
}
