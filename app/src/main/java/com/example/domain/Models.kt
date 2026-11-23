package com.example.domain

enum class MainTab(val label: String) {
    JADWAL("Jadwal"),
    MATRIKS("Matriks"),
    FOKUS("Fokus"),
    REKAP("Rekap"),
    ANALITIK("Analitik"),
}

enum class ScheduleScope(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
}

enum class PriorityQuadrant(
    val shortCode: String,
    val chipLabelId: String,
    val titleId: String,
    val subtitleId: String,
) {
    Q1_IMPORTANT_URGENT(
        shortCode = "K1",
        chipLabelId = "Penting Banget",
        titleId = "Penting & Harus Segera",
        subtitleId = "Penting, Mendesak",
    ),
    Q2_IMPORTANT_NOT_URGENT(
        shortCode = "K2",
        chipLabelId = "Penting",
        titleId = "Penting & Terjadwal",
        subtitleId = "Penting, Kapan aja",
    ),
    Q3_NOT_IMPORTANT_URGENT(
        shortCode = "K3",
        chipLabelId = "Cepat",
        titleId = "Biasa Tapi Mendesak",
        subtitleId = "Ga Penting/Biasa, Mendesak",
    ),
    Q4_NOT_IMPORTANT_NOT_URGENT(
        shortCode = "K4",
        chipLabelId = "Santai",
        titleId = "Santai / Opsional",
        subtitleId = "Ga penting/Biasa, Kapan aja",
    ),
}

enum class TaskStatus(val labelId: String, val descriptionId: String) {
    COMPLETED_GREEN("Selesai", "Sudah beres"),
    CRITICAL_RED("Mepet Banget", "Waktunya hampir habis"),
    APPROACHING_YELLOW("Sebentar Lagi", "Mau mulai"),
    FLEXIBLE_BLUE("Fleksibel", "Jamnya santai"),
    PENDING_GRAY("Menunggu", "Belum mulai"),
    OVERDUE_DARK("Terlewat", "Sudah lewat jamnya"),
}

enum class AttendanceStatus(val labelId: String) {
    UNCONFIRMED("Belum Cek"),
    CONFIRMED_ATTENDING("Lagi Ikut"),
    NOT_YET("Belum Ikut"),
    CONFIRMED_OVERRIDE("Sudah Ikut"),
}

enum class QuickGlanceTarget(val labelId: String) {
    ANALITIK("Analitik"),
    MATRIKS("Matriks"),
}

enum class RecurrenceRule(val labelId: String) {
    NONE("Sekali"),
    DAILY("Tiap Hari"),
    WEEKLY("Tiap Minggu"),
    MONTHLY("Tiap Bulan"),
}

enum class DurationCategory(val labelId: String) {
    SHORT("Singkat"),
    MEDIUM("Sedang"),
    LONG("Panjang"),
}

enum class ThemeMode(val labelId: String) {
    SYSTEM("Ikut HP"),
    LIGHT("Terang"),
    DARK("Gelap"),
}

enum class ReligionOption(
    val labelId: String,
    val shortHint: String,
    val usesIslamicPrayerTimes: Boolean,
) {
    ISLAM("Islam", "5 waktu sholat harian", true),
    KRISTEN("Kristen", "Doa pagi, malam & ibadah Minggu", false),
    KATOLIK("Katolik", "Doa pagi, malam & Misa Minggu", false),
    HINDU("Hindu", "Tri Sandhya & sembahyang mingguan", false),
    BUDDHA("Buddha", "Puja pagi, malam & Kebaktian Vihara", false),
    KONGHUCU("Konghucu", "Doa harian & sembahyang Klenteng", false),
    LAINNYA("Lainnya", "Refleksi pagi, malam & ibadah mingguan", false);

    companion object {
        fun fromStoredName(raw: String?): ReligionOption {
            return entries.firstOrNull { it.name == raw } ?: ISLAM
        }
    }
}

