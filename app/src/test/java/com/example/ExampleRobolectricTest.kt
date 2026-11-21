package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.ImageStorageHelper
import com.example.data.deserializeSubTasks
import com.example.data.serializeSubTasks
import com.example.domain.AttendanceStatus
import com.example.domain.DailyTimelineEntry
import com.example.domain.GreetingState
import com.example.domain.PhotoRecap
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
import com.example.domain.PriorityQuadrant
import com.example.domain.QuickGlanceSummary
import com.example.domain.QuickGlanceTarget
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.SubTaskItem
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.ui.JadwalNihUiState
import com.example.ui.components.PostCapturePreviewSheet
import com.example.ui.components.RecapCalendarSheet
import com.example.ui.components.RingkasanCepatCard
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.AnalitikScreen
import com.example.ui.screens.MatriksScreen
import com.example.ui.theme.JadwalNihTheme
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JadwalNih", appName)
    }

    @Test
    fun `subtask serialization round trips cleanly`() {
        val items = listOf(
            SubTaskItem("a", "Cek email", isDone = true),
            SubTaskItem("b", "Kirim laporan", isDone = false),
        )
        val json = serializeSubTasks(items)
        val decoded = deserializeSubTasks(json)
        assertEquals(items, decoded)
    }

    @Test
    fun `attendance status evaluator handles check-in window, overdue missed, and override`() {
        val baseTask = ScheduleTask(
            id = 10L,
            title = "Rapat Pagi",
            dateIso = "2026-11-21",
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 10 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )

        // Needs check-in prompt inside [start-15m .. end]
        assertTrue(StatusEvaluator.needsCheckInPrompt(baseTask, "2026-11-21", 8 * 60 + 50))
        assertTrue(StatusEvaluator.needsCheckInPrompt(baseTask, "2026-11-21", 9 * 60 + 15))
        assertFalse(StatusEvaluator.needsCheckInPrompt(baseTask, "2026-11-21", 10 * 60 + 5))

        // Unconfirmed past end -> OVERDUE_DARK
        assertEquals(
            TaskStatus.OVERDUE_DARK,
            StatusEvaluator.evaluate(baseTask, "2026-11-21", 10 * 60 + 5),
        )

        // Overridden as attended -> COMPLETED_GREEN
        val overridden = baseTask.copy(attendanceStatus = AttendanceStatus.CONFIRMED_OVERRIDE)
        assertEquals(
            TaskStatus.COMPLETED_GREEN,
            StatusEvaluator.evaluate(overridden, "2026-11-21", 10 * 60 + 5),
        )
    }

    @Test
    fun `hourly timeline generator builds 1-hour empty slots, gap breaks, and task blocks`() {
        val t1 = TaskWithStatus(
            task = ScheduleTask(
                id = 1L,
                title = "PAQ",
                dateIso = "2026-11-21",
                startMinuteOfDay = 8 * 60 + 40,
                endMinuteOfDay = 10 * 60,
                quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
            ),
            status = TaskStatus.PENDING_GRAY,
        )
        val t2 = TaskWithStatus(
            task = ScheduleTask(
                id = 2L,
                title = "Bahasa Indonesia",
                dateIso = "2026-11-21",
                startMinuteOfDay = 10 * 60 + 20,
                endMinuteOfDay = 12 * 60 + 20,
                quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
            ),
            status = TaskStatus.APPROACHING_YELLOW,
        )

        val entries = ScheduleAnalyzer.buildHourlyTimeline(listOf(t1, t2), defaultStartHour = 7, defaultEndHour = 13)
        assertTrue(entries.any { it is DailyTimelineEntry.HourSlotEmpty && it.hour == 7 })
        assertTrue(entries.any { it is DailyTimelineEntry.TaskBlock && it.item.task.id == 1L && it.durationMinutes == 80 })
        assertTrue(entries.any { it is DailyTimelineEntry.GapBreak && it.durationMinutes == 20 })
        assertTrue(entries.any { it is DailyTimelineEntry.TaskBlock && it.item.task.id == 2L && it.durationMinutes == 120 })
    }

    @Test
    fun `image storage helper saves styled photos and exports collage share uri`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = ImageStorageHelper(context)

        val jpgFile = File(context.cacheDir, "test_photo.jpg")
        val bmp = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.BLUE)
        FileOutputStream(jpgFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        bmp.recycle()

        val savedPaths = mutableListOf<String>()
        for (align in PhotoTextAlignment.entries) {
            val styledResult = helper.saveCompressedFromUri(
                uri = Uri.fromFile(jpgFile),
                textStyle = PhotoTextStyle.CAPTION_BOTTOM,
                textAlignment = align,
                overlayTimeLabel = "10:15",
                overlayTitle = "Momen",
            )
            assertTrue("Styled photo ($align) save should succeed", styledResult.isSuccess)
            val savedPath = styledResult.getOrThrow()
            assertNotNull(BitmapFactory.decodeFile(savedPath))
            savedPaths.add(savedPath)
        }

        val collageRes = helper.exportCollageToGalleryAndShareUri(
            dateIso = "2026-11-21",
            title = "Kolase Uji",
            imagePaths = savedPaths,
        )
        assertTrue("Collage export should succeed: ${collageRes.exceptionOrNull()}", collageRes.isSuccess)
        assertNotNull(collageRes.getOrThrow().shareUri)

        helper.clearAllRecapImages()
        jpgFile.delete()
    }

    @Test
    fun `quick glance card is info-only without internal shortcut buttons and routes on tap`() {
        var clicked = false
        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                RingkasanCepatCard(
                    summary = QuickGlanceSummary(todayTotalCount = 3, todayCompletedCount = 1, nextTaskTitle = "Belajar Kotlin"),
                    greeting = GreetingState("Pagi, Devan!", "Lanjut yuk!"),
                    tapTarget = QuickGlanceTarget.MATRIKS,
                    onCardClick = { clicked = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pagi, Devan!").assertIsDisplayed()
        composeRule.onNodeWithText("Ketuk untuk buka Matriks").assertIsDisplayed()
        composeRule.onNodeWithText("Fokus Sekarang").assertDoesNotExist()
        composeRule.onNodeWithTag("quick_glance_card").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `task editor omits manual duration and uses 2x2 priority squares with todo mode`() {
        var savedTask: ScheduleTask? = null
        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                TaskEditorSheet(
                    initialTask = null,
                    defaultDateIso = "2026-11-21",
                    onSaveDraftAndDismiss = {},
                    onDiscardAndDismiss = {},
                    onSave = { savedTask = it },
                    onNotifyPermissionDenied = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Durasi (menit)").assertDoesNotExist()
        composeRule.onNodeWithTag("pick_manual_date_button").assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K1").assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K2").assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K3").assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K4").performScrollTo().performClick()

        composeRule.onNodeWithTag("task_title_input").performScrollTo().performTextReplacement("Baca Buku")
        composeRule.onNodeWithTag("new_subtask_input").performScrollTo().performTextReplacement("Bab 1")
        composeRule.onNodeWithTag("add_subtask_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("subtask_todo_mode_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_task_button").performScrollTo().performClick()

        assertNotNull(savedTask)
        assertEquals("Baca Buku", savedTask?.title)
        assertEquals(PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT, savedTask?.quadrant)
        assertTrue(savedTask?.isSubtasksTodoMode == true)
    }

    @Test
    fun `matrix screen displays quadrant filters, hourly timeline, and synced monthly detail`() {
        val sampleTask = TaskWithStatus(
            task = ScheduleTask(
                id = 5L,
                title = "Kuliah Algoritma",
                dateIso = "2026-11-21",
                startMinuteOfDay = 8 * 60,
                endMinuteOfDay = 9 * 60 + 30,
                quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
            ),
            status = TaskStatus.PENDING_GRAY,
        )
        val state = JadwalNihUiState(
            selectedDateIso = "2026-11-21",
            selectedRecapDateIso = "2026-11-21",
            matrixScope = ScheduleScope.DAILY,
            matrixTasks = listOf(sampleTask),
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                MatriksScreen(
                    state = state,
                    onSelectScope = {},
                    onSelectQuadrantFilter = {},
                    onStepDate = {},
                    onJumpToToday = {},
                    onSelectSharedDate = {},
                    onAddTaskAtHour = { _, _ -> },
                    onOpenRecapForDate = {},
                    onToggleComplete = {},
                    onMoveToTomorrow = {},
                    onChangeQuadrant = { _, _ -> },
                    onEditTask = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("matrix_quad_filter_all").assertIsDisplayed()
        composeRule.onNodeWithTag("matrix_quad_filter_K1").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline_task_block_5").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `post capture preview hides filters and supports left center right overlay alignment`() {
        var savedStyle: PhotoTextStyle? = null
        var savedAlignment: PhotoTextAlignment? = null

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                PostCapturePreviewSheet(
                    imageUri = Uri.parse("file:///nonexistent.jpg"),
                    targetDateIso = "2026-11-21",
                    existingTitle = "",
                    existingCaption = "",
                    timeLabel = "14:20",
                    onDismiss = {},
                    onConfirmSave = { style, align, _, _ ->
                        savedStyle = style
                        savedAlignment = align
                    },
                )
            }
        }

        composeRule.onNodeWithText("Filter Warna").assertDoesNotExist()
        composeRule.onNodeWithText("Judul & Jam").performScrollTo().performClick()
        composeRule.onNodeWithTag("overlay_align_left").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_align_center").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("overlay_align_right").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirm_save_captured_recap_button").performScrollTo().performClick()

        assertEquals(PhotoTextStyle.CAPTION_BOTTOM, savedStyle)
        assertEquals(PhotoTextAlignment.RIGHT, savedAlignment)
    }

    @Test
    fun `recap calendar notebook saves header title, short note, and shows collage export when photos exist`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val photoFile = File(context.cacheDir, "recap_ui_test.jpg")
        val bmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.RED)
        FileOutputStream(photoFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        bmp.recycle()

        var savedTitle = ""
        var savedCaption = ""
        var exportedDate: String? = null

        composeRule.setContent {
            JadwalNihTheme(darkTheme = true) {
                RecapCalendarSheet(
                    selectedDateIso = "2026-11-21",
                    photoRecaps = mapOf(
                        "2026-11-21" to PhotoRecap(
                            dateIso = "2026-11-21",
                            title = "",
                            caption = "",
                            imagePaths = listOf(photoFile.absolutePath),
                            updatedAtEpochMs = 1L,
                        ),
                    ),
                    allTasks = listOf(
                        TaskWithStatus(
                            task = ScheduleTask(
                                id = 99L,
                                title = "Review Harian",
                                dateIso = "2026-11-21",
                                startMinuteOfDay = 17 * 60,
                                endMinuteOfDay = 18 * 60,
                                quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
                            ),
                            status = TaskStatus.PENDING_GRAY,
                        ),
                    ),
                    onSelectDate = {},
                    onJumpToCameraForDate = {},
                    onPickGalleryForDate = {},
                    onPickFileForDate = {},
                    onRemovePhoto = { _, _ -> },
                    onSaveMeta = { _, title, caption ->
                        savedTitle = title
                        savedCaption = caption
                    },
                    onDeleteRecap = {},
                    onExportCollage = { exportedDate = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recap_synced_tasks_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("export_collage_button").performScrollTo().performClick()
        assertEquals("2026-11-21", exportedDate)

        composeRule.onNodeWithTag("recap_title_input").performScrollTo().performTextReplacement("Catatan Sprint")
        composeRule.onNodeWithTag("recap_caption_input").performScrollTo().performTextReplacement("Berhasil bereskan UI rekap.")
        composeRule.onNodeWithTag("save_recap_meta_button").performScrollTo().performClick()

        assertEquals("Catatan Sprint", savedTitle)
        assertEquals("Berhasil bereskan UI rekap.", savedCaption)
        photoFile.delete()
    }

    @Test
    fun `settings sheet updates name, dark mode, quick glance target, and backup actions`() {
        var nameUpdated = ""
        var darkUpdated = false
        var targetUpdated: QuickGlanceTarget? = null

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                SettingsSheet(
                    userName = "",
                    isDarkMode = false,
                    quickGlanceTarget = QuickGlanceTarget.ANALITIK,
                    checkInNotificationsEnabled = true,
                    isBusyBackup = false,
                    onUserNameChange = { nameUpdated = it },
                    onDarkModeChange = { darkUpdated = it },
                    onQuickGlanceTargetChange = { targetUpdated = it },
                    onCheckInNotificationsChange = {},
                    onExportJson = {},
                    onImportJson = {},
                    onResetDataConfirmed = {},
                    onNotifyPermissionDenied = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_name_input").performScrollTo().performTextReplacement("Raka")
        composeRule.onNodeWithTag("settings_theme_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_glance_target_matriks").performScrollTo().performClick()

        assertEquals("Raka", nameUpdated)
        assertTrue(darkUpdated)
        assertEquals(QuickGlanceTarget.MATRIKS, targetUpdated)
    }

    @Test
    fun `dark mode palette provides crisp light text colors across tabs`() {
        var darkOnSurfaceLuminance = 0f
        var darkOnBgLuminance = 0f

        composeRule.setContent {
            JadwalNihTheme(darkTheme = true) {
                val cs = androidx.compose.material3.MaterialTheme.colorScheme
                darkOnSurfaceLuminance =
                    (0.299f * cs.onSurface.red + 0.587f * cs.onSurface.green + 0.114f * cs.onSurface.blue)
                darkOnBgLuminance =
                    (0.299f * cs.onBackground.red + 0.587f * cs.onBackground.green + 0.114f * cs.onBackground.blue)

                com.example.ui.components.AtmosphericBackground {
                    AnalitikScreen(
                        state = JadwalNihUiState(),
                        onSelectScope = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Analitik").assertIsDisplayed()
        composeRule.onNodeWithTag("consistency_heatmap_card").performScrollTo().assertIsDisplayed()
        assertTrue("Dark onSurface should be bright, got $darkOnSurfaceLuminance", darkOnSurfaceLuminance > 0.85f)
        assertTrue("Dark onBackground should be bright, got $darkOnBgLuminance", darkOnBgLuminance > 0.85f)
    }

    @Test
    fun `splash motion overlay displays JadwalNih icon and finishes on tap or animation end`() {
        var finished = false
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.setContent {
                JadwalNihTheme(darkTheme = false) {
                    com.example.ui.components.SplashMotionOverlay(
                        onFinished = { finished = true },
                    )
                }
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeBy(450L)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("splash_motion_overlay").assertIsDisplayed()
            composeRule.onNodeWithTag("splash_app_icon").assertIsDisplayed()
            composeRule.onNodeWithText("JadwalNih").assertIsDisplayed()
            assertFalse("Splash should still be playing at 450ms", finished)

            composeRule.mainClock.autoAdvance = true
            composeRule.mainClock.advanceTimeBy(1200L)
            composeRule.waitForIdle()
            assertTrue("Splash should invoke onFinished after animation completes", finished)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `splash motion overlay finishes immediately when tapped`() {
        var finished = false
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.setContent {
                JadwalNihTheme(darkTheme = false) {
                    com.example.ui.components.SplashMotionOverlay(
                        onFinished = { finished = true },
                    )
                }
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeBy(200L)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("splash_motion_overlay").performClick()
            composeRule.waitForIdle()
            assertTrue("Splash should finish immediately on tap", finished)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun `haptic feedback fires when scheduling a task`() {
        val recordedHaptics = mutableListOf<androidx.compose.ui.hapticfeedback.HapticFeedbackType>()
        val recordingHaptics = object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                recordedHaptics.add(hapticFeedbackType)
            }
        }

        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalHapticFeedback provides recordingHaptics,
            ) {
                JadwalNihTheme(darkTheme = false) {
                    TaskEditorSheet(
                        initialTask = null,
                        defaultDateIso = "2026-11-21",
                        onSaveDraftAndDismiss = {},
                        onDiscardAndDismiss = {},
                        onSave = {},
                        onNotifyPermissionDenied = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("priority_square_K1").performScrollTo().performClick()
        composeRule.onNodeWithTag("task_title_input").performScrollTo().performTextReplacement("Review Desain")
        composeRule.onNodeWithTag("subtask_todo_mode_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_task_button").performScrollTo().performClick()

        assertTrue("Expected haptic feedback when scheduling a task", recordedHaptics.size >= 3)
        assertTrue(
            "Expected LongPress confirm haptic on task save",
            recordedHaptics.contains(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress),
        )
    }

    @Test
    fun `haptic feedback fires when completing checklist item`() {
        val recordedHaptics = mutableListOf<androidx.compose.ui.hapticfeedback.HapticFeedbackType>()
        val recordingHaptics = object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                recordedHaptics.add(hapticFeedbackType)
            }
        }

        val taskWithSub = ScheduleTask(
            id = 42L,
            title = "Siapkan Demo",
            dateIso = "2026-11-21",
            startMinuteOfDay = 10 * 60,
            endMinuteOfDay = 11 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
            subtasks = listOf(SubTaskItem("sub1", "Cek audio", isDone = false)),
            isSubtasksTodoMode = true,
        )

        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalHapticFeedback provides recordingHaptics,
            ) {
                JadwalNihTheme(darkTheme = false) {
                    com.example.ui.components.ScheduleTaskCard(
                        item = TaskWithStatus(taskWithSub, TaskStatus.PENDING_GRAY),
                        onToggleComplete = {},
                        onMoveToTomorrow = {},
                        onExportCalendar = {},
                        onEdit = {},
                        onDelete = {},
                        onToggleSubTask = { _, _ -> },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subtask_row_42_sub1").performClick()

        assertTrue(
            "Expected LongPress confirm haptic when completing checklist item",
            recordedHaptics.contains(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress),
        )
    }

    @Test
    fun `haptic feedback fires when interacting with focus timer`() {
        val recordedHaptics = mutableListOf<androidx.compose.ui.hapticfeedback.HapticFeedbackType>()
        val recordingHaptics = object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                recordedHaptics.add(hapticFeedbackType)
            }
        }

        val timerState = JadwalNihUiState()
        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalHapticFeedback provides recordingHaptics,
            ) {
                JadwalNihTheme(darkTheme = false) {
                    com.example.ui.screens.FokusScreen(
                        state = timerState,
                        onMinutesInputChange = {},
                        onStepMinutes = {},
                        onSelectTask = {},
                        onStart = {},
                        onPause = {},
                        onResume = {},
                        onReset = {},
                        onStopAndSave = {},
                        onToggleDistractionFree = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+5").performClick()
        composeRule.onNodeWithTag("focus_start_button").performClick()
        composeRule.onNodeWithTag("focus_reset_button").performClick()

        assertTrue("Expected at least 3 haptic events during timer interaction", recordedHaptics.size >= 3)
        assertTrue(
            "Expected TextHandleMove tick/soft haptic on timer stepper/reset",
            recordedHaptics.contains(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove),
        )
        assertTrue(
            "Expected LongPress confirm haptic on timer start",
            recordedHaptics.contains(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress),
        )
    }
}
