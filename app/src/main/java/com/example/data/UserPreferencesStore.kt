package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.DateTimeHelper
import com.example.domain.PriorityQuadrant
import com.example.domain.QuickGlanceTarget
import com.example.domain.RecurrenceRule
import com.example.domain.TaskDraft
import com.example.domain.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jadwalnih_settings",
)

class UserPreferencesStore(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val QUICK_GLANCE_TARGET = stringPreferencesKey("quick_glance_target")
        val CHECK_IN_NOTIFICATIONS = booleanPreferencesKey("check_in_notifications")
        val TASK_DRAFT_JSON = stringPreferencesKey("task_draft_json")
    }

    val preferencesFlow: Flow<UserPreferences> = context.settingsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val targetName = prefs[Keys.QUICK_GLANCE_TARGET]
            val target = runCatching {
                if (targetName != null) QuickGlanceTarget.valueOf(targetName) else QuickGlanceTarget.ANALITIK
            }.getOrDefault(QuickGlanceTarget.ANALITIK)

            UserPreferences(
                userName = prefs[Keys.USER_NAME].orEmpty(),
                isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
                quickGlanceTarget = target,
                checkInNotificationsEnabled = prefs[Keys.CHECK_IN_NOTIFICATIONS] ?: true,
            )
        }

    val draftFlow: Flow<TaskDraft?> = context.settingsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            deserializeDraft(prefs[Keys.TASK_DRAFT_JSON])
        }

    suspend fun setUserName(name: String) {
        context.settingsDataStore.edit { it[Keys.USER_NAME] = name.take(40) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setQuickGlanceTarget(target: QuickGlanceTarget) {
        context.settingsDataStore.edit { it[Keys.QUICK_GLANCE_TARGET] = target.name }
    }

    suspend fun setCheckInNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CHECK_IN_NOTIFICATIONS] = enabled }
    }

    suspend fun saveDraft(draft: TaskDraft?) {
        context.settingsDataStore.edit { prefs ->
            if (draft == null) {
                prefs.remove(Keys.TASK_DRAFT_JSON)
            } else {
                prefs[Keys.TASK_DRAFT_JSON] = serializeDraft(draft)
            }
        }
    }

    suspend fun resetPreferences() {
        context.settingsDataStore.edit { it.clear() }
    }

    private fun serializeDraft(draft: TaskDraft): String {
        return JSONObject().apply {
            if (draft.editingTaskId != null) put("editingTaskId", draft.editingTaskId)
            put("title", draft.title)
            put("dateIso", draft.dateIso)
            put("startMinuteOfDay", draft.startMinuteOfDay)
            put("endMinuteOfDay", draft.endMinuteOfDay)
            put("quadrant", draft.quadrant.name)
            put("isFlexible", draft.isFlexible)
            put("recurrence", draft.recurrence.name)
            put("reminderEnabled", draft.reminderEnabled)
            put("reminderLeadMinutes", draft.reminderLeadMinutes)
            put("subtasksJson", serializeSubTasks(draft.subtasks))
            put("isSubtasksTodoMode", draft.isSubtasksTodoMode)
        }.toString()
    }

    private fun deserializeDraft(raw: String?): TaskDraft? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            TaskDraft(
                editingTaskId = if (obj.has("editingTaskId")) obj.optLong("editingTaskId") else null,
                title = obj.optString("title", ""),
                dateIso = obj.optString("dateIso", DateTimeHelper.todayIso()),
                startMinuteOfDay = obj.optInt("startMinuteOfDay", 9 * 60),
                endMinuteOfDay = obj.optInt("endMinuteOfDay", 10 * 60),
                quadrant = runCatching {
                    PriorityQuadrant.valueOf(obj.optString("quadrant"))
                }.getOrDefault(PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT),
                isFlexible = obj.optBoolean("isFlexible", false),
                recurrence = runCatching {
                    RecurrenceRule.valueOf(obj.optString("recurrence"))
                }.getOrDefault(RecurrenceRule.NONE),
                reminderEnabled = obj.optBoolean("reminderEnabled", false),
                reminderLeadMinutes = obj.optInt("reminderLeadMinutes", 10),
                subtasks = deserializeSubTasks(obj.optString("subtasksJson", "[]")),
                isSubtasksTodoMode = obj.optBoolean("isSubtasksTodoMode", false),
            )
        }.getOrNull()
    }
}
