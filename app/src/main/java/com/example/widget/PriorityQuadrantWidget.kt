package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ImageStorageHelper
import com.example.data.JadwalNihRepository
import com.example.data.JsonBackupManager
import com.example.data.UserPreferencesStore
import com.example.data.toDomain
import com.example.domain.DateTimeHelper
import com.example.domain.MainTab
import com.example.domain.PrayerTimesService
import com.example.domain.ReligionOption
import com.example.domain.ScheduleAnalyzer
import com.example.domain.UserPreferences
import com.example.domain.WidgetQuadrantFilter
import com.example.domain.WorshipScheduleItem
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PriorityQuadrantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PriorityQuadrantWidget()
}

class PriorityQuadrantWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val taskDao = db.scheduleTaskDao()
        val worshipCheckinDao = db.worshipCheckinDao()
        val prefsStore = UserPreferencesStore(context)

        val initialEntities = withContext(Dispatchers.IO) {
            taskDao.getAllTasksOnce()
        }
        val initialCheckins = withContext(Dispatchers.IO) {
            worshipCheckinDao.getAllCheckinsOnce()
        }
        val initialUserPrefs = withContext(Dispatchers.IO) {
            prefsStore.preferencesFlow.first()
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val filter = WidgetQuadrantFilter.fromStoredName(prefs[KEY_QUADRANT_FILTER])
            val liveEntities by taskDao.observeAllTasks().collectAsState(initial = initialEntities)
            val liveCheckins by worshipCheckinDao.observeAllCheckins().collectAsState(initial = initialCheckins)
            val userPrefs by prefsStore.preferencesFlow.collectAsState(initial = initialUserPrefs)

            val tasks = liveEntities.map { it.toDomain() }
            val todayIso = DateTimeHelper.todayIso()
            val nowMinute = DateTimeHelper.nowMinuteOfDay()

            val completedWorshipKeys = liveCheckins
                .filter { it.isCompleted }
                .map { it.dateItemKey }
                .toSet()
            val todayWorship = if (userPrefs.prayerReminderEnabled) {
                PrayerTimesService.getCachedOrOfflineWorshipSchedule(
                    dateIso = todayIso,
                    religion = userPrefs.religion,
                    latitude = userPrefs.lastLatitude,
                    longitude = userPrefs.lastLongitude,
                    completedKeys = completedWorshipKeys,
                    locationName = userPrefs.lastLocationName,
                )
            } else {
                emptyList()
            }
            val nextWorship = todayWorship.firstOrNull { !it.isCompleted && it.minuteOfDay >= nowMinute }
                ?: todayWorship.firstOrNull { !it.isCompleted }
            val countdownLabel = if (nextWorship != null) {
                val diff = nextWorship.minuteOfDay - nowMinute
                when {
                    diff > 0 -> "${DateTimeHelper.formatDuration(diff)} lagi"
                    diff >= -30 -> "Sekarang"
                    else -> "Terjadwal"
                }
            } else {
                ""
            }

            val summary = ScheduleAnalyzer.buildWidgetPrioritySummary(
                tasks = tasks,
                filter = filter,
                todayIso = todayIso,
                nowMinuteOfDay = nowMinute,
                nextWorship = nextWorship,
                worshipCountDownLabel = countdownLabel,
                worshipEnabled = userPrefs.prayerReminderEnabled,
            )

            val quickAddIntent = Intent(context, QuickAddWidgetActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(
                    QuickAddWidgetActivity.EXTRA_DEFAULT_QUADRANT,
                    filter.defaultQuickAddQuadrant.name,
                )
            }
            val openJadwalIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetSyncHelper.EXTRA_OPEN_TAB, MainTab.JADWAL.name)
            }

            GlanceTheme {
                PriorityQuadrantWidgetContent(
                    summary = summary,
                    onCycleFilter = actionRunCallback<CycleWidgetQuadrantAction>(),
                    onSelectFilter = { selected ->
                        actionRunCallback<SelectWidgetQuadrantAction>(
                            actionParametersOf(SelectWidgetQuadrantAction.KEY_FILTER_NAME to selected.name),
                        )
                    },
                    onToggleTask = { taskId ->
                        actionRunCallback<ToggleWidgetTaskAction>(
                            actionParametersOf(ToggleWidgetTaskAction.KEY_TASK_ID to taskId),
                        )
                    },
                    onQuickAdd = actionStartActivity(quickAddIntent),
                    onOpenApp = actionStartActivity(openJadwalIntent),
                    onToggleWorship = { worshipItem ->
                        actionRunCallback<ToggleWidgetWorshipAction>(
                            actionParametersOf(
                                ToggleWidgetWorshipAction.KEY_WORSHIP_DATE to worshipItem.dateIso,
                                ToggleWidgetWorshipAction.KEY_WORSHIP_KEY to worshipItem.key,
                                ToggleWidgetWorshipAction.KEY_WORSHIP_TITLE to worshipItem.title,
                                ToggleWidgetWorshipAction.KEY_WORSHIP_MINUTE to worshipItem.minuteOfDay,
                                ToggleWidgetWorshipAction.KEY_RELIGION_NAME to userPrefs.religion.name,
                            ),
                        )
                    },
                )
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Widget JadwalNih belum dapat dimuat")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    companion object {
        val KEY_QUADRANT_FILTER = stringPreferencesKey("widget_quadrant_filter")
        val KEY_REVISION = intPreferencesKey("widget_revision")

        val COMPACT_SIZE = DpSize(130.dp, 120.dp)
        val MEDIUM_SIZE = DpSize(270.dp, 148.dp)
        val EXPANDED_SIZE = DpSize(290.dp, 260.dp)

        private val RESPONSIVE_SIZES = setOf(
            COMPACT_SIZE,
            MEDIUM_SIZE,
            EXPANDED_SIZE,
        )
    }
}

class ToggleWidgetTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[KEY_TASK_ID] ?: return
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val taskDao = db.scheduleTaskDao()
            val entity = taskDao.getAllTasksOnce().firstOrNull { it.id == taskId } ?: return@withContext
            val task = entity.toDomain()

            val imageHelper = ImageStorageHelper(context)
            val backupManager = JsonBackupManager(
                context = context,
                taskDao = taskDao,
                focusDao = db.focusSessionDao(),
                recapDao = db.photoRecapDao(),
                imageHelper = imageHelper,
            )
            val repo = JadwalNihRepository(
                taskDao = taskDao,
                focusDao = db.focusSessionDao(),
                recapDao = db.photoRecapDao(),
                imageHelper = imageHelper,
                backupManager = backupManager,
                notificationLogDao = db.notificationLogDao(),
                worshipCheckinDao = db.worshipCheckinDao(),
            )
            val nextRecurring = repo.toggleTaskComplete(task)
            val scheduler = ReminderScheduler(context)
            val checkInEnabled = UserPreferencesStore(context).preferencesFlow.first().checkInNotificationsEnabled

            if (!task.isCompleted) {
                scheduler.cancelForTask(task.id)
            } else if (task.reminderEnabled || checkInEnabled) {
                scheduler.scheduleForTask(
                    task.copy(isCompleted = false),
                    checkInNotificationsEnabled = checkInEnabled,
                )
            }
            if (nextRecurring != null && (nextRecurring.reminderEnabled || checkInEnabled)) {
                scheduler.scheduleForTask(
                    nextRecurring,
                    checkInNotificationsEnabled = checkInEnabled,
                )
            }
        }
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PriorityQuadrantWidget.KEY_REVISION] =
                (prefs[PriorityQuadrantWidget.KEY_REVISION] ?: 0) + 1
        }
        WidgetSyncHelper.refreshAll(context)
    }

    companion object {
        val KEY_TASK_ID = ActionParameters.Key<Long>("widget_task_id")
    }
}

class CycleWidgetQuadrantAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = WidgetQuadrantFilter.fromStoredName(prefs[PriorityQuadrantWidget.KEY_QUADRANT_FILTER])
            prefs[PriorityQuadrantWidget.KEY_QUADRANT_FILTER] = current.next().name
        }
        PriorityQuadrantWidget().update(context, glanceId)
    }
}

class SelectWidgetQuadrantAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val filterName = parameters[KEY_FILTER_NAME] ?: return
        val selected = WidgetQuadrantFilter.fromStoredName(filterName)
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PriorityQuadrantWidget.KEY_QUADRANT_FILTER] = selected.name
        }
        PriorityQuadrantWidget().update(context, glanceId)
    }

    companion object {
        val KEY_FILTER_NAME = ActionParameters.Key<String>("widget_filter_name")
    }
}

class ToggleWidgetWorshipAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val dateIso = parameters[KEY_WORSHIP_DATE] ?: DateTimeHelper.todayIso()
        val key = parameters[KEY_WORSHIP_KEY] ?: return
        val title = parameters[KEY_WORSHIP_TITLE] ?: key
        val minute = parameters[KEY_WORSHIP_MINUTE] ?: 0
        val religion = ReligionOption.fromStoredName(parameters[KEY_RELIGION_NAME])

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val imageHelper = ImageStorageHelper(context)
            val backupManager = JsonBackupManager(
                context = context,
                taskDao = db.scheduleTaskDao(),
                focusDao = db.focusSessionDao(),
                recapDao = db.photoRecapDao(),
                imageHelper = imageHelper,
            )
            val repo = JadwalNihRepository(
                taskDao = db.scheduleTaskDao(),
                focusDao = db.focusSessionDao(),
                recapDao = db.photoRecapDao(),
                imageHelper = imageHelper,
                backupManager = backupManager,
                notificationLogDao = db.notificationLogDao(),
                worshipCheckinDao = db.worshipCheckinDao(),
            )
            val item = WorshipScheduleItem(
                key = key,
                title = title,
                subtitle = "",
                minuteOfDay = minute,
                dateIso = dateIso,
                isCompleted = false,
            )
            repo.toggleWorshipCheckin(item, religion)
        }
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PriorityQuadrantWidget.KEY_REVISION] =
                (prefs[PriorityQuadrantWidget.KEY_REVISION] ?: 0) + 1
        }
        WidgetSyncHelper.refreshAll(context)
    }

    companion object {
        val KEY_WORSHIP_DATE = ActionParameters.Key<String>("widget_worship_date")
        val KEY_WORSHIP_KEY = ActionParameters.Key<String>("widget_worship_key")
        val KEY_WORSHIP_TITLE = ActionParameters.Key<String>("widget_worship_title")
        val KEY_WORSHIP_MINUTE = ActionParameters.Key<Int>("widget_worship_minute")
        val KEY_RELIGION_NAME = ActionParameters.Key<String>("widget_religion_name")
    }
}
