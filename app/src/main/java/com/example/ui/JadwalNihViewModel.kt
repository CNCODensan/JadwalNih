package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ImageStorageHelper
import com.example.data.JadwalNihRepository
import com.example.data.JsonBackupManager
import com.example.data.PreferencesManager
import com.example.data.UserPreferences
import com.example.domain.AnalyticsSummary
import com.example.domain.DailyChartPoint
import com.example.domain.DateTimeHelper
import com.example.domain.FocusSession
import com.example.domain.FocusTimerState
import com.example.domain.MainTab
import com.example.domain.PhotoRecap
import com.example.domain.PriorityQuadrant
import com.example.domain.QuickGlanceSummary
import com.example.domain.ScheduleScope
import com.example.domain.ScheduleTask
import com.example.domain.StatusEvaluator
import com.example.domain.TaskStatus
import com.example.domain.TaskWithStatus
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JadwalNihUiState(
    val selectedTab: MainTab = MainTab.JADWAL,
    val selectedScope: ScheduleScope = ScheduleScope.DAILY,
    val selectedDateIso: String = DateTimeHelper.todayIso(),
    val statusFilter: TaskStatus? = null,
    val matrixScope: ScheduleScope = ScheduleScope.WEEKLY,
    val analyticsScope: ScheduleScope = ScheduleScope.WEEKLY,
    val selectedRecapDateIso: String = DateTimeHelper.todayIso(),
    val isDarkMode: Boolean = false,
    val allTasksWithStatus: List<TaskWithStatus> = emptyList(),
    val filteredScheduleTasks: List<TaskWithStatus> = emptyList(),
    val matrixTasks: List<TaskWithStatus> = emptyList(),
    val quickGlance: QuickGlanceSummary = QuickGlanceSummary(),
    val focusSessions: List<FocusSession> = emptyList(),
    val photoRecaps: Map<String, PhotoRecap> = emptyMap(),
    val focusTimer: FocusTimerState = FocusTimerState(),
    val analytics: AnalyticsSummary = AnalyticsSummary(),
    val snackbarMessage: String? = null,
    val isBusyBackup: Boolean = false,
)

class JadwalNihViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefsManager = PreferencesManager(application)
    private val imageHelper = ImageStorageHelper(application)
    private val backupManager = JsonBackupManager(
        context = application,
        taskDao = db.scheduleTaskDao(),
        focusDao = db.focusSessionDao(),
        recapDao = db.photoRecapDao(),
        prefsManager = prefsManager,
        imageHelper = imageHelper,
    )
    private val repository = JadwalNihRepository(
        taskDao = db.scheduleTaskDao(),
        focusDao = db.focusSessionDao(),
        recapDao = db.photoRecapDao(),
        prefsManager = prefsManager,
        imageHelper = imageHelper,
        backupManager = backupManager,
    )
    private val reminderScheduler = ReminderScheduler(application)

    private val _selectedTab = MutableStateFlow(MainTab.JADWAL)
    private val _selectedScope = MutableStateFlow(ScheduleScope.DAILY)
    private val _selectedDateIso = MutableStateFlow(DateTimeHelper.todayIso())
    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    private val _matrixScope = MutableStateFlow(ScheduleScope.WEEKLY)
    private val _analyticsScope = MutableStateFlow(ScheduleScope.WEEKLY)
    private val _selectedRecapDateIso = MutableStateFlow(DateTimeHelper.todayIso())
    private val _clockTick = MutableStateFlow(System.currentTimeMillis())
    private val _focusTimer = MutableStateFlow(FocusTimerState())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isBusyBackup = MutableStateFlow(false)

    private var timerTickerJob: Job? = null

    private data class NavFilterParams(
        val tab: MainTab,
        val scope: ScheduleScope,
        val dateIso: String,
        val statusFilter: TaskStatus?,
        val matrixScope: ScheduleScope,
        val analyticsScope: ScheduleScope,
        val recapDateIso: String,
    )

    private val navParamsFlow = combine(
        combine(_selectedTab, _selectedScope, _selectedDateIso, _statusFilter) { tab, scope, dateIso, statusFilter ->
            QuadFilter(tab, scope, dateIso, statusFilter)
        },
        combine(_matrixScope, _analyticsScope, _selectedRecapDateIso) { matrixScope, analyticsScope, recapDateIso ->
            Triple(matrixScope, analyticsScope, recapDateIso)
        },
    ) { left, right ->
        NavFilterParams(
            tab = left.tab,
            scope = left.scope,
            dateIso = left.dateIso,
            statusFilter = left.statusFilter,
            matrixScope = right.first,
            analyticsScope = right.second,
            recapDateIso = right.third,
        )
    }

    private data class QuadFilter(
        val tab: MainTab,
        val scope: ScheduleScope,
        val dateIso: String,
        val statusFilter: TaskStatus?,
    )

    private data class DataSnapshot(
        val tasks: List<ScheduleTask>,
        val sessions: List<FocusSession>,
        val recaps: List<PhotoRecap>,
        val prefs: UserPreferences,
        val nav: NavFilterParams,
    )

    private val dataSnapshotFlow = combine(
        repository.tasksFlow,
        repository.focusSessionsFlow,
        repository.photoRecapsFlow,
        repository.preferencesFlow,
        navParamsFlow,
    ) { tasks, sessions, recaps, prefs, nav ->
        DataSnapshot(tasks, sessions, recaps, prefs, nav)
    }

    val uiState: StateFlow<JadwalNihUiState> = combine(
        dataSnapshotFlow,
        _clockTick,
        _focusTimer,
        _snackbarMessage,
        _isBusyBackup,
    ) { snapshot, _, focusState, snackbar, isBusy ->
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

        val matrixTasks = filterByScope(allWithStatus, nav.matrixScope, nav.dateIso)
            .filterNot { it.task.isCompleted }

        val recapMap = recaps.associateBy { it.dateIso }
        val todayTasks = allWithStatus.filter { it.task.dateIso == todayIso }
        val todayUpcoming = todayTasks.filterNot { it.task.isCompleted }
            .sortedBy { it.task.startMinuteOfDay }
        val nextTask = todayUpcoming.firstOrNull {
            it.status != TaskStatus.OVERDUE_DARK
        } ?: todayUpcoming.firstOrNull()

        val urgentCount = todayTasks.count {
            !it.task.isCompleted && (
                it.task.quadrant == PriorityQuadrant.Q1_IMPORTANT_URGENT ||
                    it.status == TaskStatus.CRITICAL_RED ||
                    it.status == TaskStatus.APPROACHING_YELLOW
                )
        }

        val quickGlance = QuickGlanceSummary(
            todayTotalCount = todayTasks.size,
            todayCompletedCount = todayTasks.count { it.task.isCompleted },
            todayActiveOrUpcomingCount = todayUpcoming.size,
            urgentOrCriticalCount = urgentCount,
            nextTaskTitle = nextTask?.task?.title,
            nextTaskTimeLabel = nextTask?.let {
                DateTimeHelper.formatTimeRange(it.task.startMinuteOfDay, it.task.endMinuteOfDay)
            },
            todayRecap = recapMap[todayIso],
        )

        val analytics = computeAnalytics(allWithStatus, sessions, nav.analyticsScope, nav.dateIso)

        JadwalNihUiState(
            selectedTab = nav.tab,
            selectedScope = nav.scope,
            selectedDateIso = nav.dateIso,
            statusFilter = nav.statusFilter,
            matrixScope = nav.matrixScope,
            analyticsScope = nav.analyticsScope,
            selectedRecapDateIso = nav.recapDateIso,
            isDarkMode = prefs.isDarkMode,
            allTasksWithStatus = allWithStatus,
            filteredScheduleTasks = filteredSchedule,
            matrixTasks = matrixTasks,
            quickGlance = quickGlance,
            focusSessions = sessions,
            photoRecaps = recapMap,
            focusTimer = focusState,
            analytics = analytics,
            snackbarMessage = snackbar,
            isBusyBackup = isBusy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JadwalNihUiState(),
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            val prefs = prefsManager.getPreferencesOnce()
            val mins = prefs.lastCustomFocusMinutes.coerceIn(1, 240)
            _focusTimer.update {
                it.copy(
                    customMinutesInput = mins.toString(),
                    plannedMinutes = mins,
                    remainingSeconds = mins * 60,
                )
            }
        }
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

    fun stepDate(direction: Int) {
        val current = _selectedDateIso.value
        _selectedDateIso.value = when (_selectedScope.value) {
            ScheduleScope.DAILY -> DateTimeHelper.addDays(current, direction)
            ScheduleScope.WEEKLY -> DateTimeHelper.addDays(current, direction * 7)
            ScheduleScope.MONTHLY -> DateTimeHelper.addMonths(current, direction)
        }
    }

    fun jumpToToday() {
        _selectedDateIso.value = DateTimeHelper.todayIso()
    }

    fun setStatusFilter(status: TaskStatus?) {
        _statusFilter.value = if (_statusFilter.value == status) null else status
    }

    fun setMatrixScope(scope: ScheduleScope) {
        _matrixScope.value = scope
    }

    fun setAnalyticsScope(scope: ScheduleScope) {
        _analyticsScope.value = scope
    }

    fun selectRecapDate(dateIso: String) {
        _selectedRecapDateIso.value = dateIso
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val next = !uiState.value.isDarkMode
            repository.setDarkMode(next)
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
            if (persisted.reminderEnabled && !persisted.isCompleted) {
                reminderScheduler.scheduleForTask(persisted)
            } else {
                reminderScheduler.cancelForTask(persisted.id)
            }
            _snackbarMessage.value = if (task.id == 0L) {
                "Jadwal ditambahkan."
            } else {
                "Jadwal diperbarui."
            }
        }
    }

    fun toggleTaskComplete(task: ScheduleTask) {
        viewModelScope.launch {
            val nextRecurring = repository.toggleTaskComplete(task)
            if (!task.isCompleted) {
                reminderScheduler.cancelForTask(task.id)
            } else if (task.reminderEnabled) {
                reminderScheduler.scheduleForTask(task.copy(isCompleted = false))
            }
            if (nextRecurring != null && nextRecurring.reminderEnabled) {
                reminderScheduler.scheduleForTask(nextRecurring)
            }
            _snackbarMessage.value = when {
                !task.isCompleted && nextRecurring != null ->
                    "Beres! Jadwal berikutnya dibuat (${DateTimeHelper.formatShortDate(nextRecurring.dateIso)})."
                !task.isCompleted -> "Sip, selesai!"
                else -> "Jadwal dibuka lagi."
            }
        }
    }

    fun moveTaskToTomorrow(task: ScheduleTask) {
        viewModelScope.launch {
            val moved = repository.moveTaskToTomorrow(task)
            if (moved.reminderEnabled) {
                reminderScheduler.scheduleForTask(moved)
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
        if (error == null) {
            viewModelScope.launch { repository.setLastFocusMinutes(validMinutes) }
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
                _snackbarMessage.value = "$count foto ditambahkan."
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.message
                    ?: "Gagal simpan foto. Cek ruang penyimpanan."
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
    ): AnalyticsSummary {
        val scopedTasks = filterByScope(
            allTasks,
            if (scope == ScheduleScope.DAILY) ScheduleScope.WEEKLY else scope,
            anchorIso,
        )
        val total = scopedTasks.size
        val completed = scopedTasks.count { it.task.isCompleted }
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
                        java.util.Calendar.getInstance().apply { timeInMillis = s.startedAtEpochMs },
                    )
                    iso in wStart..wEnd
                }.sumOf { it.actualSeconds } / 60
                DailyChartPoint(
                    label = "M${idx + 1}",
                    dateIso = wStart,
                    completedCount = weekTasks.count { it.task.isCompleted },
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
                        java.util.Calendar.getInstance().apply { timeInMillis = s.startedAtEpochMs },
                    )
                    iso == dayIso
                }.sumOf { it.actualSeconds } / 60
                DailyChartPoint(
                    label = DateTimeHelper.formatDayShortLabel(dayIso),
                    dateIso = dayIso,
                    completedCount = dayTasks.count { it.task.isCompleted },
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
        )
    }
}
