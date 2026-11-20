package com.example

import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class JadwalNihDomainTest {
    private val today = "2026-11-20"

    private fun sampleTask(
        dateIso: String = today,
        startMin: Int = 10 * 60,
        endMin: Int = 11 * 60,
        isCompleted: Boolean = false,
        isFlexible: Boolean = false,
        quadrant: PriorityQuadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
    ) = ScheduleTask(
        id = 1L,
        title = "Tes Kegiatan",
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

        // 1. Hijau: Completed
        assertEquals(
            TaskStatus.COMPLETED_GREEN,
            StatusEvaluator.evaluate(sampleTask(isCompleted = true), today, now),
        )

        // 2. Hitam/Gelap: Overdue
        assertEquals(
            TaskStatus.OVERDUE_DARK,
            StatusEvaluator.evaluate(sampleTask(startMin = 7 * 60, endMin = 8 * 60), today, now),
        )

        // 3. Merah: Mepet Banget (<= 20 min before end)
        assertEquals(
            TaskStatus.CRITICAL_RED,
            StatusEvaluator.evaluate(sampleTask(startMin = 8 * 60 + 30, endMin = 9 * 60 + 15), today, now),
        )

        // 4. Kuning: Mau Mepet (<= 60 min before start/end)
        assertEquals(
            TaskStatus.APPROACHING_YELLOW,
            StatusEvaluator.evaluate(sampleTask(startMin = 9 * 60 + 40, endMin = 10 * 60 + 40), today, now),
        )

        // 5. Biru: Waktu Aman / Fleksibel
        assertEquals(
            TaskStatus.FLEXIBLE_BLUE,
            StatusEvaluator.evaluate(sampleTask(startMin = 14 * 60, endMin = 15 * 60, isFlexible = true), today, now),
        )

        // 6. Abu-abu: Belum Dikerjain
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
}
