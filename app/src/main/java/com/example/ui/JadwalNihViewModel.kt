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
import com.example.domain.AnalyticsSummary
import com.example.domain.AttendanceStatus
import com.example.domain.ConflictInfo
import com.example.domain.DailyChartPoint
import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.FocusTimerState
import com.example.domain.GreetingState
import com.example.domain.HeatmapDayCell
import com.example.domain.MainTab
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
import com.example.domain.TaskDraft
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.domain.UserPreferences
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class JadwalNihUiState(
    val selectedTab: MainTab = MainTab.JADWAL,
    val selectedScope: ScheduleScope = ScheduleScope.DAILY,
    val selectedDateIso: String = DateTimeHelper.todayIso(),
    val statusFilter: TaskStatus? = null,
    val matrixScope: ScheduleScope = ScheduleScope.DAILY,
    val matrixQuadrantFilter: PriorityQuadrant? = null,
    val analyticsScope: ScheduleScope = ScheduleScope.WEEKLY,
    val selectedRecapDateIso: String = DateTimeHelper.todayIso(),
    val isDarkMode: Boolean = false,
    val userName: String = "",
    val quickGlanceTarget: QuickGlanceTarget = QuickGlanceTarget.ANALITIK,
    val checkInNotificationsEnabled: Boolean = true,
    val taskDraft: TaskDraft? = null,
    val allTasksWithStatus: List<TaskWithStatus> = emptyList(),
    val filteredScheduleTasks: List<TaskWithStatus> = emptyList(),
    val matrixTasks: List<TaskWithStatus> = emptyList(),
    val checkInPromptTask: TaskWithStatus? = null,
    val recentMissedTaskForOverride: TaskWithStatus? = null,
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
        encouragementText = "Mulai dari satu tugas ringan dulu yuk.",
    ),
    val celebrationTick: Int = 0,
    val snackbarMessage: String? = null,
    val isBusyBackup: Boolean = false,
    val isExportingCollage: Boolean = false,
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
    )
    private val repository = JadwalNihRepository(
        taskDao = db.scheduleTaskDao(),
        focusDao = db.focusSessionDao(),
        recapDao = db.photoRecapDao(),
        imageHelper = imageHelper,
        backupManager = backupManager,
    )
    private val reminderScheduler = ReminderScheduler(application)

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
    private val _isBusyBackup = MutableStateFlow(false)
    private val _isExportingCollage = MutableStateFlow(false)
    private val _celebrationTick = MutableStateFlow(0)

    private var timerTickerJob: Job? = null

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

    private data class FeedbackState(
        val snackbar: String?,
        val isBusyBackup: Boolean,
        val isExportingCollage: Boolean,
        val celebrationTick: Int,
    )

    private val feedbackStateFlow = combine(
        _snackbarMessage,
        _isBusyBackup,
        _isExportingCollage,
        _celebrationTick,
    ) { snackbar, isBusy, isExporting, celebrationTick ->
        FeedbackState(snackbar, isBusy, isExporting, celebrationTick)
    }

    val uiState: StateFlow<JadwalNihUiState> = combine(
        dataSnapshotFlow,
        _clockTick,
        _focusTimer,
        feedbackStateFlow,
    ) { snapshot, _, focusState, feedback ->
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

        val analytics = computeAnalytics(
            allTasks = allWithStatus,
            sessions = sessions,
            scope = nav.analyticsScope,
            anchorIso = nav.dateIso,
            streakDays = streakDays,
            heatmapDays = heatmapDays,
        )

        JadwalNihUiState(
            selectedTab = nav.tab,
            selectedScope = nav.scope,
            selectedDateIso = nav.dateIso,
            statusFilter = nav.statusFilter,
            matrixScope = nav.matrixScope,
            matrixQuadrantFilter = nav.matrixQuadrantFilter,
            analyticsScope = nav.analyticsScope,
            selectedRecapDateIso = nav.recapDateIso,
            isDarkMode = prefs.isDarkMode,
            userName = prefs.userName,
            quickGlanceTarget = prefs.quickGlanceTarget,
            checkInNotificationsEnabled = prefs.checkInNotificationsEnabled,
            taskDraft = snapshot.draft,
            allTasksWithStatus = allWithStatus,
            filteredScheduleTasks = filteredSchedule,
            matrixTasks = matrixTasks,
            checkInPromptTask = checkInPromptTask,
            recentMissedTaskForOverride = recentMissedTaskForOverride,
            quickGlance = quickGlance,
            focusSessions = sessions,
            photoRecaps = recapMap,
            focusTimer = focusState,
            analytics = analytics,
            conflictsByTaskId = conflictsByTaskId,
            selectedDateLoadMinutes = selectedDateLoadMinutes,
            selectedDateConflictCount = selectedDateConflictCount,
            greeting = greeting,
            celebrationTick = feedback.celebrationTick,
            snackbarMessage = feedback.snackbar,
            isBusyBackup = feedback.isBusyBackup,
            isExportingCollage = feedback.isExportingCollage,
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

    // --- User Settings & Drafts ---

    fun updateUserName(name: String) {
        viewModelScope.launch {
            prefsStore.setUserName(name)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.setDarkMode(enabled)
        }
    }

    fun setQuickGlanceTarget(target: QuickGlanceTarget) {
        viewModelScope.launch {
            prefsStore.setQuickGlanceTarget(target)
        }
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

    fun saveTaskDraft(draft: TaskDraft) {
        viewModelScope.launch {
            prefsStore.saveDraft(draft)
        }
    }

    fun clearTaskDraft() {
        viewModelScope.launch {
            prefsStore.saveDraft(null)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun saveTask(task: ScheduleTask) {
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
            _snackbarMessage.value = if (task.id == 0L) {
                "Jadwal ditambahkan."
            } else {
                "Jadwal diperbarui."
            }
        }
    }

    fun confirmTaskAttendance(task: ScheduleTask, isAttending: Boolean) {
        viewModelScope.launch {
            if (isAttending) {
                repository.updateAttendance(task, AttendanceStatus.CONFIRMED_ATTENDING, markCompleted = false)
                _snackbarMessage.value = "Mantap! Kehadiran untuk \"${task.title}\" tercatat."
            } else {
                repository.updateAttendance(task, AttendanceStatus.NOT_YET, markCompleted = false)
                _snackbarMessage.value = "Ditandai belum hadir. Kamu tetap bisa konfirmasi nanti."
            }
        }
    }

    fun overrideMissedTaskAsAttended(task: ScheduleTask) {
        viewModelScope.launch {
            repository.updateAttendance(task, AttendanceStatus.CONFIRMED_OVERRIDE, markCompleted = true)
            reminderScheduler.cancelForTask(task.id)
            _celebrationTick.update { it + 1 }
            _snackbarMessage.value = "Status \"${task.title}\" diperbarui menjadi Hadir & Selesai!"
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
            _snackbarMessage.value = when {
                !task.isCompleted && nextRecurring != null ->
                    "Beres! Jadwal berikutnya dibuat (${DateTimeHelper.formatShortDate(nextRecurring.dateIso)})."
                !task.isCompleted -> "Sip, selesai!"
                else -> "Jadwal dibuka lagi."
            }
        }
    }

    fun toggleSubTask(task: ScheduleTask, subTaskId: String) {
        viewModelScope.launch {
            val allJustCompleted = repository.toggleSubTask(task, subTaskId)
            if (allJustCompleted) {
                _celebrationTick.update { it + 1 }
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
            _snackbarMessage.value = "Dipindah ke ${DateTimeHelper.formatShortDate(moved.dateIso)}."
        }
    }

    fun changeTaskQuadrant(task: ScheduleTask, quadrant: PriorityQuadrant) {
        viewModelScope.launch {
            repository.updateTaskQuadrant(task, quadrant)
            _snackbarMessage.value = "Dipindah ke ${quadrant.shortCode} • ${quadrant.chipLabelId}."
        }
    }

    fun deleteTask(task: ScheduleTask) {
        viewModelScope.launch {
            reminderScheduler.cancelForTask(task.id)
            repository.deleteTask(task.id)
            _snackbarMessage.value = "Jadwal dihapus."
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
                it.copy(validationError = "Durasi harus antara 1 sampai 240 menit sebelum memulai.")
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
                _snackbarMessage.value = "Sesi fokus (${DateTimeHelper.formatCountdown(snapshot.elapsedSeconds)}) disimpan."
            }
            _celebrationTick.update { it + 1 }
        } else if (snapshot.hasActiveSession) {
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

    // --- Photo Recap ---

    fun addPhotosToRecap(dateIso: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = repository.addPhotosToRecap(dateIso, uris)
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _celebrationTick.update { it + 1 }
                _snackbarMessage.value = "$count foto masuk ke rekap!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message
                    ?: "Gagal simpan foto. Cek ruang penyimpanan."
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
            val result = repository.addStyledPhotoToRecap(
                dateIso = dateIso,
                uri = uri,
                textStyle = textStyle,
                textAlignment = textAlignment,
                overlayTimeLabel = overlayTimeLabel,
                title = title,
                caption = caption,
            )
            if (result.isSuccess) {
                _celebrationTick.update { it + 1 }
                _snackbarMessage.value = "Momen disimpan ke rekap!"
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message
                    ?: "Gagal menyimpan foto ke rekap."
            }
        }
    }

    fun removePhotoFromRecap(dateIso: String, path: String) {
        viewModelScope.launch {
            repository.removePhotoFromRecap(dateIso, path)
            _snackbarMessage.value = "Foto dihapus."
        }
    }

    fun saveRecapMeta(dateIso: String, title: String, caption: String) {
        viewModelScope.launch {
            repository.updateRecapMeta(dateIso, title, caption)
            _snackbarMessage.value = "Rekap tersimpan."
        }
    }

    fun deleteRecap(dateIso: String) {
        viewModelScope.launch {
            repository.deleteRecap(dateIso)
            _snackbarMessage.value = "Rekap dihapus."
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
            _snackbarMessage.value = res.getOrElse {
                "Gagal mengekspor data: ${it.localizedMessage ?: "Terjadi kesalahan penyimpanan."}"
            }
        }
    }

    fun importJsonFromUri(uri: Uri) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val res = repository.importJson(uri)
            _isBusyBackup.value = false
            _snackbarMessage.value = res.getOrElse {
                "Gagal mengimpor data: ${it.localizedMessage ?: "Berkas JSON tidak valid."}"
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            uiState.value.allTasksWithStatus.forEach { reminderScheduler.cancelForTask(it.task.id) }
            prefsStore.resetPreferences()
            resetFocusTimer()
            repository.resetAllData()
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
        )
    }
}
