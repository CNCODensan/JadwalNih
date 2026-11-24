package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ImageStorageHelper
import com.example.data.JsonBackupManager
import com.example.data.UserPreferencesStore
import com.example.domain.DateTimeHelper
import com.example.domain.HolidayInfo
import com.example.domain.HolidayThemeType
import com.example.domain.MainTab
import com.example.domain.PersonalBirthdayStats
import com.example.domain.PhotoRecap
import com.example.domain.PrayerCardVisibilityMode
import com.example.domain.PrayerTimesService
import com.example.domain.PriorityQuadrant
import com.example.domain.PublicHolidayService
import com.example.domain.RecurrenceRule
import com.example.domain.ReligionOption
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.domain.WorshipActiveWindow
import com.example.domain.WorshipScheduleItem
import com.example.notifications.ReminderScheduler
import com.example.ui.JadwalNihUiState
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.WorshipHistorySheet
import com.example.ui.components.WorshipScheduleCard
import com.example.ui.screens.KalenderScreen
import com.example.ui.screens.RekapScreen
import com.example.ui.theme.BirthdayCelebrationOverlay
import com.example.ui.theme.JadwalNihTheme
import com.example.ui.theme.ProvideAdaptiveLayoutSpec
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class FinalPolishFeaturesTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `cascading reminder offsets step down progressively to 5 minutes`() {
        assertEquals(listOf(60, 40, 30, 20, 10, 5), ReminderScheduler.computeCascadingReminderOffsets(60))
        assertEquals(listOf(30, 20, 10, 5), ReminderScheduler.computeCascadingReminderOffsets(30))
        assertEquals(listOf(15, 10, 5), ReminderScheduler.computeCascadingReminderOffsets(15))
        assertEquals(listOf(5), ReminderScheduler.computeCascadingReminderOffsets(5))
        assertTrue(ReminderScheduler.computeCascadingReminderOffsets(0).isEmpty())
    }

    @Test
    fun `prayer natural active window hides Dhuha gap and detects active windows plus 7-day history`() {
        val dateIso = "2026-11-24"
        val slots = listOf(
            WorshipScheduleItem("subuh", "Subuh", "Sholat Subuh", 4 * 60 + 15, dateIso),
            WorshipScheduleItem("dzuhur", "Dzuhur", "Sholat Dzuhur", 11 * 60 + 45, dateIso),
            WorshipScheduleItem("ashar", "Ashar", "Sholat Ashar", 15 * 60 + 10, dateIso),
            WorshipScheduleItem("maghrib", "Maghrib", "Sholat Maghrib", 17 * 60 + 55, dateIso),
            WorshipScheduleItem("isya", "Isya", "Sholat Isya", 19 * 60 + 10, dateIso),
        )

        // During Subuh window (04:30)
        val subuhWin = PrayerTimesService.resolveActiveWorshipWindow(
            items = slots,
            religion = ReligionOption.ISLAM,
            dateIso = dateIso,
            nowMinuteOfDay = 4 * 60 + 30,
        )
        assertTrue(subuhWin.isInActiveWindow)
        assertEquals("subuh", subuhWin.activeItem?.key)

        // Mid-morning Dhuha gap (09:00) -> NOT in active Islamic prayer window
        val gapWin = PrayerTimesService.resolveActiveWorshipWindow(
            items = slots,
            religion = ReligionOption.ISLAM,
            dateIso = dateIso,
            nowMinuteOfDay = 9 * 60,
        )
        assertFalse(gapWin.isInActiveWindow)
        assertEquals(null, gapWin.activeItem)

        // Dzuhur window (12:30) -> active Dzuhur
        val dzuhurWin = PrayerTimesService.resolveActiveWorshipWindow(
            items = slots,
            religion = ReligionOption.ISLAM,
            dateIso = dateIso,
            nowMinuteOfDay = 12 * 60 + 30,
        )
        assertTrue(dzuhurWin.isInActiveWindow)
        assertEquals("dzuhur", dzuhurWin.activeItem?.key)

        // 7-day history lock enforcement
        val lockedHistory = PrayerTimesService.buildWorshipHistoryDays(
            todayIso = dateIso,
            daysBack = 7,
            religion = ReligionOption.ISLAM,
            latitude = null,
            longitude = null,
            completedKeys = setOf("$dateIso|subuh"),
            locationName = "Jakarta",
            allowMissedManualEdit = false,
        )
        assertEquals(8, lockedHistory.size) // today + 7 past days
        assertTrue(lockedHistory.first().isToday)
        assertTrue(lockedHistory.first().isEditable)
        assertTrue(lockedHistory.drop(1).all { !it.isEditable })
    }

    @Test
    fun `public holiday service distinguishes red national holidays and gold observances`() {
        val combined = PublicHolidayService.getCachedOrEmbeddedHolidaysForYear(2026)
        val aug17 = PublicHolidayService.resolveHolidaysForDate("2026-08-17", combined)
        assertTrue(aug17.any { !it.isNationalObservance && it.themeType == HolidayThemeType.INDEPENDENCE_DAY })

        val kartini = PublicHolidayService.resolveHolidaysForDate("2026-04-21", combined)
        assertTrue(kartini.any { it.isNationalObservance && it.name.contains("Kartini", ignoreCase = true) })
    }

    @Test
    fun `yearly birthday recurrence and usage duration formatting work accurately`() {
        assertEquals("2027-11-24", StatusEvaluator.nextOccurrenceDate("2026-11-24", RecurrenceRule.YEARLY))
        assertEquals("2025-02-28", StatusEvaluator.nextOccurrenceDate("2024-02-29", RecurrenceRule.YEARLY))
        assertTrue(DateTimeHelper.isSameMonthAndDay("1999-11-24", "2026-11-24"))
        assertFalse(DateTimeHelper.isSameMonthAndDay("1999-11-25", "2026-11-24"))

        assertEquals("7 hari", DateTimeHelper.formatUsageDuration(7))
        assertEquals("2 bulan 5 hari", DateTimeHelper.formatUsageDuration(65))
        assertEquals("1 tahun 2 bulan", DateTimeHelper.formatUsageDuration(425))
    }

    @Test
    fun `json backup v5 round trips birthday profile and worship visibility preferences`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val prefsStore = UserPreferencesStore(context)
        prefsStore.resetPreferences()
        prefsStore.setBirthDateIso("2001-05-12")
        prefsStore.setPrayerCardVisibilityMode(PrayerCardVisibilityMode.COMPACT_COUNTDOWN)
        prefsStore.setAllowMissedPrayerManualEdit(false)
        prefsStore.setLastLocation("Yogyakarta", -7.7956, 110.3695)

        val backupManager = JsonBackupManager(
            context = context,
            taskDao = db.scheduleTaskDao(),
            focusDao = db.focusSessionDao(),
            recapDao = db.photoRecapDao(),
            imageHelper = ImageStorageHelper(context),
            notificationLogDao = db.notificationLogDao(),
            worshipCheckinDao = db.worshipCheckinDao(),
            prefsStore = prefsStore,
        )

        val backupFile = File(context.cacheDir, "v5_prefs_roundtrip.json")
        backupManager.exportToUri(Uri.fromFile(backupFile)).getOrThrow()

        // Reset preferences to verify restore from JSON
        prefsStore.resetPreferences()
        assertEquals("", prefsStore.preferencesFlow.first().birthDateIso)

        backupManager.executeImport(Uri.fromFile(backupFile), JsonBackupManager.ImportStrategy.REPLACE).getOrThrow()
        val restored = prefsStore.preferencesFlow.first()
        assertEquals("2001-05-12", restored.birthDateIso)
        assertEquals(PrayerCardVisibilityMode.COMPACT_COUNTDOWN, restored.prayerCardVisibilityMode)
        assertFalse(restored.allowMissedPrayerManualEdit)
        assertEquals("Yogyakarta", restored.lastLocationName)

        backupFile.delete()
    }

    @Test
    fun `worship schedule card hides completely outside natural window when ONLY_ACTIVE_WINDOW`() {
        val dateIso = "2026-11-24"
        val items = listOf(
            WorshipScheduleItem("subuh", "Subuh", "Sholat Subuh", 4 * 60 + 15, dateIso),
            WorshipScheduleItem("dzuhur", "Dzuhur", "Sholat Dzuhur", 11 * 60 + 45, dateIso),
        )
        val outOfWindow = WorshipActiveWindow(
            currentItem = null,
            nextItem = items[1],
            nextCountdownLabel = "2j lagi",
            isWithinNaturalWindow = false,
            todayItems = items,
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                WorshipScheduleCard(
                    religion = ReligionOption.ISLAM,
                    items = items,
                    nextItem = items[1],
                    countdownLabel = "2j lagi",
                    locationName = "Jakarta",
                    onToggleComplete = {},
                    visibilityMode = PrayerCardVisibilityMode.ONLY_ACTIVE_WINDOW,
                    activeWindow = outOfWindow,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("worship_schedule_card").assertDoesNotExist()
    }

    @Test
    fun `worship history sheet enforces lock banner when manual missed edit is disabled`() {
        val todayIso = "2026-11-24"
        val history = PrayerTimesService.buildWorshipHistoryDays(
            todayIso = todayIso,
            daysBack = 7,
            religion = ReligionOption.ISLAM,
            latitude = null,
            longitude = null,
            completedKeys = emptySet(),
            locationName = "Jakarta",
            allowMissedManualEdit = false,
        )
        var toggledCount = 0

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                WorshipHistorySheet(
                    religion = ReligionOption.ISLAM,
                    historyDays = history,
                    allowManualMissedEdit = false,
                    onToggleItem = { toggledCount++ },
                    onOpenSettings = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("worship_history_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("worship_history_locked_banner").assertIsDisplayed()

        val pastDateIso = history[1].dateIso
        composeRule.onNodeWithTag("worship_history_item_${pastDateIso}_subuh")
            .performScrollTo()
            .performClick()
        assertEquals(0, toggledCount)
    }

    @Test
    fun `kalender screen displays unified grid, red holidays, gold observances, and filters`() {
        val todayIso = "2026-08-17"
        val holidays = listOf(
            HolidayInfo(
                dateIso = "2026-08-17",
                name = "Hari Kemerdekaan RI",
                isNationalHoliday = true,
                isNationalObservance = false,
                categoryLabel = "Libur Nasional",
                themeType = HolidayThemeType.INDEPENDENCE_DAY,
            ),
            HolidayInfo(
                dateIso = "2026-08-14",
                name = "Hari Pramuka",
                isNationalHoliday = false,
                isNationalObservance = true,
                categoryLabel = "Hari Nasional",
            ),
        )
        val state = JadwalNihUiState(
            currentDateIso = todayIso,
            selectedDateIso = todayIso,
            birthDateIso = "2000-08-17",
            allHolidays = holidays,
            selectedDateHolidays = listOf(holidays[0]),
            allTasksWithStatus = listOf(
                TaskWithStatus(
                    task = ScheduleTask(
                        id = 10L,
                        title = "Upacara Bendera",
                        dateIso = todayIso,
                        startMinuteOfDay = 7 * 60,
                        endMinuteOfDay = 8 * 60,
                        quadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
                    ),
                    status = TaskStatus.PENDING_GRAY,
                ),
            ),
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                ProvideAdaptiveLayoutSpec(availableWidth = 411.dp) {
                    KalenderScreen(
                        state = state,
                        onSelectDate = {},
                        onAddTaskForDate = {},
                        onEditTask = {},
                        onToggleComplete = {},
                        onToggleSubTask = { _, _ -> },
                        onMoveToTomorrow = {},
                        onExportCalendar = {},
                        onDeleteTask = {},
                        onDeleteRecurringSeries = {},
                        onOverrideAttended = {},
                        onOpenRecapForDate = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("kalender_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("kalender_day_2026-08-17").assertIsDisplayed()
        composeRule.onNodeWithTag("kalender_holiday_2026-08-17").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Hari Kemerdekaan RI").assertIsDisplayed()
        composeRule.onNodeWithTag("kalender_filter_holidays").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("kalender_filter_recaps").performClick()
    }

    @Test
    fun `birthday celebration overlay displays user name, stats, and closes on action button`() {
        var dismissed = false
        val stats = PersonalBirthdayStats(
            userName = "Raka",
            daysUsingApp = 45,
            usageDurationLabel = "1 bulan 15 hari",
            totalCompletedTasks = 28,
            totalFocusMinutes = 420,
            totalPhotoRecaps = 9,
            streakDays = 6,
        )

        composeRule.setContent {
            JadwalNihTheme(darkTheme = false, isBirthdayTheme = true) {
                BirthdayCelebrationOverlay(
                    userName = "Raka",
                    stats = stats,
                    onDismiss = { dismissed = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("birthday_celebration_overlay").assertIsDisplayed()
        composeRule.onNodeWithText("Selamat ulang tahun, Raka!").assertIsDisplayed()
        composeRule.onNodeWithText("1 bulan 15 hari").assertIsDisplayed()
        composeRule.onNodeWithText("28").assertIsDisplayed()
        composeRule.onNodeWithTag("birthday_celebration_close_button").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `floating pill nav bar renders all 6 tabs including kalender on 320dp screen`() {
        composeRule.setContent {
            JadwalNihTheme(darkTheme = false) {
                ProvideAdaptiveLayoutSpec(availableWidth = 320.dp) {
                    FloatingPillNavBar(
                        selectedTab = MainTab.KALENDER,
                        onSelectTab = {},
                        tabIconProvider = { _, _ -> Icons.Filled.Timer },
                        visibleTabs = MainTab.entries,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        MainTab.entries.forEach { tab ->
            composeRule.onNodeWithTag("nav_tab_${tab.name.lowercase()}").assertIsDisplayed()
        }
    }

    @Test
    fun `rekap screen offers third Peta mode alongside Kamera and Galeri`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val todayIso = DateTimeHelper.todayIso()
        val photoFile = File(context.cacheDir, "rekap_map_test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            composeRule.setContent {
                JadwalNihTheme(darkTheme = true) {
                    ProvideAdaptiveLayoutSpec(availableWidth = 411.dp) {
                        RekapScreen(
                            state = JadwalNihUiState(
                                currentDateIso = todayIso,
                                selectedRecapDateIso = todayIso,
                                currentLocationName = "Bandung",
                                currentLatitude = -6.9175,
                                currentLongitude = 107.6191,
                                photoRecaps = mapOf(
                                    todayIso to PhotoRecap(
                                        dateIso = todayIso,
                                        title = "Pagi Produktif",
                                        caption = "Foto di Bandung",
                                        imagePaths = listOf(photoFile.absolutePath),
                                        updatedAtEpochMs = 1_700_000_000_000L,
                                    ),
                                ),
                            ),
                            onSelectDate = {},
                            onAddPhotos = { _, _ -> },
                            onSaveStyledPhoto = { _, _, _, _, _, _, _ -> },
                            onRemovePhoto = { _, _ -> },
                            onSaveMeta = { _, _, _ -> },
                            onDeleteRecap = {},
                            onExportCollage = {},
                            onNotifyMessage = {},
                            onOverlayVisibilityChange = {},
                        )
                    }
                }
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("rekap_mode_camera").assertIsDisplayed()
            composeRule.onNodeWithTag("rekap_mode_gallery").assertIsDisplayed()
            composeRule.onNodeWithTag("rekap_mode_map").assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("rekap_map_view").assertIsDisplayed()
            composeRule.onNodeWithTag("rekap_map_selected_card").assertIsDisplayed()
            composeRule.onNodeWithTag("rekap_map_move_pin_button").assertIsDisplayed()
            composeRule.onNodeWithTag("rekap_map_google_maps_button").assertIsDisplayed()
        } finally {
            photoFile.delete()
        }
    }
}
