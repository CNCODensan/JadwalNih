package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ImageStorageHelper
import com.example.data.JadwalNihRepository
import com.example.data.JsonBackupManager
import com.example.data.UserPreferencesStore
import com.example.domain.AiBatchScheduleParser
import com.example.domain.AiConflictResolution
import com.example.domain.AnalyticsSummary
import com.example.domain.AttendanceStatus
import com.example.domain.ConflictInfo
import com.example.domain.DailyChartPoint
import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.FocusTimerState
import com.example.domain.ForeshadowOccurrence
import com.example.domain.GreetingState
import com.example.domain.HeatmapDayCell
import com.example.domain.HolidayInfo
import com.example.domain.HolidayThemeType
import com.example.domain.LocationHelper
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
import com.example.domain.ReligionOption
import com.example.domain.ScheduleAnalyzer
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.TaskDraft
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.domain.ThemeMode
import com.example.domain.UserPreferences
import com.example.domain.WorshipScheduleItem
import com.example.notifications.ReminderScheduler
import com.example.widget.WidgetSyncHelper
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JadwalNihUiState(
    val selectedTab: MainTab = MainTab.JADWAL,
    val selectedScope: ScheduleScope = ScheduleScope.DAILY,
    val selectedDateIso: String = DateTimeHelper.todayIso(),
    val statusFilter: TaskStatus? = null,
    val matrixScope: ScheduleScope = ScheduleScope.DAILY,
    val matrixQuadrantFilter: PriorityQuadrant? = null,
    val analyticsScope: ScheduleScope = ScheduleScope.WEEKLY,
    val selectedRecapDateIso: String = DateTimeHelper.todayIso(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSimpleMode: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val isPreferencesLoaded: Boolean = false,
    val userName: String = "",
    val quickGlanceTarget: QuickGlanceTarget = QuickGlanceTarget.ANALITIK,
    val checkInNotificationsEnabled: Boolean = true,
    val taskDraft: TaskDraft? = null,
    val allTasksWithStatus: List<TaskWithStatus> = emptyList(),
    val filteredScheduleTasks: List<TaskWithStatus> = emptyList(),
    val matrixTasks: List<TaskWithStatus> = emptyList(),
    val checkInPromptTask: TaskWithStatus? = null,
    val recentMissedTaskForOverride: TaskWithStatus? = null,
    val yesterdayMissedTasks: List<TaskWithStatus> = emptyList(),
    val currentDateIso: String = DateTimeHelper.todayIso(),
    val currentMinuteOfDay: Int = DateTimeHelper.nowMinuteOfDay(),
    val quickGlance: QuickGlanceSummary = QuickGlanceSummary(),
    val focusSessions: List<FocusSession> = emptyList(),
    val photoRecaps: Map<String, PhotoRecap> = emptyMap(),
    val focusTimer: FocusTimerState = FocusTimerState(),
    val analytics: AnalyticsSummary = AnalyticsSummary(),
    val conflictsByTaskId: Map<Long, ConflictInfo> = emptyMap(),
    val selectedDateLoadMinutes: Int = 0,
    val selectedDateConflictCount: Int = 0,
    val greeting: GreetingState = GreetingState(
        greetingText = "Hai! Siap atur harimu?",
        encouragementText = "Mulai dari satu kegiatan ringan dulu yuk.",
    ),
    val celebrationTick: Int = 0,
    val snackbarMessage: String? = null,
    val canUndoDelete: Boolean = false,
    val isBusyBackup: Boolean = false,
    val isExportingCollage: Boolean = false,
    val pendingImportPreview: JsonBackupManager.BackupPreviewInfo? = null,
    // v1.0 release additions
    val religion: ReligionOption = ReligionOption.ISLAM,
    val prayerReminderEnabled: Boolean = true,
    val worshipNotificationSoundEnabled: Boolean = true,
    val holidayThemeEnabled: Boolean = true,
    val holidayThemePreview: HolidayThemeType = HolidayThemeType.NONE,
    val activeHolidayTheme: HolidayThemeType = HolidayThemeType.NONE,
    val todayHoliday: HolidayInfo? = null,
    val selectedDateHoliday: HolidayInfo? = null,
    val holidaysByDate: Map<String, HolidayInfo> = emptyMap(),
    val hasSeenV1WelcomeCard: Boolean = false,
    val hasPromptedInitialLocation: Boolean = false,
    val currentLocationName: String = "",
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val todayWorshipItems: List<WorshipScheduleItem> = emptyList(),
    val nextWorshipItem: WorshipScheduleItem? = null,
    val nextWorshipCountdown: String = "",
    val focusGentleWorshipPrompt: WorshipScheduleItem? = null,
    val notificationLogs: List<NotificationLogEntry> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val draftNotificationCount: Int = 0,
    val foreshadowOccurrences: List<ForeshadowOccurrence> = emptyList(),
)

class JadwalNihViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val imageHelper = ImageStorageHelper(application)
    private val prefsStore = UserPreferencesStore(application)
    private val backupManager = JsonBackupManager(
        context = application,
        taskDao = db.scheduleTaskDao(),
        focusDao = db.focusSessionDao(),
        recapDao = db.photoRecapDao(),
        imageHelper = imageHelper,
        notificationLogDao = db.notificationLogDao(),
        worshipCheckinDao = db.worshipCheckinDao(),
        prefsStore = prefsStore,
    )
    private val repository = JadwalNihRepository(
        taskDao = db.scheduleTaskDao(),
        focusDao = db.focusSessionDao(),
        recapDao = db.photoRecapDao(),
        imageHelper = imageHelper,
        backupManager = backupManager,
        notificationLogDao = db.notificationLogDao(),
        worshipCheckinDao = db.worshipCheckinDao(),
    )
    private val reminderScheduler = ReminderScheduler(application)

    private sealed interface UndoPayload {
        data class Tasks(val tasks: List<ScheduleTask>) : UndoPayload
        data class Recap(val recap: PhotoRecap) : UndoPayload
        data class Notifications(val logs: List<NotificationLogEntry>) : UndoPayload
    }

    private val _selectedTab = MutableStateFlow(MainTab.JADWAL)
    private val _selectedScope = MutableStateFlow(ScheduleScope.DAILY)
    private val _selectedDateIso = MutableStateFlow(DateTimeHelper.todayIso())
    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    private val _matrixScope = MutableStateFlow(ScheduleScope.DAILY)
    private val _matrixQuadrantFilter = MutableStateFlow<PriorityQuadrant?>(null)
    private val _analyticsScope = MutableStateFlow(ScheduleScope.WEEKLY)
    private val _selectedRecapDateIso = MutableStateFlow(DateTimeHelper.todayIso())
    private val _clockTick = MutableStateFlow(System.currentTimeMillis())
    private val _focusTimer = MutableStateFlow(FocusTimerState())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _pendingUndoPayload = MutableStateFlow<UndoPayload?>(null)
    private val _isBusyBackup = MutableStateFlow(false)
    private val _isExportingCollage = MutableStateFlow(false)
    private val _celebrationTick = MutableStateFlow(0)
    private val _pendingImportPreview = MutableStateFlow<JsonBackupManager.BackupPreviewInfo?>(null)
    private val _holidaysMap = MutableStateFlow<Map<String, HolidayInfo>>(emptyMap())
    private val _rawTodayWorship = MutableStateFlow<List<WorshipScheduleItem>>(emptyList())
    private val _dismissedFocusWorshipKey = MutableStateFlow<String?>(null)

    private var timerTickerJob: Job? = null
    private var draftSaveJob: Job? = null

    private data class NavFilterParams(
        val tab: MainTab,
        val scope: ScheduleScope,
        val dateIso: String,
        val statusFilter: TaskStatus?,
        val matrixScope: ScheduleScope,
        val matrixQuadrantFilter: PriorityQuadrant?,
        val analyticsScope: ScheduleScope,
        val recapDateIso: String,
    )

    private data class QuadFilter(
        val tab: MainTab,
        val scope: ScheduleScope,
        val dateIso: String,
        val statusFilter: TaskStatus?,
    )

    private data class RightFilter(
        val matrixScope: ScheduleScope,
        val matrixQuadrantFilter: PriorityQuadrant?,
        val analyticsScope: ScheduleScope,
        val recapDateIso: String,
    )

    private val navParamsFlow = combine(
        combine(_selectedTab, _selectedScope, _selectedDateIso, _statusFilter) { tab, scope, dateIso, statusFilter ->
            QuadFilter(tab, scope, dateIso, statusFilter)
        },
        combine(_matrixScope, _matrixQuadrantFilter, _analyticsScope, _selectedRecapDateIso) { mScope, mQuad, aScope, rDate ->
            RightFilter(mScope, mQuad, aScope, rDate)
        },
    ) { left, right ->
        NavFilterParams(
            tab = left.tab,
            scope = left.scope,
            dateIso = left.dateIso,
            statusFilter = left.statusFilter,
            matrixScope = right.matrixScope,
            matrixQuadrantFilter = right.matrixQuadrantFilter,
            analyticsScope = right.analyticsScope,
            recapDateIso = right.recapDateIso,
        )
    }

    private data class DataSnapshot(
        val tasks: List<ScheduleTask>,
        val sessions: List<FocusSession>,
        val recaps: List<PhotoRecap>,
        val prefs: UserPreferences,
        val draft: TaskDraft?,
        val nav: NavFilterParams,
    )

    private data class SecondaryDataSnapshot(
        val notificationLogs: List<NotificationLogEntry>,
        val worshipCheckinKeys: Set<String>,
        val holidaysMap: Map<String, HolidayInfo>,
        val rawTodayWorship: List<WorshipScheduleItem>,
        val dismissedFocusWorshipKey: String?,
    )

    private val prefsAndDraftFlow = combine(prefsStore.preferencesFlow, prefsStore.draftFlow) { prefs, draft ->
        prefs to draft
    }

    private val dataSnapshotFlow = combine(
        repository.tasksFlow,
        repository.focusSessionsFlow,
        repository.photoRecapsFlow,
        prefsAndDraftFlow,
        navParamsFlow,
    ) { tasks, sessions, recaps, prefsDraft, nav ->
        DataSnapshot(
            tasks = tasks,
            sessions = sessions,
            recaps = recaps,
            prefs = prefsDraft.first,
            draft = prefsDraft.second,
            nav = nav,
        )
    }

    private val secondaryDataFlow = combine(
        repository.notificationLogsFlow,
        repository.worshipCheckinKeysFlow,
        _holidaysMap,
        _rawTodayWorship,
        _dismissedFocusWorshipKey,
    ) { logs, checkins, holidays, rawWorship, dismissedKey ->
        SecondaryDataSnapshot(
            notificationLogs = logs,
            worshipCheckinKeys = checkins,
            holidaysMap = holidays,
            rawTodayWorship = rawWorship,
            dismissedFocusWorshipKey = dismissedKey,
        )
    }

    private data class FeedbackState(
        val snackbar: String?,
        val canUndoDelete: Boolean,
        val isBusyBackup: Boolean,
        val isExportingCollage: Boolean,
        val celebrationTick: Int,
        val pendingImportPreview: JsonBackupManager.BackupPreviewInfo?,
    )

    private val feedbackStateFlow = combine(
        combine(_snackbarMessage, _pendingUndoPayload) { msg, undo -> msg to (undo != null) },
        _isBusyBackup,
        _isExportingCollage,
        _celebrationTick,
        _pendingImportPreview,
    ) { msgUndo, isBusy, isExporting, celebrationTick, preview ->
        FeedbackState(msgUndo.first, msgUndo.second, isBusy, isExporting, celebrationTick, preview)
    }

    private data class ScheduleCalculations(
        val allTasksWithStatus: List<TaskWithStatus>,
        val filteredScheduleTasks: List<TaskWithStatus>,
        val matrixTasks: List<TaskWithStatus>,
        val checkInPromptTask: TaskWithStatus?,
        val recentMissedTaskForOverride: TaskWithStatus?,
        val yesterdayMissedTasks: List<TaskWithStatus>,
        val todayIso: String,
        val nowMinuteOfDay: Int,
        val quickGlance: QuickGlanceSummary,
        val photoRecapsMap: Map<String, PhotoRecap>,
        val focusSessions: List<FocusSession>,
        val analytics: AnalyticsSummary,
        val conflictsByTaskId: Map<Long, ConflictInfo>,
        val selectedDateLoadMinutes: Int,
        val selectedDateConflictCount: Int,
        val greeting: GreetingState,
        val draft: TaskDraft?,
        val prefs: UserPreferences,
        val nav: NavFilterParams,
        val secondary: SecondaryDataSnapshot,
        val activeHolidayTheme: HolidayThemeType,
        val todayHoliday: HolidayInfo?,
        val selectedDateHoliday: HolidayInfo?,
        val todayWorshipItems: List<WorshipScheduleItem>,
        val nextWorshipItem: WorshipScheduleItem?,
        val nextWorshipCountdown: String,
        val foreshadowOccurrences: List<ForeshadowOccurrence>,
    )

    private val scheduleCalculationsFlow = combine(
        dataSnapshotFlow,
        secondaryDataFlow,
        _clockTick,
    ) { snapshot, secondary, _ ->
        val tasks = snapshot.tasks
        val sessions = snapshot.sessions
        val recaps = snapshot.recaps
        val prefs = snapshot.prefs
        val nav = snapshot.nav

        val todayIso = DateTimeHelper.todayIso()
        val nowMin = DateTimeHelper.nowMinuteOfDay()

        val allWithStatus = tasks.map { task ->
            TaskWithStatus(
                task = task,
                status = StatusEvaluator.evaluate(task, todayIso, nowMin),
            )
        }

        val scopedSchedule = filterByScope(allWithStatus, nav.scope, nav.dateIso)
        val filteredSchedule = if (nav.statusFilter != null) {
            scopedSchedule.filter { it.status == nav.statusFilter }
        } else {
            scopedSchedule
        }

        val matrixAnchorIso = if (nav.matrixScope == ScheduleScope.MONTHLY) {
            nav.recapDateIso
        } else {
            nav.dateIso
        }
        val scopedMatrix = filterByScope(allWithStatus, nav.matrixScope, matrixAnchorIso)
        val matrixTasks = if (nav.matrixQuadrantFilter != null) {
            scopedMatrix.filter { it.task.quadrant == nav.matrixQuadrantFilter }
        } else {
            scopedMatrix
        }

        val conflictsByTaskId = ScheduleAnalyzer.detectConflicts(tasks)
        val selectedDateLoadMinutes = ScheduleAnalyzer.dailyScheduledMinutes(tasks, nav.dateIso)
        val selectedDateConflictCount = tasks.count {
            it.dateIso == nav.dateIso && !it.isCompleted && conflictsByTaskId.containsKey(it.id)
        }

        val streakDays = ScheduleAnalyzer.computeStreakDays(tasks, sessions, todayIso)
        val heatmapDays = ScheduleAnalyzer.buildHeatmap35Days(tasks, sessions, todayIso)

        val recapMap = recaps.associateBy { it.dateIso }
        val todayTasks = allWithStatus.filter { it.task.dateIso == todayIso }
        val todayCompletedCount = todayTasks.count { it.status == TaskStatus.COMPLETED_GREEN }
        val todayUpcoming = todayTasks
            .filterNot { it.status == TaskStatus.COMPLETED_GREEN }
            .sortedBy { it.task.startMinuteOfDay }
        val nextTask = todayUpcoming.firstOrNull {
            it.status != TaskStatus.OVERDUE_DARK
        } ?: todayUpcoming.firstOrNull()

        val checkInPromptTask = todayTasks
            .filter { StatusEvaluator.needsCheckInPrompt(it.task, todayIso, nowMin) }
            .sortedBy { it.task.startMinuteOfDay }
            .firstOrNull { it.task.attendanceStatus == AttendanceStatus.UNCONFIRMED }

        val recentMissedTaskForOverride = todayTasks
            .filter {
                it.status == TaskStatus.OVERDUE_DARK &&
                    it.task.attendanceStatus != AttendanceStatus.CONFIRMED_OVERRIDE
            }
            .maxByOrNull { it.task.endMinuteOfDay }

        val yesterdayIso = DateTimeHelper.addDays(todayIso, -1)
        val skippedIdsToday = if (prefs.skippedRescheduleDateIso == todayIso) {
            prefs.skippedRescheduleTaskIds
        } else {
            emptySet()
        }
        val yesterdayMissedTasks = allWithStatus
            .filter {
                it.task.dateIso == yesterdayIso &&
                    !it.task.isCompleted &&
                    it.task.attendanceStatus != AttendanceStatus.CONFIRMED_OVERRIDE &&
                    it.task.id !in skippedIdsToday
            }
            .sortedBy { it.task.startMinuteOfDay }

        val urgentCount = todayTasks.count {
            it.status != TaskStatus.COMPLETED_GREEN && (
                it.task.quadrant == PriorityQuadrant.Q1_IMPORTANT_URGENT ||
                    it.status == TaskStatus.CRITICAL_RED ||
                    it.status == TaskStatus.APPROACHING_YELLOW
                )
        }

        val quickGlance = QuickGlanceSummary(
            todayTotalCount = todayTasks.size,
            todayCompletedCount = todayCompletedCount,
            todayActiveOrUpcomingCount = todayUpcoming.size,
            urgentOrCriticalCount = urgentCount,
            nextTaskTitle = nextTask?.task?.title,
            nextTaskTimeLabel = nextTask?.let {
                DateTimeHelper.formatTimeRange(it.task.startMinuteOfDay, it.task.endMinuteOfDay)
            },
            todayRecap = recapMap[todayIso],
            streakDays = streakDays,
        )

        val greeting = ScheduleAnalyzer.buildGreeting(
            userName = prefs.userName,
            todayCompleted = todayCompletedCount,
            todayTotal = todayTasks.size,
            streakDays = streakDays,
        )

        // Holiday & Theme resolution
        val holidaysList = if (secondary.holidaysMap.isNotEmpty()) {
            secondary.holidaysMap.values.toList()
        } else {
            val currentYear = todayIso.take(4).toIntOrNull() ?: 2026
            PublicHolidayService.getCachedOrEmbeddedHolidaysForYear(currentYear)
        }
        val holidaysMapEffective = if (secondary.holidaysMap.isNotEmpty()) {
            secondary.holidaysMap
        } else {
            holidaysList.associateBy { it.dateIso }
        }
        val todayHoliday = holidaysMapEffective[todayIso]
        val selectedDateHoliday = holidaysMapEffective[nav.dateIso]
        val activeHolidayTheme = PublicHolidayService.resolveActiveHolidayTheme(
            todayIso = todayIso,
            holidays = holidaysList,
            holidayThemeEnabled = prefs.holidayThemeEnabled,
            previewTheme = prefs.holidayThemePreview,
        )

        // Worship schedule & completion mapping
        val baseWorship = if (secondary.rawTodayWorship.isNotEmpty() &&
            secondary.rawTodayWorship.firstOrNull()?.dateIso == todayIso
        ) {
            secondary.rawTodayWorship
        } else {
            PrayerTimesService.getCachedOrOfflineWorshipSchedule(
                dateIso = todayIso,
                religion = prefs.religion,
                latitude = prefs.lastLatitude,
                longitude = prefs.lastLongitude,
                completedKeys = secondary.worshipCheckinKeys,
            )
        }
        val todayWorshipItems = baseWorship.map { item ->
            val composite = "${item.dateIso}|${item.key}"
            item.copy(isCompleted = composite in secondary.worshipCheckinKeys)
        }
        val (nextWorship, countdownLabel) = PrayerTimesService.findNextPendingWorship(
            items = todayWorshipItems,
            nowMinuteOfDay = nowMin,
        )

        val worshipAnalytics = ScheduleAnalyzer.computeWorshipAnalytics(
            religion = prefs.religion,
            enabled = prefs.prayerReminderEnabled,
            todayIso = todayIso,
            completedDateItemKeys = secondary.worshipCheckinKeys,
            todayWorshipItems = todayWorshipItems,
        )

        val analytics = computeAnalytics(
            allTasks = allWithStatus,
            sessions = sessions,
            scope = nav.analyticsScope,
            anchorIso = nav.dateIso,
            streakDays = streakDays,
            heatmapDays = heatmapDays,
            worshipSummary = worshipAnalytics,
        )

        val foreshadows = StatusEvaluator.buildForeshadowOccurrences(
            tasks = tasks,
            todayIso = todayIso,
            horizonDays = 28,
        )

        ScheduleCalculations(
            allTasksWithStatus = allWithStatus,
            filteredScheduleTasks = filteredSchedule,
            matrixTasks = matrixTasks,
            checkInPromptTask = checkInPromptTask,
            recentMissedTaskForOverride = recentMissedTaskForOverride,
            yesterdayMissedTasks = yesterdayMissedTasks,
            todayIso = todayIso,
            nowMinuteOfDay = nowMin,
            quickGlance = quickGlance,
            photoRecapsMap = recapMap,
            focusSessions = sessions,
            analytics = analytics,
            conflictsByTaskId = conflictsByTaskId,
            selectedDateLoadMinutes = selectedDateLoadMinutes,
            selectedDateConflictCount = selectedDateConflictCount,
            greeting = greeting,
            draft = snapshot.draft,
            prefs = prefs,
            nav = nav,
            secondary = secondary.copy(holidaysMap = holidaysMapEffective),
            activeHolidayTheme = activeHolidayTheme,
            todayHoliday = todayHoliday,
            selectedDateHoliday = selectedDateHoliday,
            todayWorshipItems = todayWorshipItems,
            nextWorshipItem = nextWorship,
            nextWorshipCountdown = countdownLabel,
            foreshadowOccurrences = foreshadows,
        )
    }

    val uiState: StateFlow<JadwalNihUiState> = combine(
        scheduleCalculationsFlow,
        _focusTimer,
        feedbackStateFlow,
    ) { calc, focusState, feedback ->
        val logs = calc.secondary.notificationLogs
        val unreadCount = logs.count { !it.isRead }
        val draftCount = logs.count { it.category == NotificationCategory.DRAFT }

        // Gentle focus worship reminder: if Focus timer is running and a worship item window just entered [-2..+20] mins
        val gentlePrompt = if (calc.prefs.prayerReminderEnabled && focusState.isRunning) {
            calc.todayWorshipItems.firstOrNull { w ->
                !w.isCompleted &&
                    calc.nowMinuteOfDay in w.minuteOfDay..(w.minuteOfDay + 20) &&
                    "${w.dateIso}|${w.key}" != calc.secondary.dismissedFocusWorshipKey
            }
        } else {
            null
        }

        JadwalNihUiState(
            selectedTab = calc.nav.tab,
            selectedScope = calc.nav.scope,
            selectedDateIso = calc.nav.dateIso,
            statusFilter = calc.nav.statusFilter,
            matrixScope = calc.nav.matrixScope,
            matrixQuadrantFilter = calc.nav.matrixQuadrantFilter,
            analyticsScope = calc.nav.analyticsScope,
            selectedRecapDateIso = calc.nav.recapDateIso,
            themeMode = calc.prefs.themeMode,
            isSimpleMode = calc.prefs.isSimpleMode,
            hasCompletedOnboarding = calc.prefs.hasCompletedOnboarding,
            isPreferencesLoaded = true,
            userName = calc.prefs.userName,
            quickGlanceTarget = calc.prefs.quickGlanceTarget,
            checkInNotificationsEnabled = calc.prefs.checkInNotificationsEnabled,
            taskDraft = calc.draft,
            allTasksWithStatus = calc.allTasksWithStatus,
            filteredScheduleTasks = calc.filteredScheduleTasks,
            matrixTasks = calc.matrixTasks,
            checkInPromptTask = calc.checkInPromptTask,
            recentMissedTaskForOverride = calc.recentMissedTaskForOverride,
            yesterdayMissedTasks = calc.yesterdayMissedTasks,
            currentDateIso = calc.todayIso,
            currentMinuteOfDay = calc.nowMinuteOfDay,
            quickGlance = calc.quickGlance,
            focusSessions = calc.focusSessions,
            photoRecaps = calc.photoRecapsMap,
            focusTimer = focusState,
            analytics = calc.analytics,
            conflictsByTaskId = calc.conflictsByTaskId,
            selectedDateLoadMinutes = calc.selectedDateLoadMinutes,
            selectedDateConflictCount = calc.selectedDateConflictCount,
            greeting = calc.greeting,
            celebrationTick = feedback.celebrationTick,
            snackbarMessage = feedback.snackbar,
            canUndoDelete = feedback.canUndoDelete,
            isBusyBackup = feedback.isBusyBackup,
            isExportingCollage = feedback.isExportingCollage,
            pendingImportPreview = feedback.pendingImportPreview,
            religion = calc.prefs.religion,
            prayerReminderEnabled = calc.prefs.prayerReminderEnabled,
            worshipNotificationSoundEnabled = calc.prefs.worshipNotificationSoundEnabled,
            holidayThemeEnabled = calc.prefs.holidayThemeEnabled,
            holidayThemePreview = calc.prefs.holidayThemePreview,
            activeHolidayTheme = calc.activeHolidayTheme,
            todayHoliday = calc.todayHoliday,
            selectedDateHoliday = calc.selectedDateHoliday,
            holidaysByDate = calc.secondary.holidaysMap,
            hasSeenV1WelcomeCard = calc.prefs.hasSeenV1WelcomeCard,
            hasPromptedInitialLocation = calc.prefs.hasPromptedInitialLocation,
            currentLocationName = calc.prefs.lastLocationName,
            currentLatitude = calc.prefs.lastLatitude,
            currentLongitude = calc.prefs.lastLongitude,
            todayWorshipItems = calc.todayWorshipItems,
            nextWorshipItem = calc.nextWorshipItem,
            nextWorshipCountdown = calc.nextWorshipCountdown,
            focusGentleWorshipPrompt = gentlePrompt,
            notificationLogs = logs,
            unreadNotificationCount = unreadCount,
            draftNotificationCount = draftCount,
            foreshadowOccurrences = calc.foreshadowOccurrences,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JadwalNihUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                _clockTick.value = System.currentTimeMillis()
            }
        }
        refreshDynamicServices(tryGpsLocation = false)
    }

    fun refreshDynamicServices(tryGpsLocation: Boolean = true) {
        viewModelScope.launch {
            val todayIso = DateTimeHelper.todayIso()
            val prefs = prefsStore.preferencesFlow.first()

            // 1. Prune expired notifications (> 30 days)
            repository.pruneExpiredNotifications()

            // 2. Materialize due recurring schedules automatically up to today
            val materialized = repository.materializeDueRecurrences(todayIso)
            if (materialized.isNotEmpty()) {
                materialized.forEach { t ->
                    if (!t.isCompleted && (t.reminderEnabled || prefs.checkInNotificationsEnabled)) {
                        reminderScheduler.scheduleForTask(t, checkInNotificationsEnabled = prefs.checkInNotificationsEnabled)
                    }
                }
            }

            // 3. Fetch Indonesian national holidays for current & next year
            val year = todayIso.take(4).toIntOrNull() ?: 2026
            val combinedHolidays = (PublicHolidayService.getHolidaysForYear(year) +
                PublicHolidayService.getHolidaysForYear(year + 1))
                .associateBy { it.dateIso }
            _holidaysMap.value = combinedHolidays

            val todayHoliday = combinedHolidays[todayIso]
            if (todayHoliday != null && !todayHoliday.isCutiBersama) {
                repository.logNotification(
                    category = NotificationCategory.HOLIDAY,
                    title = "Hari Raya: ${todayHoliday.name}",
                    message = todayHoliday.bannerGreeting,
                    isRead = false,
                )
            }

            // 4. Refresh GPS location if permitted
            var lat = prefs.lastLatitude
            var lng = prefs.lastLongitude
            if (tryGpsLocation && LocationHelper.hasLocationPermission(getApplication())) {
                val resolved = LocationHelper.resolveCurrentLocation(getApplication())
                if (resolved != null) {
                    prefsStore.setLastLocation(resolved.locationName, resolved.latitude, resolved.longitude)
                    lat = resolved.latitude
                    lng = resolved.longitude
                }
            }

            // 5. Fetch & schedule worship reminders for selected religion
            refreshWorshipSchedule(
                dateIso = todayIso,
                religion = prefs.religion,
                lat = lat,
                lng = lng,
                enabled = prefs.prayerReminderEnabled,
                soundEnabled = prefs.worshipNotificationSoundEnabled,
            )

            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
        }
    }

    private suspend fun refreshWorshipSchedule(
        dateIso: String,
        religion: ReligionOption,
        lat: Double?,
        lng: Double?,
        enabled: Boolean,
        soundEnabled: Boolean,
    ) {
        val completed = repository.worshipCheckinKeysFlow.first()
        val items = PrayerTimesService.getDailyWorshipSchedule(
            dateIso = dateIso,
            religion = religion,
            latitude = lat,
            longitude = lng,
            completedKeys = completed,
        )
        _rawTodayWorship.value = items
        reminderScheduler.scheduleWorshipReminders(
            items = items,
            enabled = enabled,
            soundEnabled = soundEnabled,
        )
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectScope(scope: ScheduleScope) {
        _selectedScope.value = scope
    }

    fun selectDate(dateIso: String) {
        _selectedDateIso.value = dateIso
    }

    fun selectSharedCalendarDate(dateIso: String) {
        _selectedRecapDateIso.value = dateIso
        _selectedDateIso.value = dateIso
    }

    fun stepDate(direction: Int) {
        val current = _selectedDateIso.value
        _selectedDateIso.value = when (_selectedScope.value) {
            ScheduleScope.DAILY -> DateTimeHelper.addDays(current, direction)
            ScheduleScope.WEEKLY -> DateTimeHelper.addDays(current, direction * 7)
            ScheduleScope.MONTHLY -> DateTimeHelper.addMonths(current, direction)
        }
    }

    fun jumpToToday() {
        val today = DateTimeHelper.todayIso()
        _selectedDateIso.value = today
        _selectedRecapDateIso.value = today
    }

    fun setStatusFilter(status: TaskStatus?) {
        _statusFilter.value = if (_statusFilter.value == status) null else status
    }

    fun setMatrixScope(scope: ScheduleScope) {
        _matrixScope.value = scope
    }

    fun setMatrixQuadrantFilter(quadrant: PriorityQuadrant?) {
        _matrixQuadrantFilter.value = if (_matrixQuadrantFilter.value == quadrant) null else quadrant
    }

    fun setAnalyticsScope(scope: ScheduleScope) {
        _analyticsScope.value = scope
    }

    fun selectRecapDate(dateIso: String) {
        _selectedRecapDateIso.value = dateIso
    }

    // --- User Settings & v1.0 Controls ---

    fun updateUserName(name: String) {
        viewModelScope.launch { prefsStore.setUserName(name) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsStore.setThemeMode(mode) }
    }

    fun setSimpleMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setSimpleMode(enabled)
            if (enabled && (_selectedTab.value == MainTab.MATRIKS || _selectedTab.value == MainTab.ANALITIK)) {
                _selectedTab.value = MainTab.JADWAL
            }
        }
    }

    fun completeOnboarding(name: String, isSimpleMode: Boolean, themeMode: ThemeMode) {
        viewModelScope.launch {
            prefsStore.completeOnboarding(name, isSimpleMode, themeMode)
            _selectedTab.value = MainTab.JADWAL
            val greetingName = name.trim()
            _pendingUndoPayload.value = null
            _snackbarMessage.value = if (greetingName.isNotEmpty()) {
                "Halo $greetingName! JadwalNih siap dipakai."
            } else {
                "Selamat datang di JadwalNih!"
            }
        }
    }

    fun setQuickGlanceTarget(target: QuickGlanceTarget) {
        viewModelScope.launch { prefsStore.setQuickGlanceTarget(target) }
    }

    fun setCheckInNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setCheckInNotificationsEnabled(enabled)
            uiState.value.allTasksWithStatus.forEach { item ->
                if (!item.task.isCompleted) {
                    reminderScheduler.scheduleForTask(item.task, checkInNotificationsEnabled = enabled)
                }
            }
        }
    }

    fun setReligion(religion: ReligionOption) {
        viewModelScope.launch {
            prefsStore.setReligion(religion)
            val state = uiState.value
            refreshWorshipSchedule(
                dateIso = DateTimeHelper.todayIso(),
                religion = religion,
                lat = state.currentLatitude,
                lng = state.currentLongitude,
                enabled = state.prayerReminderEnabled,
                soundEnabled = state.worshipNotificationSoundEnabled,
            )
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _snackbarMessage.value = "Jadwal ibadah disesuaikan untuk ${religion.labelId}."
        }
    }

    fun setPrayerReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setPrayerReminderEnabled(enabled)
            val state = uiState.value
            reminderScheduler.scheduleWorshipReminders(
                items = state.todayWorshipItems,
                enabled = enabled,
                soundEnabled = state.worshipNotificationSoundEnabled,
            )
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
        }
    }

    fun setWorshipNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setWorshipNotificationSoundEnabled(enabled)
            val state = uiState.value
            reminderScheduler.scheduleWorshipReminders(
                items = state.todayWorshipItems,
                enabled = state.prayerReminderEnabled,
                soundEnabled = enabled,
            )
            _snackbarMessage.value = if (enabled) {
                "Notifikasi ibadah pakai suara."
            } else {
                "Notifikasi ibadah diubah ke mode senyap."
            }
        }
    }

    fun setHolidayThemeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setHolidayThemeEnabled(enabled) }
    }

    fun setHolidayThemePreview(preview: HolidayThemeType) {
        viewModelScope.launch {
            prefsStore.setHolidayThemePreview(preview)
            _snackbarMessage.value = if (preview == HolidayThemeType.NONE) {
                "Tema Hari Raya balik ke otomatis."
            } else {
                "Pratinjau tema ${preview.labelId} aktif."
            }
        }
    }

    fun dismissV1WelcomeCard() {
        viewModelScope.launch { prefsStore.setHasSeenV1WelcomeCard(true) }
    }

    fun markInitialLocationPrompted() {
        viewModelScope.launch { prefsStore.setHasPromptedInitialLocation(true) }
    }

    fun detectCurrentLocationNow(showFeedback: Boolean = true) {
        viewModelScope.launch {
            val resolved = LocationHelper.resolveCurrentLocation(getApplication())
            if (resolved != null) {
                prefsStore.setLastLocation(resolved.locationName, resolved.latitude, resolved.longitude)
                val state = uiState.value
                refreshWorshipSchedule(
                    dateIso = DateTimeHelper.todayIso(),
                    religion = state.religion,
                    lat = resolved.latitude,
                    lng = resolved.longitude,
                    enabled = state.prayerReminderEnabled,
                    soundEnabled = state.worshipNotificationSoundEnabled,
                )
                if (showFeedback) {
                    _pendingUndoPayload.value = null
                    _snackbarMessage.value = "Lokasi terdeteksi: ${resolved.locationName}"
                }
            } else if (showFeedback) {
                _pendingUndoPayload.value = null
                _snackbarMessage.value = "GPS belum aktif/terjangkau. Kamu bisa isi kota manual di Pengaturan."
            }
        }
    }

    fun updateManualLocation(locationName: String) {
        viewModelScope.launch {
            val trimmed = locationName.trim()
            prefsStore.setLastLocation(trimmed, uiState.value.currentLatitude, uiState.value.currentLongitude)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = if (trimmed.isNotEmpty()) {
                "Lokasi disimpan: $trimmed"
            } else {
                "Lokasi manual dikosongkan."
            }
        }
    }

    fun toggleWorshipComplete(item: WorshipScheduleItem) {
        viewModelScope.launch {
            val nowDone = repository.toggleWorshipCheckin(item, uiState.value.religion)
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            if (nowDone) {
                _celebrationTick.update { it + 1 }
                _snackbarMessage.value = "${item.title} tercatat sudah ibadah."
            } else {
                _snackbarMessage.value = "Centang ${item.title} dibuka lagi."
            }
        }
    }

    fun dismissGentleFocusWorshipPrompt() {
        val item = uiState.value.focusGentleWorshipPrompt ?: return
        _dismissedFocusWorshipKey.value = "${item.dateIso}|${item.key}"
    }

    // --- Draft & Notification Center ---

    fun updateLiveDraft(draft: TaskDraft) {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(180L)
            if (draft.hasMeaningfulContent()) {
                prefsStore.saveDraft(draft)
            } else {
                prefsStore.saveDraft(null)
            }
        }
    }

    fun saveTaskDraftAndNotifyCenter(draft: TaskDraft) {
        draftSaveJob?.cancel()
        viewModelScope.launch {
            prefsStore.saveDraft(draft)
            repository.saveDraftToNotificationCenter(draft)
        }
    }

    fun saveTaskDraft(draft: TaskDraft) {
        saveTaskDraftAndNotifyCenter(draft)
    }

    fun clearTaskDraft() {
        draftSaveJob?.cancel()
        viewModelScope.launch {
            prefsStore.saveDraft(null)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun deleteNotificationLogWithUndo(entry: NotificationLogEntry) {
        viewModelScope.launch {
            repository.deleteNotificationLog(entry.id)
            _pendingUndoPayload.value = UndoPayload.Notifications(listOf(entry))
            _snackbarMessage.value = "Notifikasi dihapus."
        }
    }

    fun clearAllNotificationsWithUndo() {
        val snapshot = uiState.value.notificationLogs
        if (snapshot.isEmpty()) return
        viewModelScope.launch {
            repository.clearAllNotifications()
            _pendingUndoPayload.value = UndoPayload.Notifications(snapshot)
            _snackbarMessage.value = "${snapshot.size} riwayat notifikasi dibersihkan."
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
        _pendingUndoPayload.value = null
    }

    fun showMessage(msg: String) {
        _pendingUndoPayload.value = null
        _snackbarMessage.value = msg
    }

    fun undoLastDelete() {
        val payload = _pendingUndoPayload.value ?: return
        _pendingUndoPayload.value = null
        viewModelScope.launch {
            when (payload) {
                is UndoPayload.Tasks -> {
                    val toRestore = payload.tasks.map { it.copy(id = 0L) }
                    val restored = repository.insertTasksBatch(toRestore)
                    val checkInEnabled = uiState.value.checkInNotificationsEnabled
                    restored.forEach { t ->
                        if (!t.isCompleted && (t.reminderEnabled || checkInEnabled)) {
                            reminderScheduler.scheduleForTask(t, checkInNotificationsEnabled = checkInEnabled)
                        }
                    }
                    WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
                    _snackbarMessage.value = if (restored.size == 1) {
                        "Jadwal \"${restored.first().title}\" dikembalikan."
                    } else {
                        "${restored.size} jadwal dikembalikan."
                    }
                }
                is UndoPayload.Recap -> {
                    repository.restoreRecap(payload.recap)
                    _snackbarMessage.value = "Rekap tanggal ${DateTimeHelper.formatShortDate(payload.recap.dateIso)} dikembalikan."
                }
                is UndoPayload.Notifications -> {
                    repository.restoreNotificationLogs(payload.logs.map { it.copy(id = 0L) })
                    _snackbarMessage.value = "Riwayat notifikasi dikembalikan."
                }
            }
        }
    }

    // --- Task CRUD, AI Batch Conflict Resolution & Foreshadow ---

    fun saveTask(task: ScheduleTask) {
        draftSaveJob?.cancel()
        viewModelScope.launch {
            val savedId = repository.upsertTask(task)
            val persisted = task.copy(id = if (task.id > 0L) task.id else savedId)
            val checkInEnabled = uiState.value.checkInNotificationsEnabled
            if (!persisted.isCompleted && (persisted.reminderEnabled || checkInEnabled)) {
                reminderScheduler.scheduleForTask(persisted, checkInNotificationsEnabled = checkInEnabled)
            } else {
                reminderScheduler.cancelForTask(persisted.id)
            }
            prefsStore.saveDraft(null)
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = if (task.id == 0L) "Jadwal ditambahkan." else "Jadwal diperbarui."
        }
    }

    fun saveBatchTasks(
        tasks: List<ScheduleTask>,
        conflictMessages: List<String>,
        conflictResolution: AiConflictResolution = AiConflictResolution.KEEP_BOTH_CONFLICTING,
    ) {
        if (tasks.isEmpty()) return
        draftSaveJob?.cancel()
        viewModelScope.launch {
            val existing = repository.getAllTasksOnce()
            val detailedConflicts = AiBatchScheduleParser.detectDetailedConflicts(tasks, existing)

            val tasksToInsert: List<ScheduleTask> = when (conflictResolution) {
                AiConflictResolution.OVERWRITE_EXISTING -> {
                    val conflictingOldIds = detailedConflicts
                        .flatMap { it.conflictingExistingTasks }
                        .map { it.id }
                        .distinct()
                    conflictingOldIds.forEach { oldId ->
                        reminderScheduler.cancelForTask(oldId)
                        repository.deleteTask(oldId)
                    }
                    tasks
                }
                AiConflictResolution.KEEP_EXISTING_SKIP_NEW -> {
                    val conflictIndexes = detailedConflicts.map { it.importedIndex }.toSet()
                    tasks.filterIndexed { idx, _ -> idx !in conflictIndexes }
                }
                AiConflictResolution.KEEP_BOTH_CONFLICTING -> tasks
            }

            if (tasksToInsert.isEmpty()) {
                _pendingUndoPayload.value = null
                _snackbarMessage.value = "Semua jadwal bentrok dilewati karena memilih simpan jadwal lama."
                return@launch
            }

            val persistedList = repository.insertTasksBatch(tasksToInsert)
            val checkInEnabled = uiState.value.checkInNotificationsEnabled
            persistedList.forEach { persisted ->
                if (!persisted.isCompleted && (persisted.reminderEnabled || checkInEnabled)) {
                    reminderScheduler.scheduleForTask(persisted, checkInNotificationsEnabled = checkInEnabled)
                }
            }
            prefsStore.saveDraft(null)
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _celebrationTick.update { it + 1 }

            val distinctDates = persistedList.map { it.dateIso }.distinct().sorted()
            if (distinctDates.size > 1) {
                _selectedScope.value = ScheduleScope.WEEKLY
                _selectedDateIso.value = distinctDates.first()
            } else {
                val firstDate = distinctDates.firstOrNull()
                if (firstDate != null && persistedList.none { it.dateIso == _selectedDateIso.value }) {
                    _selectedDateIso.value = firstDate
                }
            }

            _pendingUndoPayload.value = null
            _snackbarMessage.value = when {
                distinctDates.size > 1 ->
                    "Sip! ${persistedList.size} jadwal (${distinctDates.size} hari) masuk, tampilan pindah ke Mingguan."
                conflictResolution == AiConflictResolution.OVERWRITE_EXISTING && detailedConflicts.isNotEmpty() ->
                    "Berhasil simpan ${persistedList.size} jadwal & menimpa jadwal lama yang bentrok."
                conflictMessages.isNotEmpty() && conflictResolution == AiConflictResolution.KEEP_BOTH_CONFLICTING ->
                    "Berhasil simpan ${persistedList.size} jadwal (${conflictMessages.size} jam berbarengan)."
                else ->
                    "Sip! ${persistedList.size} jadwal baru berhasil disimpan."
            }
        }
    }

    fun activateForeshadowEarly(foreshadow: ForeshadowOccurrence, onOpenEdit: (ScheduleTask) -> Unit) {
        viewModelScope.launch {
            val materialized = repository.materializeForeshadowEarly(foreshadow.parentTask, foreshadow.occurrenceDateIso)
            val checkInEnabled = uiState.value.checkInNotificationsEnabled
            if (materialized.reminderEnabled || checkInEnabled) {
                reminderScheduler.scheduleForTask(materialized, checkInNotificationsEnabled = checkInEnabled)
            }
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            onOpenEdit(materialized)
        }
    }

    fun skipForeshadowOccurrence(foreshadow: ForeshadowOccurrence) {
        viewModelScope.launch {
            repository.skipRecurrenceDate(foreshadow.parentTask, foreshadow.occurrenceDateIso)
            _pendingUndoPayload.value = null
            _snackbarMessage.value =
                "Jadwal \"${foreshadow.parentTask.title}\" tanggal ${DateTimeHelper.formatShortDate(foreshadow.occurrenceDateIso)} dilewati."
        }
    }

    fun rescheduleYesterdayMissedToToday() {
        val allIds = uiState.value.yesterdayMissedTasks.map { it.task.id }.toSet()
        rescheduleYesterdayTasksToToday(allIds)
    }

    fun rescheduleYesterdayTasksToToday(selectedIds: Set<Long>) {
        if (selectedIds.isEmpty()) return
        val toMove = uiState.value.yesterdayMissedTasks
            .map { it.task }
            .filter { it.id in selectedIds }
        if (toMove.isEmpty()) return
        val todayIso = DateTimeHelper.todayIso()
        viewModelScope.launch {
            val moved = repository.rescheduleTasksToDate(toMove, todayIso)
            val checkInEnabled = uiState.value.checkInNotificationsEnabled
            moved.forEach { task ->
                if (task.reminderEnabled || checkInEnabled) {
                    reminderScheduler.scheduleForTask(task, checkInNotificationsEnabled = checkInEnabled)
                }
            }
            _selectedDateIso.value = todayIso
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = if (moved.size == 1) {
                "Sip! \"${moved.first().title}\" dipindah ke hari ini."
            } else {
                "Sip! ${moved.size} jadwal kemarin dipindah ke hari ini."
            }
        }
    }

    fun skipYesterdayReschedule(taskIds: Set<Long>) {
        if (taskIds.isEmpty()) return
        val todayIso = DateTimeHelper.todayIso()
        viewModelScope.launch {
            prefsStore.skipYesterdayReschedule(todayIso, taskIds)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Oke, jadwal kemarin dilewati buat hari ini."
        }
    }

    fun confirmTaskAttendance(task: ScheduleTask, isAttending: Boolean) {
        viewModelScope.launch {
            _pendingUndoPayload.value = null
            if (isAttending) {
                repository.updateAttendance(task, AttendanceStatus.CONFIRMED_ATTENDING, markCompleted = false)
                _snackbarMessage.value = "Sip! Kehadiran buat \"${task.title}\" dicatat."
            } else {
                repository.updateAttendance(task, AttendanceStatus.NOT_YET, markCompleted = false)
                _snackbarMessage.value = "Dicatat belum ikut. Masih bisa konfirmasi nanti."
            }
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
        }
    }

    fun overrideMissedTaskAsAttended(task: ScheduleTask) {
        viewModelScope.launch {
            repository.updateAttendance(task, AttendanceStatus.CONFIRMED_OVERRIDE, markCompleted = true)
            reminderScheduler.cancelForTask(task.id)
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _celebrationTick.update { it + 1 }
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Status \"${task.title}\" diubah jadi Sudah Ikut & Selesai!"
        }
    }

    fun toggleTaskComplete(task: ScheduleTask) {
        viewModelScope.launch {
            val nextRecurring = repository.toggleTaskComplete(task)
            if (!task.isCompleted) {
                reminderScheduler.cancelForTask(task.id)
                _celebrationTick.update { it + 1 }
            } else if (task.reminderEnabled || uiState.value.checkInNotificationsEnabled) {
                reminderScheduler.scheduleForTask(
                    task.copy(isCompleted = false),
                    checkInNotificationsEnabled = uiState.value.checkInNotificationsEnabled,
                )
            }
            if (nextRecurring != null && (nextRecurring.reminderEnabled || uiState.value.checkInNotificationsEnabled)) {
                reminderScheduler.scheduleForTask(
                    nextRecurring,
                    checkInNotificationsEnabled = uiState.value.checkInNotificationsEnabled,
                )
            }
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = when {
                !task.isCompleted && nextRecurring != null ->
                    "Beres! Jadwal berikutnya disiapkan (${DateTimeHelper.formatShortDate(nextRecurring.dateIso)})."
                !task.isCompleted -> "Sip, selesai!"
                else -> "Jadwal dibuka lagi."
            }
        }
    }

    fun toggleSubTask(task: ScheduleTask, subTaskId: String) {
        viewModelScope.launch {
            val allJustCompleted = repository.toggleSubTask(task, subTaskId)
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            if (allJustCompleted) {
                _celebrationTick.update { it + 1 }
                _pendingUndoPayload.value = null
                _snackbarMessage.value = "Mantap! Semua langkah di \"${task.title}\" beres."
            }
        }
    }

    fun moveTaskToTomorrow(task: ScheduleTask) {
        viewModelScope.launch {
            val moved = repository.moveTaskToTomorrow(task)
            if (moved.reminderEnabled || uiState.value.checkInNotificationsEnabled) {
                reminderScheduler.scheduleForTask(
                    moved,
                    checkInNotificationsEnabled = uiState.value.checkInNotificationsEnabled,
                )
            } else {
                reminderScheduler.cancelForTask(moved.id)
            }
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Dipindah ke ${DateTimeHelper.formatShortDate(moved.dateIso)}."
        }
    }

    fun changeTaskQuadrant(task: ScheduleTask, quadrant: PriorityQuadrant) {
        viewModelScope.launch {
            repository.updateTaskQuadrant(task, quadrant)
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Dipindah ke ${quadrant.shortCode} • ${quadrant.chipLabelId}."
        }
    }

    fun deleteTask(task: ScheduleTask, deleteAllSeries: Boolean = false) {
        viewModelScope.launch {
            if (deleteAllSeries && task.recurrence != com.example.domain.RecurrenceRule.NONE) {
                val deleted = repository.deleteRecurringSeries(task)
                deleted.forEach { reminderScheduler.cancelForTask(it.id) }
                _pendingUndoPayload.value = UndoPayload.Tasks(deleted)
                _snackbarMessage.value = "${deleted.size} pengulangan \"${task.title}\" dihapus."
            } else {
                if (task.recurrence != com.example.domain.RecurrenceRule.NONE) {
                    repository.skipRecurrenceDate(task, task.dateIso)
                }
                reminderScheduler.cancelForTask(task.id)
                repository.deleteTask(task.id)
                _pendingUndoPayload.value = UndoPayload.Tasks(listOf(task))
                _snackbarMessage.value = "Jadwal \"${task.title}\" dihapus."
            }
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
        }
    }

    // --- Custom Focus Timer ---

    fun updateCustomFocusMinutes(rawInput: String) {
        if (_focusTimer.value.hasActiveSession) return
        val filtered = rawInput.filter { it.isDigit() }.take(3)
        val parsed = filtered.toIntOrNull()
        val error = when {
            filtered.isEmpty() -> "Isi durasi 1–240 menit."
            parsed == null || parsed < 1 -> "Minimal 1 menit."
            parsed > 240 -> "Maksimal 240 menit (4 jam)."
            else -> null
        }
        val validMinutes = (parsed ?: 1).coerceIn(1, 240)
        _focusTimer.update {
            it.copy(
                customMinutesInput = filtered,
                validationError = error,
                plannedMinutes = if (error == null) validMinutes else it.plannedMinutes,
                remainingSeconds = if (error == null) validMinutes * 60 else it.remainingSeconds,
            )
        }
    }

    fun stepCustomFocusMinutes(delta: Int) {
        if (_focusTimer.value.hasActiveSession) return
        val current = _focusTimer.value.customMinutesInput.toIntOrNull() ?: _focusTimer.value.plannedMinutes
        val next = (current + delta).coerceIn(1, 240)
        updateCustomFocusMinutes(next.toString())
    }

    fun selectFocusTask(task: ScheduleTask?) {
        _focusTimer.update {
            it.copy(
                linkedTaskId = task?.id,
                linkedTaskTitle = task?.title,
            )
        }
    }

    fun startFocusTimer() {
        val current = _focusTimer.value
        val parsed = current.customMinutesInput.toIntOrNull()
        if (parsed == null || parsed !in 1..240) {
            _focusTimer.update {
                it.copy(validationError = "Durasi harus antara 1 sampai 240 menit dulu ya.")
            }
            return
        }

        val totalSecs = if (current.hasActiveSession && current.isPaused) {
            current.remainingSeconds
        } else {
            parsed * 60
        }

        _focusTimer.update {
            it.copy(
                validationError = null,
                plannedMinutes = parsed,
                remainingSeconds = totalSecs,
                elapsedSeconds = if (current.hasActiveSession) current.elapsedSeconds else 0,
                isRunning = true,
                isPaused = false,
                hasActiveSession = true,
                startedAtEpochMs = current.startedAtEpochMs ?: System.currentTimeMillis(),
            )
        }
        launchTimerLoop()
    }

    fun pauseFocusTimer() {
        timerTickerJob?.cancel()
        _focusTimer.update {
            if (!it.hasActiveSession) it else it.copy(isRunning = false, isPaused = true)
        }
    }

    fun resumeFocusTimer() {
        if (!_focusTimer.value.hasActiveSession) return
        _focusTimer.update { it.copy(isRunning = true, isPaused = false) }
        launchTimerLoop()
    }

    fun resetFocusTimer() {
        timerTickerJob?.cancel()
        val planned = _focusTimer.value.plannedMinutes.coerceIn(1, 240)
        _focusTimer.update {
            it.copy(
                remainingSeconds = planned * 60,
                elapsedSeconds = 0,
                isRunning = false,
                isPaused = false,
                hasActiveSession = false,
                isDistractionFreeMode = false,
                startedAtEpochMs = null,
            )
        }
    }

    fun stopAndSaveFocusTimer() {
        timerTickerJob?.cancel()
        val snapshot = _focusTimer.value
        if (snapshot.hasActiveSession && snapshot.elapsedSeconds >= 10) {
            val now = System.currentTimeMillis()
            val session = FocusSession(
                linkedTaskId = snapshot.linkedTaskId,
                linkedTaskTitle = snapshot.linkedTaskTitle,
                plannedMinutes = snapshot.plannedMinutes,
                actualSeconds = snapshot.elapsedSeconds,
                completedNaturally = snapshot.remainingSeconds == 0,
                startedAtEpochMs = snapshot.startedAtEpochMs ?: (now - snapshot.elapsedSeconds * 1000L),
                endedAtEpochMs = now,
            )
            viewModelScope.launch {
                repository.saveFocusSession(session)
                _pendingUndoPayload.value = null
                _snackbarMessage.value = "Sesi fokus (${DateTimeHelper.formatCountdown(snapshot.elapsedSeconds)}) disimpan."
            }
            _celebrationTick.update { it + 1 }
        } else if (snapshot.hasActiveSession) {
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Sesi fokus dihentikan."
        }
        resetFocusTimer()
    }

    fun toggleDistractionFreeMode(enabled: Boolean) {
        _focusTimer.update { it.copy(isDistractionFreeMode = enabled) }
    }

    private fun launchTimerLoop() {
        timerTickerJob?.cancel()
        timerTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _focusTimer.value
                if (!current.isRunning) break
                val nextRem = (current.remainingSeconds - 1).coerceAtLeast(0)
                val nextElapsed = current.elapsedSeconds + 1
                if (nextRem == 0) {
                    val now = System.currentTimeMillis()
                    val session = FocusSession(
                        linkedTaskId = current.linkedTaskId,
                        linkedTaskTitle = current.linkedTaskTitle,
                        plannedMinutes = current.plannedMinutes,
                        actualSeconds = nextElapsed,
                        completedNaturally = true,
                        startedAtEpochMs = current.startedAtEpochMs ?: (now - nextElapsed * 1000L),
                        endedAtEpochMs = now,
                    )
                    repository.saveFocusSession(session)
                    _focusTimer.update {
                        it.copy(
                            remainingSeconds = it.plannedMinutes * 60,
                            elapsedSeconds = 0,
                            isRunning = false,
                            isPaused = false,
                            hasActiveSession = false,
                            isDistractionFreeMode = false,
                            startedAtEpochMs = null,
                        )
                    }
                    _pendingUndoPayload.value = null
                    _snackbarMessage.value = "Mantap! Fokus ${current.plannedMinutes} mnt selesai."
                    _celebrationTick.update { it + 1 }
                    break
                } else {
                    _focusTimer.update {
                        it.copy(remainingSeconds = nextRem, elapsedSeconds = nextElapsed)
                    }
                }
            }
        }
    }

    // --- Photo Recap (with GPS Location Metadata) ---

    fun addPhotosToRecap(dateIso: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val loc = resolveBestRecapLocation()
            val result = repository.addPhotosToRecap(
                dateIso = dateIso,
                uris = uris,
                locationName = loc.first,
                locationLat = loc.second,
                locationLng = loc.third,
            )
            _pendingUndoPayload.value = null
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _celebrationTick.update { it + 1 }
                _snackbarMessage.value = "$count foto masuk ke rekap!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message
                    ?: "Gagal simpan foto. Cek penyimpanan HP."
            }
        }
    }

    fun saveCapturedPhotoToRecap(
        dateIso: String,
        uri: Uri,
        textStyle: PhotoTextStyle,
        textAlignment: PhotoTextAlignment,
        overlayTimeLabel: String,
        title: String,
        caption: String,
    ) {
        viewModelScope.launch {
            val loc = resolveBestRecapLocation()
            val result = repository.addStyledPhotoToRecap(
                dateIso = dateIso,
                uri = uri,
                textStyle = textStyle,
                textAlignment = textAlignment,
                overlayTimeLabel = overlayTimeLabel,
                title = title,
                caption = caption,
                locationName = loc.first,
                locationLat = loc.second,
                locationLng = loc.third,
            )
            _pendingUndoPayload.value = null
            if (result.isSuccess) {
                _celebrationTick.update { it + 1 }
                _snackbarMessage.value = if (loc.first.isNotBlank()) {
                    "Momen disimpan di ${loc.first}!"
                } else {
                    "Momen disimpan ke rekap!"
                }
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message
                    ?: "Gagal menyimpan foto ke rekap."
            }
        }
    }

    private suspend fun resolveBestRecapLocation(): Triple<String, Double?, Double?> {
        if (LocationHelper.hasLocationPermission(getApplication())) {
            val live = LocationHelper.resolveCurrentLocation(getApplication())
            if (live != null) {
                prefsStore.setLastLocation(live.locationName, live.latitude, live.longitude)
                return Triple(live.locationName, live.latitude, live.longitude)
            }
        }
        val state = uiState.value
        return Triple(state.currentLocationName, state.currentLatitude, state.currentLongitude)
    }

    fun removePhotoFromRecap(dateIso: String, path: String) {
        val snapshotRecap = uiState.value.photoRecaps[dateIso]
        viewModelScope.launch {
            // Keep undo snapshot if it was a metadata/path removal (copy to backup not deleted immediately if only 1)
            repository.removePhotoFromRecap(dateIso, path)
            if (snapshotRecap != null && snapshotRecap.imagePaths.size > 1) {
                _pendingUndoPayload.value = UndoPayload.Recap(snapshotRecap)
            } else {
                _pendingUndoPayload.value = null
            }
            _snackbarMessage.value = "Foto dihapus."
        }
    }

    fun saveRecapMeta(dateIso: String, title: String, caption: String) {
        viewModelScope.launch {
            repository.updateRecapMeta(dateIso, title, caption)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Rekap tersimpan."
        }
    }

    fun deleteRecap(dateIso: String) {
        val snapshot = uiState.value.photoRecaps[dateIso]
        viewModelScope.launch {
            // Defer physical file unlink if we offer Undo, or delete DB record and keep Undo restore possible
            repository.deleteRecap(dateIso, deleteImageFiles = false)
            _pendingUndoPayload.value = snapshot?.let { UndoPayload.Recap(it) }
            _snackbarMessage.value = "Rekap tanggal ${DateTimeHelper.formatShortDate(dateIso)} dihapus."
        }
    }

    fun exportRecapCollage(dateIso: String, onShareUriReady: (Uri) -> Unit) {
        val recap = uiState.value.photoRecaps[dateIso]
        if (recap == null || recap.imagePaths.isEmpty()) {
            _snackbarMessage.value = "Belum ada foto di tanggal ini untuk diekspor."
            return
        }
        viewModelScope.launch {
            _isExportingCollage.value = true
            val res = repository.exportRecapCollage(dateIso, recap)
            _isExportingCollage.value = false
            _pendingUndoPayload.value = null
            res.onSuccess { export ->
                _snackbarMessage.value = if (export.savedToGallery) {
                    "Kolase disimpan ke Galeri & siap dibagikan."
                } else {
                    "Kolase siap dibagikan."
                }
                onShareUriReady(export.shareUri)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal membuat kolase foto."
            }
        }
    }

    // --- Data Portability ---

    fun exportJsonToUri(uri: Uri) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val res = repository.exportJson(uri)
            _isBusyBackup.value = false
            _pendingUndoPayload.value = null
            _snackbarMessage.value = res.getOrElse {
                "Gagal mencadangkan data: ${it.localizedMessage ?: "Kesalahan penyimpanan."}"
            }
        }
    }

    fun importJsonFromUri(uri: Uri) {
        prepareImport(uri)
    }

    fun prepareImport(uri: Uri) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val res = repository.inspectBackup(uri)
            _isBusyBackup.value = false
            res.onSuccess { preview ->
                _pendingImportPreview.value = preview
            }.onFailure { err ->
                _pendingUndoPayload.value = null
                _snackbarMessage.value = "Gagal membaca file cadangan: ${err.localizedMessage ?: "File JSON tidak valid."}"
            }
        }
    }

    fun confirmImport(strategy: JsonBackupManager.ImportStrategy) {
        val preview = _pendingImportPreview.value ?: return
        _pendingImportPreview.value = null
        viewModelScope.launch {
            _isBusyBackup.value = true
            val res = repository.executeImport(preview.uri, strategy)
            _isBusyBackup.value = false
            _pendingUndoPayload.value = null
            res.onSuccess { result ->
                _snackbarMessage.value = result.message
                if (result.importedUserName != null && strategy == JsonBackupManager.ImportStrategy.REPLACE) {
                    prefsStore.setUserName(result.importedUserName)
                }
                refreshDynamicServices(tryGpsLocation = false)
            }.onFailure { err ->
                _snackbarMessage.value = "Gagal mengimpor data: ${err.localizedMessage ?: "Terjadi kesalahan."}"
            }
        }
    }

    fun dismissImportPreview() {
        _pendingImportPreview.value = null
    }

    fun resetAllData() {
        viewModelScope.launch {
            uiState.value.allTasksWithStatus.forEach { reminderScheduler.cancelForTask(it.task.id) }
            reminderScheduler.cancelWorshipReminders()
            prefsStore.resetPreferences()
            resetFocusTimer()
            repository.resetAllData()
            WidgetSyncHelper.refreshAllAsync(getApplication(), viewModelScope)
            _pendingUndoPayload.value = null
            _snackbarMessage.value = "Semua data sudah direset."
        }
    }

    private fun filterByScope(
        items: List<TaskWithStatus>,
        scope: ScheduleScope,
        anchorIso: String,
    ): List<TaskWithStatus> {
        return when (scope) {
            ScheduleScope.DAILY -> items.filter { it.task.dateIso == anchorIso }
            ScheduleScope.WEEKLY -> {
                val weekSet = DateTimeHelper.weekDaysFor(anchorIso).toSet()
                items.filter { it.task.dateIso in weekSet }
            }
            ScheduleScope.MONTHLY -> {
                val (start, end) = DateTimeHelper.monthRangeFor(anchorIso)
                items.filter { it.task.dateIso in start..end }
            }
        }
    }

    private fun computeAnalytics(
        allTasks: List<TaskWithStatus>,
        sessions: List<FocusSession>,
        scope: ScheduleScope,
        anchorIso: String,
        streakDays: Int,
        heatmapDays: List<HeatmapDayCell>,
        worshipSummary: com.example.domain.WorshipAnalyticsSummary,
    ): AnalyticsSummary {
        val scopedTasks = filterByScope(
            allTasks,
            if (scope == ScheduleScope.DAILY) ScheduleScope.WEEKLY else scope,
            anchorIso,
        )
        val total = scopedTasks.size
        val completed = scopedTasks.count { it.status == TaskStatus.COMPLETED_GREEN }
        val overdue = scopedTasks.count { it.status == TaskStatus.OVERDUE_DARK }
        val rate = if (total == 0) 0 else ((completed * 100f) / total).toInt()
        val focusMins = (sessions.sumOf { it.actualSeconds } / 60)

        val qCounts = PriorityQuadrant.entries.associateWith { q ->
            scopedTasks.count { it.task.quadrant == q }
        }

        val points = if (scope == ScheduleScope.MONTHLY) {
            val (startIso, endIso) = DateTimeHelper.monthRangeFor(anchorIso)
            val weekAnchors = (0..3).map { w -> DateTimeHelper.addDays(startIso, w * 7) }
            weekAnchors.mapIndexed { idx, wStart ->
                val wEnd = DateTimeHelper.addDays(wStart, 6).coerceAtMost(endIso)
                val weekTasks = allTasks.filter { it.task.dateIso in wStart..wEnd }
                val weekFocusMins = sessions.filter { s ->
                    val iso = DateTimeHelper.calendarToIso(
                        Calendar.getInstance().apply { timeInMillis = s.startedAtEpochMs },
                    )
                    iso in wStart..wEnd
                }.sumOf { it.actualSeconds } / 60
                DailyChartPoint(
                    label = "M${idx + 1}",
                    dateIso = wStart,
                    completedCount = weekTasks.count { it.status == TaskStatus.COMPLETED_GREEN },
                    totalCount = weekTasks.size,
                    focusMinutes = weekFocusMins,
                )
            }
        } else {
            val days = DateTimeHelper.weekDaysFor(anchorIso)
            days.map { dayIso ->
                val dayTasks = allTasks.filter { it.task.dateIso == dayIso }
                val dayFocusMins = sessions.filter { s ->
                    val iso = DateTimeHelper.calendarToIso(
                        Calendar.getInstance().apply { timeInMillis = s.startedAtEpochMs },
                    )
                    iso == dayIso
                }.sumOf { it.actualSeconds } / 60
                DailyChartPoint(
                    label = DateTimeHelper.formatDayShortLabel(dayIso),
                    dateIso = dayIso,
                    completedCount = dayTasks.count { it.status == TaskStatus.COMPLETED_GREEN },
                    totalCount = dayTasks.size,
                    focusMinutes = dayFocusMins,
                )
            }
        }

        return AnalyticsSummary(
            totalTasks = total,
            completedTasks = completed,
            overdueTasks = overdue,
            completionRatePercent = rate,
            totalFocusMinutes = focusMins,
            focusSessionsCount = sessions.size,
            quadrantCounts = qCounts,
            trendPoints = points,
            streakDays = streakDays,
            heatmapDays = heatmapDays,
            worshipSummary = worshipSummary,
        )
    }
}