enum class HolidayThemeType(val labelId: String) {
    NONE("Tema Normal"),
    INDEPENDENCE_DAY("Kemerdekaan RI"),
    IDUL_FITRI("Idul Fitri / Hari Besar Islam"),
    IMLEK("Tahun Baru Imlek"),
    NATAL("Hari Raya Natal"),
    NYEPI_WAISAK("Nyepi / Waisak");

    companion object {
        fun fromStoredName(raw: String?): HolidayThemeType {
            return entries.firstOrNull { it.name == raw } ?: NONE
        }
    }
}

enum class NotificationCategory(val labelId: String) {
    ALL("Semua"),
    DRAFT("Draf"),
    TASK("Tugas"),
    WORSHIP("Ibadah"),
    HOLIDAY("Hari Raya"),
}

enum class AiConflictResolution {
    OVERWRITE_EXISTING,
    KEEP_EXISTING_SKIP_NEW,
    KEEP_BOTH_CONFLICTING,
}

enum class WidgetQuadrantFilter(
    val chipLabel: String,
    val shortLabel: String,
    val fullLabel: String,
    val defaultQuickAddQuadrant: PriorityQuadrant,
) {
    FOCUS_K1_K2(
        chipLabel = "Fokus",
        shortLabel = "Fokus K1–K2",
        fullLabel = "Fokus K1 & K2",
        defaultQuickAddQuadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
    ),
    K1_ONLY(
        chipLabel = "K1",
        shortLabel = "K1 Penting",
        fullLabel = "K1 • Penting & Harus Segera",
        defaultQuickAddQuadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
    ),
    K2_ONLY(
        chipLabel = "K2",
        shortLabel = "K2 Terjadwal",
        fullLabel = "K2 • Penting & Terjadwal",
        defaultQuickAddQuadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
    ),
    K3_ONLY(
        chipLabel = "K3",
        shortLabel = "K3 Cepat",
        fullLabel = "K3 • Biasa Tapi Mendesak",
        defaultQuickAddQuadrant = PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT,
    ),
    K4_ONLY(
        chipLabel = "K4",
        shortLabel = "K4 Santai",
        fullLabel = "K4 • Santai / Opsional",
        defaultQuickAddQuadrant = PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT,
    ),
    ALL(
        chipLabel = "Semua",
        shortLabel = "Semua K1–K4",
        fullLabel = "Semua Prioritas",
        defaultQuickAddQuadrant = PriorityQuadrant.Q1_IMPORTANT_URGENT,
    );

    fun next(): WidgetQuadrantFilter {
        val list = entries
        return list[(ordinal + 1) % list.size]
    }

    fun matches(quadrant: PriorityQuadrant): Boolean = when (this) {
        FOCUS_K1_K2 -> quadrant == PriorityQuadrant.Q1_IMPORTANT_URGENT || quadrant == PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT
        K1_ONLY -> quadrant == PriorityQuadrant.Q1_IMPORTANT_URGENT
        K2_ONLY -> quadrant == PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT
        K3_ONLY -> quadrant == PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT
        K4_ONLY -> quadrant == PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT
        ALL -> true
    }

    companion object {
        fun fromStoredName(raw: String?): WidgetQuadrantFilter {
            return entries.firstOrNull { it.name == raw } ?: FOCUS_K1_K2
        }
    }
}

data class WorshipScheduleItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val minuteOfDay: Int,
    val dateIso: String,
    val isCompleted: Boolean = false,
    val isWeeklySpecial: Boolean = false,
) {
    val formattedTime: String
        get() = DateTimeHelper.formatMinuteOfDay(minuteOfDay)
}

