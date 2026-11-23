package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleTask
import com.example.domain.SubTaskItem
import com.example.domain.WidgetQuadrantFilter
import com.example.domain.WorshipScheduleItem
import com.example.ui.theme.JadwalNihTheme
import com.example.widget.CycleWidgetQuadrantAction
import com.example.widget.PriorityQuadrantWidget
import com.example.widget.PriorityQuadrantWidgetContent
import com.example.widget.QuickAddWidgetActivity
import com.example.widget.QuickAddWidgetPopup
import com.example.widget.SelectWidgetQuadrantAction
import com.example.widget.ToggleWidgetTaskAction
import com.example.widget.ToggleWidgetWorshipAction
import com.example.widget.WidgetSizeTier
import com.example.widget.resolveWidgetSizeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class PriorityQuadrantWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = "2026-11-23"

    private fun sampleTasks(): List<ScheduleTask> = listOf(
        ScheduleTask(
            id = 1L,
            title = "Rapat Evaluasi",
            dateIso = today,
            startMinuteOfDay = 8 * 60,
            endMinuteOfDay = 9 * 60 + 30,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
            subtasks = listOf(
                SubTaskItem("s1", "Materi", isDone = true),
                SubTaskItem("s2", "Notulen", isDone = false),
            ),
        ),
        ScheduleTask(
            id = 2L,
            title = "Rancang Strategi",
            dateIso = today,
            startMinuteOfDay = 10 * 60,
            endMinuteOfDay = 11 * 60,
            quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
        ),
        ScheduleTask(
            id = 3L,
            title = "Kirim Laporan",
            dateIso = today,
            startMinuteOfDay = 13 * 60,
            endMinuteOfDay = 14 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        ),
        ScheduleTask(
            id = 4L,
            title = "Balas Pesan",
            dateIso = today,
            startMinuteOfDay = 14 * 60 + 30,
            endMinuteOfDay = 15 * 60,
            quadrant = PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT,
        ),
        ScheduleTask(
            id = 5L,
            title = "Olahraga Ringan",
            dateIso = today,
            startMinuteOfDay = 17 * 60,
            endMinuteOfDay = 18 * 60,
            quadrant = PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT,
            isCompleted = true,
        ),
        ScheduleTask(
            id = 6L,
            title = "Tugas Besok",
            dateIso = "2026-11-24",
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 10 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        ),
    )

    @Test
    fun `buildWidgetPrioritySummary filters focus K1-K2, single quadrants, and excludes completed`() {
        val tasks = sampleTasks()
        val nowMin = 9 * 60 // 09:00

        val focusSummary = ScheduleAnalyzer.buildWidgetPrioritySummary(
            tasks = tasks,
            filter = WidgetQuadrantFilter.FOCUS_K1_K2,
            todayIso = today,
            nowMinuteOfDay = nowMin,
        )
        assertEquals(listOf(1L, 3L, 2L), focusSummary.activeTasks.map { it.task.id })
        assertEquals(5, focusSummary.totalTodayCount)
        assertEquals(1, focusSummary.totalTodayCompletedCount)
        assertEquals(0, focusSummary.completedInFilterCount)

        val k3Summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
            tasks = tasks,
            filter = WidgetQuadrantFilter.K3_ONLY,
            todayIso = today,
            nowMinuteOfDay = nowMin,
        )
        assertEquals(listOf(4L), k3Summary.activeTasks.map { it.task.id })

        val k4Summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
            tasks = tasks,
            filter = WidgetQuadrantFilter.K4_ONLY,
            todayIso = today,
            nowMinuteOfDay = nowMin,
        )
        assertTrue(k4Summary.activeTasks.isEmpty())
        assertEquals(1, k4Summary.completedInFilterCount)

        assertEquals(
            WidgetSizeTier.COMPACT,
            resolveWidgetSizeTier(PriorityQuadrantWidget.COMPACT_SIZE),
        )
        assertEquals(
            WidgetSizeTier.MEDIUM,
            resolveWidgetSizeTier(PriorityQuadrantWidget.MEDIUM_SIZE),
        )
        assertEquals(
            WidgetSizeTier.EXPANDED,
            resolveWidgetSizeTier(PriorityQuadrantWidget.EXPANDED_SIZE),
        )
    }

    @Test
    fun `glance widget compact size shows max 2 tasks, extra overflow count, and quick add`() =
        runGlanceAppWidgetUnitTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            setContext(context)
            setAppWidgetSize(PriorityQuadrantWidget.COMPACT_SIZE)

            val summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
                tasks = sampleTasks(),
                filter = WidgetQuadrantFilter.FOCUS_K1_K2,
                todayIso = today,
                nowMinuteOfDay = 9 * 60,
            )

            provideComposable {
                PriorityQuadrantWidgetContent(
                    summary = summary,
                    onCycleFilter = actionRunCallback<CycleWidgetQuadrantAction>(),
                    onSelectFilter = { actionRunCallback<SelectWidgetQuadrantAction>() },
                    onToggleTask = { actionRunCallback<ToggleWidgetTaskAction>() },
                    onQuickAdd = actionStartActivity(Intent(context, QuickAddWidgetActivity::class.java)),
                    onOpenApp = actionStartActivity<MainActivity>(),
                )
            }

            onNode(hasTestTag("widget_tier_compact")).assertExists()
            onNode(hasTestTag("widget_filter_cycle_button")).assertHasClickAction()
            onNode(hasText("Fokus K1–K2")).assertExists()
            onNode(hasTestTag("widget_quick_add_button")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_1")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_3")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_2")).assertDoesNotExist()
            onNode(hasText("+1 tugas lagi")).assertExists()
        }

    @Test
    fun `glance widget medium size shows 3 tasks with quadrant code and start time`() =
        runGlanceAppWidgetUnitTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            setContext(context)
            setAppWidgetSize(PriorityQuadrantWidget.MEDIUM_SIZE)

            val summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
                tasks = sampleTasks(),
                filter = WidgetQuadrantFilter.FOCUS_K1_K2,
                todayIso = today,
                nowMinuteOfDay = 9 * 60,
            )

            provideComposable {
                PriorityQuadrantWidgetContent(
                    summary = summary,
                    onCycleFilter = actionRunCallback<CycleWidgetQuadrantAction>(),
                    onSelectFilter = { actionRunCallback<SelectWidgetQuadrantAction>() },
                    onToggleTask = { actionRunCallback<ToggleWidgetTaskAction>() },
                    onQuickAdd = actionStartActivity(Intent(context, QuickAddWidgetActivity::class.java)),
                    onOpenApp = actionStartActivity<MainActivity>(),
                )
            }

            onNode(hasTestTag("widget_tier_medium")).assertExists()
            onNode(hasTestTag("widget_quick_add_button")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_1")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_3")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_2")).assertHasClickAction()
            onNode(hasText("Rapat Evaluasi")).assertExists()
            onNode(hasText("08:00")).assertExists()
        }

    @Test
    fun `glance widget expanded size shows all quadrant chips and detailed task metadata`() =
        runGlanceAppWidgetUnitTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            setContext(context)
            setAppWidgetSize(PriorityQuadrantWidget.EXPANDED_SIZE)

            val summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
                tasks = sampleTasks(),
                filter = WidgetQuadrantFilter.ALL,
                todayIso = today,
                nowMinuteOfDay = 9 * 60,
            )

            provideComposable {
                PriorityQuadrantWidgetContent(
                    summary = summary,
                    onCycleFilter = actionRunCallback<CycleWidgetQuadrantAction>(),
                    onSelectFilter = { actionRunCallback<SelectWidgetQuadrantAction>() },
                    onToggleTask = { actionRunCallback<ToggleWidgetTaskAction>() },
                    onQuickAdd = actionStartActivity(Intent(context, QuickAddWidgetActivity::class.java)),
                    onOpenApp = actionStartActivity<MainActivity>(),
                )
            }

            onNode(hasTestTag("widget_tier_expanded")).assertExists()
            WidgetQuadrantFilter.entries.forEach { option ->
                onNode(hasTestTag("widget_filter_chip_${option.name}")).assertHasClickAction()
            }
            onNode(hasTestTag("widget_task_check_1")).assertHasClickAction()
            onNode(hasTestTag("widget_task_check_4")).assertHasClickAction()
            onNode(hasText("08:00 - 09:30 • 1j 30m • 1/2 langkah")).assertExists()
        }

    @Test
    fun `quick add widget popup validates empty title and saves task with chosen quadrant and duration`() {
        var savedTask: ScheduleTask? = null

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                QuickAddWidgetPopup(
                    initialQuadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
                    onDismiss = {},
                    onSaveTask = { savedTask = it },
                )
            }
        }
        composeRule.waitForIdle()

        // Empty title validation
        composeRule.onNodeWithTag("widget_quick_add_save").performScrollTo().performClick()
        composeRule.onNodeWithTag("widget_quick_add_error").performScrollTo().assertIsDisplayed()

        // Fill title, pick K3, pick 2 jam preset, save
        composeRule.onNodeWithTag("widget_quick_add_title_input")
            .performScrollTo()
            .performTextReplacement("Siapkan materi")
        composeRule.onNodeWithTag("widget_quick_add_quadrant_K3").performScrollTo().performClick()
        composeRule.onNodeWithTag("widget_quick_add_dur_120").performScrollTo().performClick()
        composeRule.onNodeWithTag("widget_quick_add_save").performScrollTo().performClick()

        assertNotNull(savedTask)
        assertEquals("Siapkan materi", savedTask?.title)
        assertEquals(PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT, savedTask?.quadrant)
        assertEquals(DateTimeHelper.todayIso(), savedTask?.dateIso)
        assertTrue((savedTask?.endMinuteOfDay ?: 0) > (savedTask?.startMinuteOfDay ?: 0))
        assertEquals(120, savedTask?.estimatedMinutes)
    }

    @Test
    fun `glance widget expanded size renders dual-action worship bar when worship is enabled`() =
        runGlanceAppWidgetUnitTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            setContext(context)
            setAppWidgetSize(PriorityQuadrantWidget.EXPANDED_SIZE)

            val worship = WorshipScheduleItem(
                key = "ashar",
                title = "Ashar",
                subtitle = "Sholat Ashar",
                minuteOfDay = 15 * 60 + 15,
                dateIso = today,
                isCompleted = false,
            )
            val summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
                tasks = sampleTasks(),
                filter = WidgetQuadrantFilter.FOCUS_K1_K2,
                todayIso = today,
                nowMinuteOfDay = 14 * 60,
                nextWorship = worship,
                worshipCountDownLabel = "1j 15m lagi",
                worshipEnabled = true,
            )

            provideComposable {
                PriorityQuadrantWidgetContent(
                    summary = summary,
                    onCycleFilter = actionRunCallback<CycleWidgetQuadrantAction>(),
                    onSelectFilter = { actionRunCallback<SelectWidgetQuadrantAction>() },
                    onToggleTask = { actionRunCallback<ToggleWidgetTaskAction>() },
                    onQuickAdd = actionStartActivity(Intent(context, QuickAddWidgetActivity::class.java)),
                    onOpenApp = actionStartActivity<MainActivity>(),
                    onToggleWorship = { actionRunCallback<ToggleWidgetWorshipAction>() },
                )
            }

            onNode(hasTestTag("widget_worship_bar")).assertExists()
            onNode(hasTestTag("widget_worship_toggle_button")).assertHasClickAction()
            onNode(hasTestTag("widget_worship_open_app")).assertHasClickAction()
            onNode(hasText("Ashar 15:15 • 1j 15m lagi")).assertExists()
        }
}
