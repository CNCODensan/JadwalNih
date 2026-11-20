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
        chipLabelId = "Kritis",
        titleId = "Penting & Mendesak",
        subtitleId = "Kerjain sekarang",
    ),
    Q2_IMPORTANT_NOT_URGENT(
        shortCode = "K2",
        chipLabelId = "Penting",
        titleId = "Penting & Terencana",
        subtitleId = "Atur waktunya",
    ),
    Q3_NOT_IMPORTANT_URGENT(
        shortCode = "K3",
        chipLabelId = "Cepat",
        titleId = "Kurang Penting & Mendesak",
        subtitleId = "Ringkas / delegasi",
    ),
    Q4_NOT_IMPORTANT_NOT_URGENT(
        shortCode = "K4",
        chipLabelId = "Santai",
        titleId = "Santai / Opsional",
        subtitleId = "Kapan sempat",
    ),
}

enum class TaskStatus(val labelId: String, val descriptionId: String) {
    COMPLETED_GREEN("Selesai", "Sudah tuntas"),
    CRITICAL_RED("Mepet Banget", "Tenggat kritis"),
    APPROACHING_YELLOW("Mau Mepet", "Sebentar lagi"),
    FLEXIBLE_BLUE("Fleksibel", "Waktu longgar"),
    PENDING_GRAY("Belum Mulai", "Menunggu jadwal"),
    OVERDUE_DARK("Terlewat", "Lewat batas waktu"),
}

enum class RecurrenceRule(val labelId: String) {
    NONE("Sekali"),
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
}

data class ScheduleTask(
    val id: Long = 0L,
    val title: String,
    val dateIso: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val estimatedMinutes: Int,
    val quadrant: PriorityQuadrant,
    val isCompleted: Boolean = false,
    val isFlexible: Boolean = false,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val reminderEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 10,
    val completedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class TaskWithStatus(
    val task: ScheduleTask,
    val status: TaskStatus,
)

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
)

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
)