data class WorshipAnalyticsSummary(
    val religion: ReligionOption = ReligionOption.ISLAM,
    val enabled: Boolean = true,
    val completedThisWeek: Int = 0,
    val expectedThisWeek: Int = 0,
    val completedToday: Int = 0,
    val totalToday: Int = 0,
) {
    val consistencyPercent: Int
        get() = if (expectedThisWeek <= 0) 0 else ((completedThisWeek * 100f) / expectedThisWeek).toInt().coerceIn(0, 100)

    val worshipEnabled: Boolean get() = enabled
    val weeklyCompletionRatePercent: Int get() = consistencyPercent
    val todayCompletedCount: Int get() = completedToday
    val todayTotalCount: Int get() = totalToday
    val weeklyCompletedCount: Int get() = completedThisWeek
    val weeklyTotalCount: Int get() = expectedThisWeek
}

data class HolidayInfo(
    val dateIso: String,
    val name: String,
    val isNationalHoliday: Boolean = true,
    val isCutiBersama: Boolean = false,
    val themeType: HolidayThemeType = HolidayThemeType.NONE,
    val bannerGreeting: String = "",
)

data class NotificationLogEntry(
    val id: Long = 0L,
    val category: NotificationCategory,
    val title: String,
    val message: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedTaskId: Long? = null,
    val draftJson: String? = null,
)

data class ForeshadowOccurrence(
    val parentTask: ScheduleTask,
    val occurrenceDateIso: String,
    val weekOffset: Int,
) {
    val weeksAhead: Int get() = weekOffset
}

data class WidgetPrioritySummary(
    val todayDateIso: String = DateTimeHelper.todayIso(),
    val filter: WidgetQuadrantFilter = WidgetQuadrantFilter.FOCUS_K1_K2,
    val activeTasks: List<TaskWithStatus> = emptyList(),
    val completedInFilterCount: Int = 0,
    val totalTodayCount: Int = 0,
    val totalTodayCompletedCount: Int = 0,
    val nextWorship: WorshipScheduleItem? = null,
    val worshipCountDownLabel: String = "",
    val worshipEnabled: Boolean = false,
)

data class DurationProgressInfo(
    val category: DurationCategory,
    val totalMinutes: Int,
    val isOngoing: Boolean = false,
    val elapsedFraction: Float = 0f,
    val remainingMinutes: Int = totalMinutes,
    val statusLabel: String = DateTimeHelper.formatDuration(totalMinutes),
)

data class SubTaskItem(
    val id: String,
    val title: String,
    val isDone: Boolean = false,
)

data class ConflictInfo(
    val conflictingTitles: List<String>,
    val summaryText: String,
)

data class HeatmapDayCell(
    val dateIso: String,
    val completedCount: Int,
    val focusMinutes: Int,
    val level: Int,
    val isFuture: Boolean = false,
)

data class GreetingState(
    val greetingText: String,
    val encouragementText: String,
)

data class ScheduleTask(
    val id: Long = 0L,
    val title: String,
    val dateIso: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val estimatedMinutes: Int = (endMinuteOfDay - startMinuteOfDay).coerceAtLeast(1),
    val quadrant: PriorityQuadrant,
    val isCompleted: Boolean = false,
    val isFlexible: Boolean = false,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val reminderEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 10,
    val completedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val subtasks: List<SubTaskItem> = emptyList(),
    val attendanceStatus: AttendanceStatus = AttendanceStatus.UNCONFIRMED,
    val isSubtasksTodoMode: Boolean = false,
    val notes: String = "",
    val iconKey: String = "",
    val locationName: String = "",
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val skippedOccurrenceDates: Set<String> = emptySet(),
)

data class TaskWithStatus(
    val task: ScheduleTask,
    val status: TaskStatus,
)

data class TaskDraft(
    val editingTaskId: Long? = null,
    val title: String = "",
    val dateIso: String = DateTimeHelper.todayIso(),
    val startMinuteOfDay: Int = 9 * 60,
    val endMinuteOfDay: Int = 10 * 60,
    val quadrant: PriorityQuadrant = PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT,
    val isFlexible: Boolean = false,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val reminderEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 10,
    val subtasks: List<SubTaskItem> = emptyList(),
    val isSubtasksTodoMode: Boolean = false,
    val notes: String = "",
    val aiRawText: String = "",
    val isOtomatisMode: Boolean = true,
    val iconKey: String = "",
    val locationName: String = "",
    val locationLat: Double? = null,
    val locationLng: Double? = null,
) {
    fun hasMeaningfulContent(): Boolean {
        return title.isNotBlank() ||
            notes.isNotBlank() ||
            subtasks.isNotEmpty() ||
            aiRawText.isNotBlank() ||
            locationName.isNotBlank()
    }
}

