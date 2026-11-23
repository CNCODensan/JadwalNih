package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.ImageStorageHelper
import com.example.data.deserializeSubTasks
import com.example.data.serializeSubTasks
import com.example.domain.AiBatchScheduleParser
import com.example.domain.AiConflictResolution
import com.example.domain.AttendanceStatus
import com.example.domain.DailyTimelineEntry
import com.example.domain.DurationCategory
import com.example.domain.ForeshadowOccurrence
import com.example.domain.GreetingState
import com.example.domain.HolidayThemeType
import com.example.domain.MainTab
import com.example.domain.NotificationCategory
import com.example.domain.NotificationLogEntry
import com.example.domain.PhotoRecap
import com.example.domain.PhotoTextAlignment
import com.example.domain.PhotoTextStyle
import com.example.domain.PrayerTimesService
import com.example.domain.PriorityQuadrant
import com.example.domain.PublicHolidayService
import com.example.domain.QuickGlanceSummary
import com.example.domain.QuickGlanceTarget
import com.example.domain.RecurrenceRule
import com.example.domain.ReligionOption
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.SubTaskItem
import com.example.domain.TaskDraft
import com.example.domain.TaskIconCatalog
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.domain.ThemeMode
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.PostCapturePreviewSheet
import com.example.ui.components.RecapCalendarSheet
import com.example.ui.components.RingkasanCepatCard
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TaskEditorSheet
import com.example.ui.screens.AnalitikScreen
import com.example.ui.screens.JadwalScreen
import com.example.ui.screens.MatriksScreen
import com.example.ui.screens.OnboardingStartingScreen
import com.example.ui.theme.JadwalNihTheme
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
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
                    summary = QuickGlanceSummary(todayTotalCount = 3, todayCompletedCount = 1, nextTaskTitle = "Belajar Rutin"),
                    greeting = GreetingState("Pagi, Pengguna!", "Lanjut yuk!"),
                    tapTarget = QuickGlanceTarget.MATRIKS,
                    onCardClick = { clicked = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Pagi, Pengguna!").assertIsDisplayed()
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
        composeRule.onNodeWithTag("editor_mode_manual").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Durasi (menit)").assertDoesNotExist()
        composeRule.onNodeWithTag("pick_manual_date_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("reminder_section_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("priority_square_K4").performScrollTo().performClick()

        composeRule.onNodeWithTag("task_title_input").performScrollTo().performTextReplacement("Baca Buku")
        composeRule.onNodeWithTag("new_subtask_input").performScrollTo().performTextReplacement("Bab 1")
        composeRule.onNodeWithTag("add_subtask_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("subtask_todo_mode_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_task_button").performClick()

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
        var themeUpdated: ThemeMode? = null
        var targetUpdated: QuickGlanceTarget? = null

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                SettingsSheet(
                    userName = "",
                    themeMode = ThemeMode.SYSTEM,
                    systemInDarkTheme = false,
                    quickGlanceTarget = QuickGlanceTarget.ANALITIK,
                    checkInNotificationsEnabled = true,
                    isBusyBackup = false,
                    onUserNameChange = { nameUpdated = it },
                    onThemeModeChange = { themeUpdated = it },
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
        composeRule.onNodeWithText("Tulis nama panggilanmu (opsional)").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_name_input").performScrollTo().performTextReplacement("Pengguna")
        composeRule.onNodeWithTag("settings_theme_dark").performScrollTo().performClick()
        assertEquals(ThemeMode.DARK, themeUpdated)
        composeRule.onNodeWithTag("settings_theme_light").performScrollTo().performClick()
        assertEquals(ThemeMode.LIGHT, themeUpdated)
        composeRule.onNodeWithTag("settings_theme_system").performScrollTo().performClick()
        assertEquals(ThemeMode.SYSTEM, themeUpdated)
        composeRule.onNodeWithTag("settings_glance_target_matriks").performScrollTo().performClick()

        assertEquals("Pengguna", nameUpdated)
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
    fun `splash motion overlay displays 4 second loading progress and finishes on animation end`() {
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
            composeRule.onNodeWithTag("splash_progress_bar").assertIsDisplayed()
            composeRule.onNodeWithTag("splash_status_text").assertIsDisplayed()
            composeRule.onNodeWithText("JadwalNih").assertIsDisplayed()
            assertFalse("Splash should still be playing at 450ms", finished)

            composeRule.mainClock.advanceTimeBy(2050L) // total ~2500ms (< 4.2s)
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Atur Hari Lebih Tenang").assertIsDisplayed()
            assertFalse("Splash (~4.2s) should still be playing at 2500ms", finished)

            composeRule.mainClock.autoAdvance = true
            composeRule.mainClock.advanceTimeBy(2200L) // total ~4700ms
            composeRule.waitForIdle()
            assertTrue("Splash should invoke onFinished after 4+ second animation completes", finished)
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
        composeRule.onNodeWithTag("editor_mode_manual").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("priority_square_K1").performScrollTo().performClick()
        composeRule.onNodeWithTag("task_title_input").performScrollTo().performTextReplacement("Review Desain")
        composeRule.onNodeWithTag("subtask_todo_mode_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_task_button").performClick()

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

    @Test
    fun `tugasnih backup inspection correctly parses clean and filled state`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val imageHelper = ImageStorageHelper(context)
        val backupManager = com.example.data.JsonBackupManager(
            context = context,
            taskDao = db.scheduleTaskDao(),
            focusDao = db.focusSessionDao(),
            recapDao = db.photoRecapDao(),
            imageHelper = imageHelper,
        )

        // 1. Clean state test
        val cleanFile = File(context.cacheDir, "clean_tugasnih.json")
        cleanFile.writeText(
            """{"format":"tugasnih-backup","version":14,"createdAt":"2026-11-22T09:14:18.132807","tables":{"attendance":[],"spendings":[],"saving_goals":[],"saving_entries":[],"decks":[],"cards":[],"notes":[],"events":[],"cash_payments":[],"exams":[],"grades":[],"subjects":[],"lessons":[],"days":[],"tasks":[],"settings":[{"key":"user_name","value":"Devan"},{"key":"onboarded","value":"1"}]},"photos":{}}""",
        )
        val cleanPreview = backupManager.inspectBackup(Uri.fromFile(cleanFile)).getOrThrow()
        assertEquals("TugasNih", cleanPreview.appName)
        assertEquals(0, cleanPreview.taskCount)
        assertEquals(0, cleanPreview.lessonCount)
        assertEquals("Devan", cleanPreview.suggestedUserName)

        // 2. Filled state test
        val filledFile = File(context.cacheDir, "filled_tugasnih.json")
        filledFile.writeText(
            """{"format":"tugasnih-backup","version":14,"createdAt":"2026-11-22T09:12:46.590100","tables":{"attendance":[],"subjects":[],"lessons":[{"id":1,"weekday":5,"subject":"PJOK","start_time":"08:00","end_time":"09:20","books":"","is_break":0},{"id":4,"weekday":5,"subject":"Istirahat","start_time":"10:00","end_time":"10:20","books":"","is_break":1}],"days":[],"tasks":[{"id":1,"kind":"pr","title":"Statistika","subject":"Mathematics","due_date":"2026-11-30","checklist":"[{\"t\":\"Buka Canva\",\"d\":false}]","members":"","photos":"","done":0,"note":"Lanjutkan lewat AI"}],"settings":[{"key":"user_name","value":"Depan"}]},"photos":{}}""",
        )
        val filledPreview = backupManager.inspectBackup(Uri.fromFile(filledFile)).getOrThrow()
        assertEquals("TugasNih", filledPreview.appName)
        assertEquals(1, filledPreview.taskCount)
        assertEquals(2, filledPreview.lessonCount)
        assertEquals("Depan", filledPreview.suggestedUserName)
    }

    @Test
    fun `tugasnih import correctly maps tasks, subtasks checklist, and weekly lessons`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val taskDao = db.scheduleTaskDao()
        val imageHelper = ImageStorageHelper(context)
        val backupManager = com.example.data.JsonBackupManager(
            context = context,
            taskDao = taskDao,
            focusDao = db.focusSessionDao(),
            recapDao = db.photoRecapDao(),
            imageHelper = imageHelper,
        )

        val filledFile = File(context.cacheDir, "filled_import_test.json")
        filledFile.writeText(
            """{"format":"tugasnih-backup","version":14,"createdAt":"2026-11-22T09:12:46.590100","tables":{"attendance":[],"subjects":[],"lessons":[{"id":1,"weekday":1,"subject":"PAQ","start_time":"08:40","end_time":"10:00","books":"","is_break":0},{"id":2,"weekday":1,"subject":"Istirahat","start_time":"10:00","end_time":"10:20","books":"","is_break":1}],"days":[],"tasks":[{"id":1,"kind":"pr","title":"Statistika","subject":"Mathematics","due_date":"2026-11-30","checklist":"[{\"t\":\"Buka Canva\",\"d\":false},{\"t\":\"Isi data\",\"d\":true}]","members":"","photos":"","done":0,"note":""}],"settings":[{"key":"user_name","value":"Depan"}]},"photos":{}}""",
        )

        // Test Replace Mode
        val replaceRes = backupManager.executeImport(Uri.fromFile(filledFile), com.example.data.JsonBackupManager.ImportStrategy.REPLACE).getOrThrow()
        assertEquals("Depan", replaceRes.importedUserName)

        val tasksInDb = taskDao.getAllTasksOnce()
        assertEquals(3, tasksInDb.size) // 2 lessons + 1 task

        val taskItem = tasksInDb.first { it.title.startsWith("Statistika") }
        assertEquals("2026-11-30", taskItem.dateIso)
        assertTrue("Subtasks should contain Buka Canva", taskItem.subtasksJson.contains("Buka Canva"))
        assertTrue("Subtasks should be in Todo mode", taskItem.isSubtasksTodoMode)
        assertEquals(com.example.domain.RecurrenceRule.NONE.name, taskItem.recurrenceName)

        val lessonItem = tasksInDb.first { it.title == "PAQ" }
        assertEquals(com.example.domain.RecurrenceRule.WEEKLY.name, lessonItem.recurrenceName)
        assertEquals(8 * 60 + 40, lessonItem.startMinuteOfDay)
        assertEquals(10 * 60, lessonItem.endMinuteOfDay)

        val breakItem = tasksInDb.first { it.title == "Istirahat" }
        assertEquals(com.example.domain.PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT.name, breakItem.quadrantName)
        assertEquals(com.example.domain.RecurrenceRule.WEEKLY.name, breakItem.recurrenceName)

        // Test Merge Mode
        val mergeFile = File(context.cacheDir, "merge_import_test.json")
        mergeFile.writeText(
            """{"format":"tugasnih-backup","version":14,"tables":{"lessons":[],"tasks":[{"id":2,"title":"Fisika","due_date":"2026-12-05","checklist":"[]","done":0}],"settings":[]}}""",
        )
        backupManager.executeImport(Uri.fromFile(mergeFile), com.example.data.JsonBackupManager.ImportStrategy.MERGE).getOrThrow()

        val tasksAfterMerge = taskDao.getAllTasksOnce()
        assertEquals(4, tasksAfterMerge.size) // 3 existing + 1 merged
    }

    @Test
    fun `ai batch schedule parser injects today date, extracts notes checklist reminder, and flags conflicts`() {
        val prompt = AiBatchScheduleParser.buildAiPrompt("2026-11-22")
        assertTrue(prompt.contains("tanggal hari ini adalah: 2026-11-22"))
        assertTrue(prompt.contains("- K1: Penting, Mendesak"))
        assertTrue(prompt.contains("- K4: Ga penting/Biasa, Kapan aja"))

        val existing = ScheduleTask(
            id = 50L,
            title = "Upacara Pagi",
            dateIso = "2026-11-22",
            startMinuteOfDay = 7 * 60,
            endMinuteOfDay = 8 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )

        val aiResponse = """
            Berikut jadwal yang sudah aku susun untukmu:
            ```json
            [
              {
                "title": "Belajar Matematika",
                "date": "2026-11-22",
                "startTime": "07:30",
                "endTime": "09:00",
                "priority": "K1",
                "reminderMinutes": 15,
                "notes": "Bawa kalkulator & buku rumus",
                "checklist": ["Baca bab aljabar", "Kerjakan 5 soal"]
              },
              {
                "title": "Olahraga Sore",
                "date": "2026-11-23",
                "startTime": "16:30",
                "priority": "K4",
                "reminderMinutes": 0
              }
            ]
            ```
            Semoga harimu lancar!
        """.trimIndent()

        val result = AiBatchScheduleParser.parse(
            rawText = aiResponse,
            defaultDateIso = "2026-11-22",
            existingTasks = listOf(existing),
        )

        assertEquals(null, result.errorMessage)
        assertEquals(2, result.tasks.size)

        val math = result.tasks[0]
        assertEquals("Belajar Matematika", math.title)
        assertEquals("2026-11-22", math.dateIso)
        assertEquals(7 * 60 + 30, math.startMinuteOfDay)
        assertEquals(9 * 60, math.endMinuteOfDay)
        assertEquals(PriorityQuadrant.Q1_IMPORTANT_URGENT, math.quadrant)
        assertTrue(math.reminderEnabled)
        assertEquals(15, math.reminderLeadMinutes)
        assertEquals("Bawa kalkulator & buku rumus", math.notes)
        assertEquals(2, math.subtasks.size)
        assertTrue(math.isSubtasksTodoMode)

        val sport = result.tasks[1]
        assertEquals("Olahraga Sore", sport.title)
        assertEquals(16 * 60 + 30, sport.startMinuteOfDay)
        assertEquals(17 * 60 + 30, sport.endMinuteOfDay) // default +60 min
        assertEquals(PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT, sport.quadrant)
        assertTrue(sport.reminderEnabled)
        assertEquals(0, sport.reminderLeadMinutes)

        assertTrue("Expected conflict with Upacara Pagi", result.conflictMessages.any { it.contains("Upacara Pagi") })
    }

    @Test
    fun `task editor opens on otomatis tab by default, generates batch tasks, and saves draft on close`() {
        var batchSaved: List<ScheduleTask> = emptyList()
        var dismissedDraft: TaskDraft? = null

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                TaskEditorSheet(
                    initialTask = null,
                    defaultDateIso = "2026-11-22",
                    onSaveDraftAndDismiss = { dismissedDraft = it },
                    onDiscardAndDismiss = {},
                    onSave = {},
                    onSaveBatch = { tasks, _ -> batchSaved = tasks },
                    onNotifyPermissionDenied = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ai_prompt_card").assertIsDisplayed()
        composeRule.onNodeWithTag("copy_ai_prompt_button").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_ai_schedule_button").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_editor_button").assertIsDisplayed()

        // Enter text and close -> triggers draft recovery save
        composeRule.onNodeWithTag("ai_paste_input").performScrollTo().performTextReplacement(
            """[{"title":"Rapat OSIS","date":"2026-11-22","startTime":"10:00","endTime":"11:00","priority":"K2","reminderMinutes":10,"notes":"Ruang aula"}]""",
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_batch_preview_badge").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("cancel_editor_button").performClick()
        assertNotNull(dismissedDraft)
        assertTrue(dismissedDraft?.isOtomatisMode == true)

        // Tap generate AI schedule button
        composeRule.onNodeWithTag("generate_ai_schedule_button").performClick()
        assertEquals(1, batchSaved.size)
        assertEquals("Rapat OSIS", batchSaved.first().title)
        assertEquals("Ruang aula", batchSaved.first().notes)
    }

    @Test
    fun `jadwal screen hides matriks and analitik in simple mode and shows yesterday reschedule and draft banner`() {
        val yesterdayTask = TaskWithStatus(
            task = ScheduleTask(
                id = 77L,
                title = "PR IPA",
                dateIso = "2026-11-21",
                startMinuteOfDay = 15 * 60,
                endMinuteOfDay = 16 * 60,
                quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
            ),
            status = TaskStatus.OVERDUE_DARK,
        )
        var rescheduledIds: Set<Long> = emptySet()
        var skippedIds: Set<Long> = emptySet()
        var resumedDraft: TaskDraft? = null

        val state = JadwalNihUiState(
            isSimpleMode = true,
            yesterdayMissedTasks = listOf(yesterdayTask),
            taskDraft = TaskDraft(title = "Belajar Bahasa Inggris"),
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                JadwalScreen(
                    state = state,
                    onSelectScope = {},
                    onSelectDate = {},
                    onStepDate = {},
                    onJumpToToday = {},
                    onSelectStatusFilter = {},
                    onToggleComplete = {},
                    onToggleSubTask = { _, _ -> },
                    onMoveToTomorrow = {},
                    onExportCalendar = {},
                    onEditTask = {},
                    onDeleteTask = {},
                    onQuickGlanceClick = {},
                    onResumeDraft = { resumedDraft = it },
                    onDiscardDraft = {},
                    onConfirmAttendance = { _, _ -> },
                    onOverrideAttended = {},
                    onRescheduleYesterday = { rescheduledIds = it },
                    onSkipYesterday = { skippedIds = it },
                    onOpenFocusTab = {},
                    onOpenRecapTab = {},
                    onOpenMatrixTab = {},
                    onOpenAnalyticsTab = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Simple mode hides Matriks & Analitik bento cards and tap hint
        composeRule.onNodeWithTag("bento_fokus").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bento_rekap").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bento_matriks").assertDoesNotExist()
        composeRule.onNodeWithTag("bento_analitik").assertDoesNotExist()
        composeRule.onNodeWithText("Mode Simpel • Ringkas & Nyaman").assertIsDisplayed()

        // Draft banner shows exact prompt + Lanjut Edit / Hapus Draf
        composeRule.onNodeWithTag("draft_recovery_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Draf jadwal belum disimpan. Mau lanjut edit atau hapus draf?").assertIsDisplayed()
        composeRule.onNodeWithTag("resume_draft_button").performClick()
        assertEquals("Belajar Bahasa Inggris", resumedDraft?.title)

        // Yesterday reschedule card with item row + Lewati + move button
        composeRule.onNodeWithTag("yesterday_reschedule_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("yesterday_reschedule_item_77").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("yesterday_reschedule_skip_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("yesterday_reschedule_button").performScrollTo().performClick()
        assertEquals(setOf(77L), rescheduledIds)
        assertTrue(skippedIds.isEmpty())
    }

    @Test
    fun `floating pill nav bar hides matriks and analitik in simple mode`() {
        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                FloatingPillNavBar(
                    selectedTab = MainTab.JADWAL,
                    onSelectTab = {},
                    tabIconProvider = { _, _ -> Icons.Filled.Timer },
                    visibleTabs = listOf(MainTab.JADWAL, MainTab.FOKUS, MainTab.REKAP),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_jadwal").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_fokus").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_rekap").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_matriks").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_analitik").assertDoesNotExist()
    }

    @Test
    fun `onboarding starting screen collects user name, simple mode, and dark mode`() {
        var completedName = ""
        var completedSimple = false
        var completedTheme = ThemeMode.SYSTEM

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                OnboardingStartingScreen(
                    initialThemeMode = ThemeMode.SYSTEM,
                    onPreviewThemeMode = {},
                    onComplete = { name, simple, themeMode ->
                        completedName = name
                        completedSimple = simple
                        completedTheme = themeMode
                    },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tulis nama panggilanmu (opsional)").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_name_input").performScrollTo().performTextReplacement("Pengguna Baru")
        composeRule.onNodeWithTag("onboarding_mode_simpel").performScrollTo().performClick()
        composeRule.onNodeWithTag("onboarding_theme_system").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_theme_light").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_theme_dark").performScrollTo().performClick()
        composeRule.onNodeWithTag("onboarding_start_button").performScrollTo().performClick()

        assertEquals("Pengguna Baru", completedName)
        assertTrue(completedSimple)
        assertEquals(ThemeMode.DARK, completedTheme)
    }

    @Test
    fun `status evaluator computes duration categories and live ongoing progress`() {
        assertEquals(DurationCategory.SHORT, StatusEvaluator.evaluateDurationCategory(25))
        assertEquals(DurationCategory.MEDIUM, StatusEvaluator.evaluateDurationCategory(60))
        assertEquals(DurationCategory.LONG, StatusEvaluator.evaluateDurationCategory(120))

        val ongoingTask = ScheduleTask(
            id = 201L,
            title = "Belajar Fisika",
            dateIso = "2026-11-22",
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 10 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )
        val midProgress = StatusEvaluator.evaluateDurationProgress(
            task = ongoingTask,
            todayIso = "2026-11-22",
            nowMinuteOfDay = 9 * 60 + 30,
        )
        assertTrue(midProgress.isOngoing)
        assertEquals(DurationCategory.MEDIUM, midProgress.category)
        assertEquals(60, midProgress.totalMinutes)
        assertEquals(30, midProgress.remainingMinutes)
        assertEquals(0.5f, midProgress.elapsedFraction, 0.01f)
        assertEquals("Berlangsung • Sisa 30 mnt", midProgress.statusLabel)

        val completedTask = ongoingTask.copy(isCompleted = true)
        val completedProgress = StatusEvaluator.evaluateDurationProgress(
            task = completedTask,
            todayIso = "2026-11-22",
            nowMinuteOfDay = 9 * 60 + 30,
        )
        assertFalse(completedProgress.isOngoing)

        val futureTask = ongoingTask.copy(dateIso = "2026-11-23")
        val futureProgress = StatusEvaluator.evaluateDurationProgress(
            task = futureTask,
            todayIso = "2026-11-22",
            nowMinuteOfDay = 9 * 60 + 30,
        )
        assertFalse(futureProgress.isOngoing)
    }

    @Test
    fun `smart reschedule card supports toggling individual yesterday tasks, moving selected, and skipping`() {
        val yesterday1 = TaskWithStatus(
            task = ScheduleTask(
                id = 81L,
                title = "Ringkas Bab 3",
                dateIso = "2026-11-21",
                startMinuteOfDay = 14 * 60,
                endMinuteOfDay = 15 * 60,
                quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
            ),
            status = TaskStatus.OVERDUE_DARK,
        )
        val yesterday2 = TaskWithStatus(
            task = ScheduleTask(
                id = 82L,
                title = "Latihan Soal Kimia",
                dateIso = "2026-11-21",
                startMinuteOfDay = 16 * 60,
                endMinuteOfDay = 17 * 60,
                quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
            ),
            status = TaskStatus.OVERDUE_DARK,
        )

        var movedIds: Set<Long> = emptySet()
        var skippedIds: Set<Long> = emptySet()
        val state = JadwalNihUiState(
            currentDateIso = "2026-11-22",
            selectedDateIso = "2026-11-22",
            yesterdayMissedTasks = listOf(yesterday1, yesterday2),
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                JadwalScreen(
                    state = state,
                    onSelectScope = {},
                    onSelectDate = {},
                    onStepDate = {},
                    onJumpToToday = {},
                    onSelectStatusFilter = {},
                    onToggleComplete = {},
                    onToggleSubTask = { _, _ -> },
                    onMoveToTomorrow = {},
                    onExportCalendar = {},
                    onEditTask = {},
                    onDeleteTask = {},
                    onQuickGlanceClick = {},
                    onResumeDraft = {},
                    onDiscardDraft = {},
                    onConfirmAttendance = { _, _ -> },
                    onOverrideAttended = {},
                    onRescheduleYesterday = { movedIds = it },
                    onSkipYesterday = { skippedIds = it },
                    onOpenFocusTab = {},
                    onOpenRecapTab = {},
                    onOpenMatrixTab = {},
                    onOpenAnalyticsTab = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Both checked initially; uncheck first item (81) and move only 82
        composeRule.onNodeWithTag("yesterday_reschedule_select_all").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("yesterday_reschedule_item_81").performScrollTo().performClick()
        composeRule.onNodeWithTag("yesterday_reschedule_button").performScrollTo().performClick()
        assertEquals(setOf(82L), movedIds)

        // Clicking Lewati emits all yesterday IDs
        composeRule.onNodeWithTag("yesterday_reschedule_skip_button").performScrollTo().performClick()
        assertEquals(setOf(81L, 82L), skippedIds)
    }

    @Test
    fun `schedule task card wraps neatly on narrow 320dp screen and renders live duration bar`() {
        val ongoingTask = ScheduleTask(
            id = 301L,
            title = "Presentasi Proyek Sains Terpadu di Kelas",
            dateIso = "2026-11-22",
            startMinuteOfDay = 10 * 60,
            endMinuteOfDay = 12 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                Box(modifier = Modifier.width(320.dp)) {
                    com.example.ui.components.ScheduleTaskCard(
                        item = TaskWithStatus(ongoingTask, TaskStatus.CRITICAL_RED),
                        onToggleComplete = {},
                        onMoveToTomorrow = {},
                        onExportCalendar = {},
                        onEdit = {},
                        onDelete = {},
                        todayIso = "2026-11-22",
                        nowMinuteOfDay = 10 * 60 + 45,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("task_item_card_301").assertIsDisplayed()
        composeRule.onNodeWithTag("task_duration_badge_301").assertIsDisplayed()
        composeRule.onNodeWithTag("task_live_duration_bar_301").assertIsDisplayed()
        composeRule.onNodeWithTag("task_move_tomorrow_301").assertIsDisplayed()
        composeRule.onNodeWithTag("task_export_calendar_301").assertIsDisplayed()
    }

    @Test
    fun `user preferences store defaults to system theme and persists theme mode choices`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.data.UserPreferencesStore(context)
        store.resetPreferences()

        val initial = store.preferencesFlow.first()
        assertEquals(ThemeMode.SYSTEM, initial.themeMode)

        store.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, store.preferencesFlow.first().themeMode)

        store.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.preferencesFlow.first().themeMode)

        store.completeOnboarding("Tester", isSimpleMode = true, themeMode = ThemeMode.SYSTEM)
        val afterOnboarding = store.preferencesFlow.first()
        assertEquals(ThemeMode.SYSTEM, afterOnboarding.themeMode)
        assertTrue(afterOnboarding.isSimpleMode)
        assertTrue(afterOnboarding.hasCompletedOnboarding)
    }

    @Test
    fun `task icon catalog provides 120+ icons and auto detects Indonesian schedule keywords`() {
        assertTrue("Expected >= 120 icons, got ${TaskIconCatalog.totalCount}", TaskIconCatalog.totalCount >= 120)
        assertEquals("mosque", TaskIconCatalog.autoDetectIconKey("Sholat Maghrib berjamaah"))
        assertEquals("school", TaskIconCatalog.autoDetectIconKey("Kuliah Algoritma di Kampus"))
        assertEquals("run", TaskIconCatalog.autoDetectIconKey("Olahraga lari pagi"))
        assertEquals("fitness", TaskIconCatalog.autoDetectIconKey("Latihan gym sore"))
        assertEquals("restaurant", TaskIconCatalog.autoDetectIconKey("Makan siang bareng tim"))
    }

    @Test
    fun `ai batch parser supports multi-day wrapper objects, icons, locations, and detailed conflicts`() {
        val existing = ScheduleTask(
            id = 90L,
            title = "Standup Harian",
            dateIso = "2026-11-23",
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 10 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )
        val multiScheduleJson = """
            {
              "schedules": [
                {
                  "title": "Rapat Desain",
                  "date": "2026-11-23",
                  "startTime": "09:30",
                  "endTime": "10:30",
                  "priority": "K1",
                  "icon": "meeting_room",
                  "location": "Ruang Kreatif"
                },
                {
                  "title": "Olahraga Sore",
                  "date": "2026-11-24",
                  "startTime": "17:00",
                  "endTime": "18:00",
                  "priority": "K2"
                },
                {
                  "title": "Belanja Mingguan",
                  "date": "2026-11-25",
                  "startTime": "19:00",
                  "endTime": "20:00",
                  "priority": "K4"
                }
              ]
            }
        """.trimIndent()

        val result = AiBatchScheduleParser.parse(
            rawText = multiScheduleJson,
            defaultDateIso = "2026-11-23",
            existingTasks = listOf(existing),
        )
        assertEquals(null, result.errorMessage)
        assertEquals(3, result.tasks.size)
        assertEquals("2026-11-23", result.tasks[0].dateIso)
        assertEquals("2026-11-24", result.tasks[1].dateIso)
        assertEquals("2026-11-25", result.tasks[2].dateIso)
        assertEquals("meeting_room", result.tasks[0].iconKey)
        assertEquals("Ruang Kreatif", result.tasks[0].locationName)
        assertEquals(1, result.detailedConflicts.size)
        assertEquals("Standup Harian", result.detailedConflicts[0].conflictingExistingTasks.first().title)
    }

    @Test
    fun `public holiday service maps holiday themes and prayer service generates multi-religion slots`() = runTest {
        assertEquals(HolidayThemeType.INDEPENDENCE_DAY, PublicHolidayService.mapHolidayNameToTheme("Hari Kemerdekaan RI ke-81"))
        assertEquals(HolidayThemeType.IDUL_FITRI, PublicHolidayService.mapHolidayNameToTheme("Hari Raya Idul Fitri 1447 H"))
        assertEquals(HolidayThemeType.IMLEK, PublicHolidayService.mapHolidayNameToTheme("Tahun Baru Imlek 2577"))
        assertEquals(HolidayThemeType.NATAL, PublicHolidayService.mapHolidayNameToTheme("Hari Raya Natal"))

        val islamSlots = PrayerTimesService.getCachedOrOfflineWorshipSchedule(
            dateIso = "2026-11-23",
            religion = ReligionOption.ISLAM,
            latitude = -6.2088,
            longitude = 106.8456,
        )
        assertEquals(5, islamSlots.size)
        assertEquals(listOf("subuh", "dzuhur", "ashar", "maghrib", "isya"), islamSlots.map { it.key })

        val kristenSlots = PrayerTimesService.getDailyWorshipSchedule(
            dateIso = "2026-11-22", // Sunday
            religion = ReligionOption.KRISTEN,
            latitude = null,
            longitude = null,
        )
        assertTrue(kristenSlots.any { it.isWeeklySpecial })
    }

    @Test
    fun `status evaluator generates 4 weeks of weekly foreshadows and respects skipped dates`() {
        val weeklyTask = ScheduleTask(
            id = 55L,
            title = "Evaluasi Mingguan",
            dateIso = "2026-11-23",
            startMinuteOfDay = 10 * 60,
            endMinuteOfDay = 11 * 60,
            quadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
            recurrence = RecurrenceRule.WEEKLY,
            skippedOccurrenceDates = setOf("2026-12-07"),
        )
        val foreshadows = StatusEvaluator.buildForeshadowOccurrences(
            tasks = listOf(weeklyTask),
            todayIso = "2026-11-23",
            horizonDays = 28,
        )
        assertEquals(listOf("2026-11-30", "2026-12-14", "2026-12-21"), foreshadows.map { it.occurrenceDateIso })
    }

    @Test
    fun `schedule task card requires delete confirmation dialog before invoking delete callback`() {
        var deleted = false
        val task = ScheduleTask(
            id = 404L,
            title = "Rapat Tim",
            dateIso = "2026-11-23",
            startMinuteOfDay = 13 * 60,
            endMinuteOfDay = 14 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                com.example.ui.components.ScheduleTaskCard(
                    item = TaskWithStatus(task, TaskStatus.PENDING_GRAY),
                    onToggleComplete = {},
                    onMoveToTomorrow = {},
                    onExportCalendar = {},
                    onEdit = {},
                    onDelete = { deleted = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("task_delete_404").performClick()
        composeRule.waitForIdle()
        assertFalse("Delete callback should NOT fire before confirmation", deleted)
        composeRule.onNodeWithTag("confirm_delete_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_button").performClick()
        composeRule.waitForIdle()
        assertTrue("Delete callback should fire after confirming dialog", deleted)
    }

    @Test
    fun `notification center filters logs, keeps drafts resumable, and confirms before delete`() {
        var resumedTitle = ""
        var deletedId = -1L
        val draftJson = com.example.data.UserPreferencesStore.serializeDraft(
            TaskDraft(title = "Draf Rapat Penting", dateIso = "2026-11-23"),
        )
        val logs = listOf(
            NotificationLogEntry(
                id = 1L,
                category = NotificationCategory.DRAFT,
                title = "Draf: Draf Rapat Penting",
                message = "Ketuk untuk lanjut edit",
                draftJson = draftJson,
            ),
            NotificationLogEntry(
                id = 2L,
                category = NotificationCategory.WORSHIP,
                title = "Waktu Dzuhur",
                message = "11:45 WIB",
            ),
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                NotificationCenterSheet(
                    logs = logs,
                    onResumeDraft = { resumedTitle = it.title },
                    onDeleteLog = { deletedId = it.id },
                    onClearAllLogs = {},
                    onMarkAllRead = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("notification_center_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("notif_filter_DRAFT").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_resume_draft_1").performClick()
        assertEquals("Draf Rapat Penting", resumedTitle)

        composeRule.onNodeWithTag("notif_filter_ALL").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("delete_notification_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_delete_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_button").performClick()
        composeRule.waitForIdle()
        assertEquals(2L, deletedId)
    }

    @Test
    fun `ai conflict resolution dialog appears on overlapping batch save and invokes chosen resolution`() {
        var resolvedStrategy: AiConflictResolution? = null
        val existing = ScheduleTask(
            id = 77L,
            title = "Kelas Pagi",
            dateIso = "2026-11-23",
            startMinuteOfDay = 8 * 60,
            endMinuteOfDay = 9 * 60,
            quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                TaskEditorSheet(
                    initialTask = null,
                    defaultDateIso = "2026-11-23",
                    existingTasks = listOf(existing),
                    onSaveDraftAndDismiss = {},
                    onDiscardAndDismiss = {},
                    onSave = {},
                    onSaveBatchWithResolution = { _, _, resolution ->
                        resolvedStrategy = resolution
                    },
                    onNotifyPermissionDenied = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ai_paste_input").performScrollTo().performTextReplacement(
            """[{"title":"Belajar Bersama","date":"2026-11-23","startTime":"08:30","endTime":"09:30","priority":"K2"}]""",
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_preview_title_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("generate_ai_schedule_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ai_conflict_resolution_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("conflict_overwrite_button").performClick()
        composeRule.waitForIdle()
        assertEquals(AiConflictResolution.OVERWRITE_EXISTING, resolvedStrategy)
    }
}
