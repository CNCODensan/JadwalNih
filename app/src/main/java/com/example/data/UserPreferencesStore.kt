package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.DateTimeHelper
import com.example.domain.HolidayThemeType
import com.example.domain.PrayerCardVisibilityMode
import com.example.domain.PriorityQuadrant
import com.example.domain.QuickGlanceTarget
import com.example.domain.RecurrenceRule
import com.example.domain.ReligionOption
import com.example.domain.TaskDraft
import com.example.domain.ThemeMode
import com.example.domain.UserPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jadwalnih_settings",
)

class UserPreferencesStore(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val QUICK_GLANCE_TARGET = stringPreferencesKey("quick_glance_target")
        val CHECK_IN_NOTIFICATIONS = booleanPreferencesKey("check_in_notifications")
        val IS_SIMPLE_MODE = booleanPreferencesKey("is_simple_mode")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val TASK_DRAFT_JSON = stringPreferencesKey("task_draft_json")
        val SKIPPED_RESCHEDULE_DATE = stringPreferencesKey("skipped_reschedule_date")
        val SKIPPED_RESCHEDULE_IDS = stringPreferencesKey("skipped_reschedule_ids")

        val RELIGION = stringPreferencesKey("religion")
        val PRAYER_REMINDER_ENABLED = booleanPreferencesKey("prayer_reminder_enabled")
        val WORSHIP_SOUND_ENABLED = booleanPreferencesKey("worship_sound_enabled")
        val HOLIDAY_THEME_ENABLED = booleanPreferencesKey("holiday_theme_enabled")
        val HOLIDAY_THEME_PREVIEW = stringPreferencesKey("holiday_theme_preview")
        val HAS_SEEN_V1_WELCOME_CARD = booleanPreferencesKey("has_seen_v1_welcome_card")
        val HAS_PROMPTED_INITIAL_LOCATION = booleanPreferencesKey("has_prompted_initial_location")
        val LAST_LOCATION_NAME = stringPreferencesKey("last_location_name")
        val LAST_LATITUDE = doublePreferencesKey("last_latitude")
        val LAST_LONGITUDE = doublePreferencesKey("last_longitude")

        val BIRTH_DATE_ISO = stringPreferencesKey("birth_date_iso")
        val FIRST_USED_AT_EPOCH_MS = longPreferencesKey("first_used_at_epoch_ms")
        val PRAYER_CARD_VISIBILITY_MODE = stringPreferencesKey("prayer_card_visibility_mode")
        val ALLOW_MISSED_PRAYER_MANUAL_EDIT = booleanPreferencesKey("allow_missed_prayer_manual_edit")
        val LAST_HOLIDAY_MOTION_DATE_ISO = stringPreferencesKey("last_holiday_motion_date_iso")
        val LAST_BIRTHDAY_CELEBRATION_YEAR = intPreferencesKey("last_birthday_celebration_year")
        val LAST_LOGGED_HOLIDAY_KEY = stringPreferencesKey("last_logged_holiday_key")
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

            val skippedIds = prefs[Keys.SKIPPED_RESCHEDULE_IDS]
                .orEmpty()
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .toSet()

            val themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
                runCatching { ThemeMode.valueOf(raw) }.getOrNull()
            } ?: if (prefs.contains(Keys.IS_DARK_MODE)) {
                if (prefs[Keys.IS_DARK_MODE] == true) ThemeMode.DARK else ThemeMode.LIGHT
            } else {
                ThemeMode.SYSTEM
            }

            UserPreferences(
                userName = prefs[Keys.USER_NAME].orEmpty(),
                themeMode = themeMode,
                quickGlanceTarget = target,
                checkInNotificationsEnabled = prefs[Keys.CHECK_IN_NOTIFICATIONS] ?: true,
                isSimpleMode = prefs[Keys.IS_SIMPLE_MODE] ?: false,
                hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
                skippedRescheduleDateIso = prefs[Keys.SKIPPED_RESCHEDULE_DATE].orEmpty(),
                skippedRescheduleTaskIds = skippedIds,
                religion = ReligionOption.fromStoredName(prefs[Keys.RELIGION]),
                prayerReminderEnabled = prefs[Keys.PRAYER_REMINDER_ENABLED] ?: true,
                worshipNotificationSoundEnabled = prefs[Keys.WORSHIP_SOUND_ENABLED] ?: true,
                holidayThemeEnabled = prefs[Keys.HOLIDAY_THEME_ENABLED] ?: true,
                holidayThemePreview = HolidayThemeType.fromStoredName(prefs[Keys.HOLIDAY_THEME_PREVIEW]),
                hasSeenV1WelcomeCard = prefs[Keys.HAS_SEEN_V1_WELCOME_CARD] ?: false,
                hasPromptedInitialLocation = prefs[Keys.HAS_PROMPTED_INITIAL_LOCATION] ?: false,
                lastLocationName = prefs[Keys.LAST_LOCATION_NAME].orEmpty(),
                lastLatitude = prefs[Keys.LAST_LATITUDE],
                lastLongitude = prefs[Keys.LAST_LONGITUDE],
                birthDateIso = prefs[Keys.BIRTH_DATE_ISO].orEmpty(),
                firstUsedAtEpochMs = prefs[Keys.FIRST_USED_AT_EPOCH_MS] ?: 0L,
                prayerCardVisibilityMode = PrayerCardVisibilityMode.fromStoredName(
                    prefs[Keys.PRAYER_CARD_VISIBILITY_MODE],
                ),
                allowMissedPrayerManualEdit = prefs[Keys.ALLOW_MISSED_PRAYER_MANUAL_EDIT] ?: true,
                lastHolidayMotionDateIso = prefs[Keys.LAST_HOLIDAY_MOTION_DATE_ISO].orEmpty(),
                lastBirthdayCelebrationYear = prefs[Keys.LAST_BIRTHDAY_CELEBRATION_YEAR] ?: 0,
                lastLoggedHolidayKey = prefs[Keys.LAST_LOGGED_HOLIDAY_KEY].orEmpty(),
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

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
            if (mode == ThemeMode.DARK) {
                prefs[Keys.IS_DARK_MODE] = true
            } else if (mode == ThemeMode.LIGHT) {
                prefs[Keys.IS_DARK_MODE] = false
            } else {
                prefs.remove(Keys.IS_DARK_MODE)
            }
        }
    }

    suspend fun setSimpleMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.IS_SIMPLE_MODE] = enabled }
    }

    suspend fun completeOnboarding(name: String, isSimpleMode: Boolean, themeMode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().take(40)
            prefs[Keys.IS_SIMPLE_MODE] = isSimpleMode
            prefs[Keys.THEME_MODE] = themeMode.name
            if (themeMode == ThemeMode.SYSTEM) {
                prefs.remove(Keys.IS_DARK_MODE)
            } else {
                prefs[Keys.IS_DARK_MODE] = (themeMode == ThemeMode.DARK)
            }
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun setQuickGlanceTarget(target: QuickGlanceTarget) {
        context.settingsDataStore.edit { it[Keys.QUICK_GLANCE_TARGET] = target.name }
    }

    suspend fun setCheckInNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CHECK_IN_NOTIFICATIONS] = enabled }
    }

    suspend fun setReligion(religion: ReligionOption) {
        context.settingsDataStore.edit { it[Keys.RELIGION] = religion.name }
    }

    suspend fun setPrayerReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PRAYER_REMINDER_ENABLED] = enabled }
    }

    suspend fun setWorshipNotificationSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.WORSHIP_SOUND_ENABLED] = enabled }
    }

    suspend fun setHolidayThemeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HOLIDAY_THEME_ENABLED] = enabled }
    }

    suspend fun setHolidayThemePreview(preview: HolidayThemeType) {
        context.settingsDataStore.edit { it[Keys.HOLIDAY_THEME_PREVIEW] = preview.name }
    }

    suspend fun setHasSeenV1WelcomeCard(seen: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAS_SEEN_V1_WELCOME_CARD] = seen }
    }

    suspend fun setHasPromptedInitialLocation(prompted: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAS_PROMPTED_INITIAL_LOCATION] = prompted }
    }

    suspend fun setLastLocation(name: String, lat: Double?, lng: Double?) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_LOCATION_NAME] = name.trim().take(120)
            if (lat != null && lng != null) {
                prefs[Keys.LAST_LATITUDE] = lat
                prefs[Keys.LAST_LONGITUDE] = lng
            }
        }
    }

    suspend fun setBirthDateIso(birthDateIso: String) {
        context.settingsDataStore.edit { it[Keys.BIRTH_DATE_ISO] = birthDateIso.trim().take(10) }
    }

    suspend fun ensureFirstUsedAtEpochMs(nowEpochMs: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.FIRST_USED_AT_EPOCH_MS] ?: 0L
            if (current <= 0L) {
                prefs[Keys.FIRST_USED_AT_EPOCH_MS] = nowEpochMs
            }
        }
    }

    suspend fun setFirstUsedAtEpochMs(epochMs: Long) {
        if (epochMs <= 0L) return
        context.settingsDataStore.edit { it[Keys.FIRST_USED_AT_EPOCH_MS] = epochMs }
    }

    suspend fun setPrayerCardVisibilityMode(mode: PrayerCardVisibilityMode) {
        context.settingsDataStore.edit { it[Keys.PRAYER_CARD_VISIBILITY_MODE] = mode.name }
    }

    suspend fun setAllowMissedPrayerManualEdit(allowed: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALLOW_MISSED_PRAYER_MANUAL_EDIT] = allowed }
    }

    suspend fun setLastHolidayMotionDateIso(dateIso: String) {
        context.settingsDataStore.edit { it[Keys.LAST_HOLIDAY_MOTION_DATE_ISO] = dateIso.trim().take(10) }
    }

    suspend fun setLastBirthdayCelebrationYear(year: Int) {
        context.settingsDataStore.edit { it[Keys.LAST_BIRTHDAY_CELEBRATION_YEAR] = year }
    }

    suspend fun setLastLoggedHolidayKey(key: String) {
        context.settingsDataStore.edit { it[Keys.LAST_LOGGED_HOLIDAY_KEY] = key.trim().take(160) }
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

    suspend fun skipYesterdayReschedule(dateIso: String, taskIds: Set<Long>) {
        context.settingsDataStore.edit { prefs ->
            val currentDate = prefs[Keys.SKIPPED_RESCHEDULE_DATE].orEmpty()
            val existingIds = if (currentDate == dateIso) {
                prefs[Keys.SKIPPED_RESCHEDULE_IDS]
                    .orEmpty()
                    .split(",")
                    .mapNotNull { it.trim().toLongOrNull() }
                    .toSet()
            } else {
                emptySet()
            }
            val merged = existingIds + taskIds
            prefs[Keys.SKIPPED_RESCHEDULE_DATE] = dateIso
            prefs[Keys.SKIPPED_RESCHEDULE_IDS] = merged.joinToString(",")
        }
    }

    suspend fun resetPreferences() {
        context.settingsDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.FIRST_USED_AT_EPOCH_MS] = System.currentTimeMillis()
        }
    }

    companion object {
        fun serializeDraft(draft: TaskDraft): String {
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
                put("notes", draft.notes)
                put("aiRawText", draft.aiRawText)
                put("isOtomatisMode", draft.isOtomatisMode)
                put("iconKey", draft.iconKey)
                put("locationName", draft.locationName)
                if (draft.locationLat != null) put("locationLat", draft.locationLat)
                if (draft.locationLng != null) put("locationLng", draft.locationLng)
            }.toString()
        }

        fun deserializeDraft(raw: String?): TaskDraft? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val obj = JSONObject(raw)
                val editingId = if (obj.has("editingTaskId") && !obj.isNull("editingTaskId")) {
                    obj.optLong("editingTaskId").takeIf { it > 0L }
                } else {
                    null
                }
                TaskDraft(
                    editingTaskId = editingId,
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
                    notes = obj.optString("notes", ""),
                    aiRawText = obj.optString("aiRawText", ""),
                    isOtomatisMode = obj.optBoolean("isOtomatisMode", true),
                    iconKey = obj.optString("iconKey", ""),
                    locationName = obj.optString("locationName", ""),
                    locationLat = if (obj.has("locationLat") && !obj.isNull("locationLat")) obj.optDouble("locationLat") else null,
                    locationLng = if (obj.has("locationLng") && !obj.isNull("locationLng")) obj.optDouble("locationLng") else null,
                )
            }.getOrNull()
        }
    }
}