data class UserPreferences(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val quickGlanceTarget: QuickGlanceTarget = QuickGlanceTarget.ANALITIK,
    val checkInNotificationsEnabled: Boolean = true,
    val isSimpleMode: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val skippedRescheduleDateIso: String = "",
    val skippedRescheduleTaskIds: Set<Long> = emptySet(),
    val religion: ReligionOption = ReligionOption.ISLAM,
    val prayerReminderEnabled: Boolean = true,
    val worshipNotificationSoundEnabled: Boolean = true,
    val holidayThemeEnabled: Boolean = true,
    val holidayThemePreview: HolidayThemeType = HolidayThemeType.NONE,
    val hasSeenV1WelcomeCard: Boolean = false,
    val hasPromptedInitialLocation: Boolean = false,
    val lastLocationName: String = "",
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
)

sealed interface DailyTimelineEntry {
    data class HourSlotEmpty(
        val hour: Int,
        val label: String,
    ) : DailyTimelineEntry

    data class TaskBlock(
        val item: TaskWithStatus,
        val durationMinutes: Int,
    ) : DailyTimelineEntry

    data class GapBreak(
        val startMinuteOfDay: Int,
        val endMinuteOfDay: Int,
        val durationMinutes: Int,
    ) : DailyTimelineEntry
}

data class FocusSession(
    val id: Long = 0L,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String? = null,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val completedNaturally: Boolean,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
)

data class PhotoRecap(
    val dateIso: String,
    val title: String,
    val caption: String,
    val imagePaths: List<String>,
    val updatedAtEpochMs: Long,
    val locationName: String = "",
    val locationLat: Double? = null,
    val locationLng: Double? = null,
)

enum class PhotoTextAlignment(val label: String) {
    LEFT("Kiri"),
    CENTER("Tengah"),
    RIGHT("Kanan"),
}

enum class PhotoTextStyle(val label: String) {
    TIME_PILL("Kapsul Jam"),
    CAPTION_BOTTOM("Judul & Jam"),
    NONE("Polos"),
}

data class FocusTimerState(
    val customMinutesInput: String = "25",
    val validationError: String? = null,
    val plannedMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val hasActiveSession: Boolean = false,
    val linkedTaskId: Long? = null,
    val linkedTaskTitle: String? = null,
    val isDistractionFreeMode: Boolean = false,
    val startedAtEpochMs: Long? = null,
)

data class QuickGlanceSummary(
    val todayTotalCount: Int = 0,
    val todayCompletedCount: Int = 0,
    val todayActiveOrUpcomingCount: Int = 0,
    val urgentOrCriticalCount: Int = 0,
    val nextTaskTitle: String? = null,
    val nextTaskTimeLabel: String? = null,
    val todayRecap: PhotoRecap? = null,
    val streakDays: Int = 0,
)

data class DailyChartPoint(
    val label: String,
    val dateIso: String,
    val completedCount: Int,
    val totalCount: Int,
    val focusMinutes: Int,
)

data class AnalyticsSummary(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionRatePercent: Int = 0,
    val totalFocusMinutes: Int = 0,
    val focusSessionsCount: Int = 0,
    val quadrantCounts: Map<PriorityQuadrant, Int> = emptyMap(),
    val trendPoints: List<DailyChartPoint> = emptyList(),
    val streakDays: Int = 0,
    val heatmapDays: List<HeatmapDayCell> = emptyList(),
    val worshipSummary: WorshipAnalyticsSummary = WorshipAnalyticsSummary(),
)
