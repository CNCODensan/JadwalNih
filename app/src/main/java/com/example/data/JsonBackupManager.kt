package com.example.data

import android.content.Context
import android.net.Uri
import com.example.domain.AttendanceStatus
import com.example.domain.DateTimeHelper
import com.example.domain.PrayerCardVisibilityMode
import com.example.domain.PriorityQuadrant
import com.example.domain.RecurrenceRule
import com.example.domain.ReligionOption
import com.example.domain.TaskIconCatalog
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JsonBackupManager(
    private val context: Context,
    private val taskDao: ScheduleTaskDao,
    private val focusDao: FocusSessionDao,
    private val recapDao: PhotoRecapDao,
    private val imageHelper: ImageStorageHelper,
    private val notificationLogDao: NotificationLogDao? = null,
    private val worshipCheckinDao: WorshipCheckinDao? = null,
    private val prefsStore: UserPreferencesStore? = null,
) {
    enum class ImportStrategy {
        MERGE,
        REPLACE,
    }

    data class BackupPreviewInfo(
        val format: String, // "jadwalnih-backup" or "tugasnih-backup"
        val appName: String,
        val taskCount: Int,
        val lessonCount: Int = 0,
        val sessionCount: Int = 0,
        val recapCount: Int = 0,
        val notificationCount: Int = 0,
        val suggestedUserName: String? = null,
        val uri: Uri,
    )

    data class ImportResult(
        val message: String,
        val importedUserName: String? = null,
    )

    suspend fun exportToUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tasks = taskDao.getAllTasksOnce()
            val sessions = focusDao.getAllSessionsOnce()
            val recaps = recapDao.getAllRecapsOnce()
            val notifLogs = notificationLogDao?.getAllLogsOnce().orEmpty()
            val worshipCheckins = worshipCheckinDao?.getAllCheckinsOnce().orEmpty()
            val prefs = prefsStore?.preferencesFlow?.first()

            val root = JSONObject().apply {
                put("app", "JadwalNih")
                put("format", "jadwalnih-backup")
                put("version", 5)
                put("exportedAtEpochMs", System.currentTimeMillis())
                put(
                    "tasks",
                    JSONArray().apply {
                        tasks.forEach { t ->
                            put(
                                JSONObject().apply {
                                    put("id", t.id)
                                    put("title", t.title)
                                    put("dateIso", t.dateIso)
                                    put("startMinuteOfDay", t.startMinuteOfDay)
                                    put("endMinuteOfDay", t.endMinuteOfDay)
                                    put("estimatedMinutes", t.estimatedMinutes)
                                    put("quadrantName", t.quadrantName)
                                    put("isCompleted", t.isCompleted)
                                    put("isFlexible", t.isFlexible)
                                    put("recurrenceName", t.recurrenceName)
                                    put("reminderEnabled", t.reminderEnabled)
                                    put("reminderLeadMinutes", t.reminderLeadMinutes)
                                    put("completedAtEpochMs", t.completedAtEpochMs ?: JSONObject.NULL)
                                    put("createdAtEpochMs", t.createdAtEpochMs)
                                    put("subtasks", JSONArray(t.subtasksJson))
                                    put("attendanceStatusName", t.attendanceStatusName)
                                    put("isSubtasksTodoMode", t.isSubtasksTodoMode)
                                    put("notes", t.notes)
                                    put("iconKey", t.iconKey)
                                    put("locationName", t.locationName)
                                    put("locationLat", t.locationLat ?: JSONObject.NULL)
                                    put("locationLng", t.locationLng ?: JSONObject.NULL)
                                    put("skippedOccurrenceDates", t.skippedOccurrenceDates)
                                },
                            )
                        }
                    },
                )
                put(
                    "focusSessions",
                    JSONArray().apply {
                        sessions.forEach { s ->
                            put(
                                JSONObject().apply {
                                    put("id", s.id)
                                    put("linkedTaskId", s.linkedTaskId ?: JSONObject.NULL)
                                    put("linkedTaskTitle", s.linkedTaskTitle ?: JSONObject.NULL)
                                    put("plannedMinutes", s.plannedMinutes)
                                    put("actualSeconds", s.actualSeconds)
                                    put("completedNaturally", s.completedNaturally)
                                    put("startedAtEpochMs", s.startedAtEpochMs)
                                    put("endedAtEpochMs", s.endedAtEpochMs)
                                },
                            )
                        }
                    },
                )
                put(
                    "photoRecaps",
                    JSONArray().apply {
                        recaps.forEach { r ->
                            val domain = r.toDomain()
                            val encodedImages = JSONArray()
                            domain.imagePaths.forEach { path ->
                                imageHelper.encodeFileToBase64(path)?.let { b64 ->
                                    encodedImages.put(b64)
                                }
                            }
                            put(
                                JSONObject().apply {
                                    put("dateIso", domain.dateIso)
                                    put("title", domain.title)
                                    put("caption", domain.caption)
                                    put("updatedAtEpochMs", domain.updatedAtEpochMs)
                                    put("locationName", domain.locationName)
                                    put("locationLat", domain.locationLat ?: JSONObject.NULL)
                                    put("locationLng", domain.locationLng ?: JSONObject.NULL)
                                    put("imagesBase64", encodedImages)
                                },
                            )
                        }
                    },
                )
                put(
                    "notificationLogs",
                    JSONArray().apply {
                        notifLogs.forEach { n ->
                            put(
                                JSONObject().apply {
                                    put("category", n.category)
                                    put("title", n.title)
                                    put("message", n.message)
                                    put("createdAtEpochMs", n.createdAtEpochMs)
                                    put("isRead", n.isRead)
                                    put("relatedTaskId", n.relatedTaskId ?: JSONObject.NULL)
                                    put("draftJson", n.draftJson ?: JSONObject.NULL)
                                },
                            )
                        }
                    },
                )
                put(
                    "worshipCheckins",
                    JSONArray().apply {
                        worshipCheckins.forEach { w ->
                            put(
                                JSONObject().apply {
                                    put("dateItemKey", w.dateItemKey)
                                    put("dateIso", w.dateIso)
                                    put("itemKey", w.itemKey)
                                    put("title", w.title)
                                    put("religionName", w.religionName)
                                    put("isCompleted", w.isCompleted)
                                    put("completedAtEpochMs", w.completedAtEpochMs ?: JSONObject.NULL)
                                },
                            )
                        }
                    },
                )
                if (prefs != null) {
                    put(
                        "preferences",
                        JSONObject().apply {
                            put("userName", prefs.userName)
                            put("religion", prefs.religion.name)
                            put("prayerReminderEnabled", prefs.prayerReminderEnabled)
                            put("worshipNotificationSoundEnabled", prefs.worshipNotificationSoundEnabled)
                            put("holidayThemeEnabled", prefs.holidayThemeEnabled)
                            put("lastLocationName", prefs.lastLocationName)
                            put("lastLatitude", prefs.lastLatitude ?: JSONObject.NULL)
                            put("lastLongitude", prefs.lastLongitude ?: JSONObject.NULL)
                            put("birthDateIso", prefs.birthDateIso)
                            put("firstUsedAtEpochMs", prefs.firstUsedAtEpochMs)
                            put("prayerCardVisibilityMode", prefs.prayerCardVisibilityMode.name)
                            put("allowMissedPrayerManualEdit", prefs.allowMissedPrayerManualEdit)
                        },
                    )
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Tidak dapat membuka file tujuan ekspor.")

            "Berhasil cadangkan ${tasks.size} jadwal, ${sessions.size} sesi fokus, ${recaps.size} rekap, & ${notifLogs.size} riwayat notifikasi."
        }
    }

    suspend fun inspectBackup(uri: Uri): Result<BackupPreviewInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Tidak dapat membaca file JSON.")

            val root = JSONObject(jsonText)
            val formatStr = root.optString("format", "")

            if (formatStr == "tugasnih-backup" || root.has("tables")) {
                val tables = root.optJSONObject("tables") ?: JSONObject()
                val tasksArr = tables.optJSONArray("tasks") ?: JSONArray()
                val lessonsArr = tables.optJSONArray("lessons") ?: JSONArray()
                val settingsArr = tables.optJSONArray("settings") ?: JSONArray()
                var userName: String? = null
                for (i in 0 until settingsArr.length()) {
                    val sObj = settingsArr.optJSONObject(i)
                    if (sObj?.optString("key") == "user_name") {
                        val v = sObj.optString("value").trim()
                        if (v.isNotEmpty()) userName = v
                    }
                }
                BackupPreviewInfo(
                    format = "tugasnih-backup",
                    appName = "TugasNih",
                    taskCount = tasksArr.length(),
                    lessonCount = lessonsArr.length(),
                    sessionCount = 0,
                    recapCount = 0,
                    suggestedUserName = userName,
                    uri = uri,
                )
            } else if (root.optString("app") == "JadwalNih" || root.has("tasks")) {
                val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
                val sessionsArr = root.optJSONArray("focusSessions") ?: JSONArray()
                val recapsArr = root.optJSONArray("photoRecaps") ?: JSONArray()
                val logsArr = root.optJSONArray("notificationLogs") ?: JSONArray()
                val prefsObj = root.optJSONObject("preferences")
                BackupPreviewInfo(
                    format = "jadwalnih-backup",
                    appName = "JadwalNih",
                    taskCount = tasksArr.length(),
                    lessonCount = 0,
                    sessionCount = sessionsArr.length(),
                    recapCount = recapsArr.length(),
                    notificationCount = logsArr.length(),
                    suggestedUserName = prefsObj?.optString("userName")?.takeIf { it.isNotBlank() },
                    uri = uri,
                )
            } else {
                throw IllegalArgumentException("Format file JSON belum dikenali sebagai cadangan JadwalNih atau TugasNih.")
            }
        }
    }

    suspend fun executeImport(
        uri: Uri,
        strategy: ImportStrategy,
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Tidak dapat membaca file JSON.")

            val root = JSONObject(jsonText)
            val formatStr = root.optString("format", "")

            if (formatStr == "tugasnih-backup" || root.has("tables")) {
                importTugasNih(root, strategy)
            } else if (root.optString("app") == "JadwalNih" || root.has("tasks")) {
                importJadwalNih(root, strategy)
            } else {
                throw IllegalArgumentException("Format file JSON belum dikenali.")
            }
        }
    }

    private suspend fun importTugasNih(
        root: JSONObject,
        strategy: ImportStrategy,
    ): ImportResult {
        val tables = root.optJSONObject("tables") ?: JSONObject()
        val parsedTasks = mutableListOf<ScheduleTaskEntity>()

        val lessonsArr = tables.optJSONArray("lessons") ?: JSONArray()
        val currentWeekDates = DateTimeHelper.weekDaysFor(DateTimeHelper.todayIso())

        for (i in 0 until lessonsArr.length()) {
            val lObj = lessonsArr.optJSONObject(i) ?: continue
            val weekday = lObj.optInt("weekday", 1).coerceIn(1, 7)
            val targetDateIso = currentWeekDates.getOrNull(weekday - 1) ?: DateTimeHelper.todayIso()
            val rawSubject = lObj.optString("subject", "").trim()
            val isBreak = lObj.optInt("is_break", 0) == 1 || rawSubject.equals("Istirahat", ignoreCase = true)
            val startTimeStr = lObj.optString("start_time", "08:00")
            val endTimeStr = lObj.optString("end_time", "09:00")
            val startMin = parseMinuteOfDay(startTimeStr, 8 * 60)
            val endMin = parseMinuteOfDay(endTimeStr, 9 * 60)
            val duration = (endMin - startMin).coerceAtLeast(1)
            val title = if (isBreak) "Istirahat" else rawSubject.ifBlank { "Pelajaran" }
            val books = lObj.optString("books", "").trim()
            val subtasksJson = if (books.isNotBlank()) {
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("id", UUID.randomUUID().toString())
                            put("title", "Bawa buku: $books")
                            put("isDone", false)
                        },
                    )
                }.toString()
            } else {
                "[]"
            }

            parsedTasks.add(
                ScheduleTaskEntity(
                    id = 0L,
                    title = title,
                    dateIso = targetDateIso,
                    startMinuteOfDay = startMin,
                    endMinuteOfDay = endMin,
                    estimatedMinutes = duration,
                    quadrantName = if (isBreak) {
                        PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT.name
                    } else {
                        PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT.name
                    },
                    isCompleted = false,
                    isFlexible = false,
                    recurrenceName = RecurrenceRule.WEEKLY.name,
                    reminderEnabled = false,
                    reminderLeadMinutes = 10,
                    completedAtEpochMs = null,
                    createdAtEpochMs = System.currentTimeMillis() + i,
                    subtasksJson = subtasksJson,
                    attendanceStatusName = AttendanceStatus.UNCONFIRMED.name,
                    isSubtasksTodoMode = books.isNotBlank(),
                    iconKey = if (isBreak) "coffee" else TaskIconCatalog.autoDetectIconKey(title),
                ),
            )
        }

        val tasksArr = tables.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tasksArr.length()) {
            val tObj = tasksArr.optJSONObject(i) ?: continue
            val rawTitle = tObj.optString("title", "").trim()
            val rawSubject = tObj.optString("subject", "").trim()
            if (rawTitle.isEmpty() && rawSubject.isEmpty()) continue
            val title = if (rawSubject.isNotBlank() && !rawTitle.contains(rawSubject, ignoreCase = true)) {
                "$rawTitle ($rawSubject)"
            } else {
                rawTitle.ifBlank { rawSubject }
            }
            val dueDate = tObj.optString("due_date", "").trim()
            val dateIso = if (dueDate.length == 10 && dueDate.contains("-")) dueDate else DateTimeHelper.todayIso()
            val isDone = tObj.optInt("done", 0) == 1
            val checklistStr = tObj.optString("checklist", "")
            val subtasksArr = JSONArray()
            if (checklistStr.isNotBlank()) {
                runCatching {
                    val parsedChecklist = JSONArray(checklistStr)
                    for (k in 0 until parsedChecklist.length()) {
                        val cItem = parsedChecklist.optJSONObject(k)
                        if (cItem != null) {
                            val tText = cItem.optString("t", "").trim()
                            if (tText.isNotEmpty()) {
                                subtasksArr.put(
                                    JSONObject().apply {
                                        put("id", UUID.randomUUID().toString())
                                        put("title", tText)
                                        put("isDone", cItem.optBoolean("d", false))
                                    },
                                )
                            }
                        }
                    }
                }
            }
            val note = tObj.optString("note", "").trim()
            val hasSubtasks = subtasksArr.length() > 0
            val isPr = tObj.optString("kind", "") == "pr"
            val quadrant = if (isDone) {
                PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT
            } else if (isPr) {
                PriorityQuadrant.Q1_IMPORTANT_URGENT
            } else {
                PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT
            }

            parsedTasks.add(
                ScheduleTaskEntity(
                    id = 0L,
                    title = title,
                    dateIso = dateIso,
                    startMinuteOfDay = 19 * 60,
                    endMinuteOfDay = 20 * 60,
                    estimatedMinutes = 60,
                    quadrantName = quadrant.name,
                    isCompleted = isDone,
                    isFlexible = true,
                    recurrenceName = RecurrenceRule.NONE.name,
                    reminderEnabled = false,
                    reminderLeadMinutes = 10,
                    completedAtEpochMs = if (isDone) System.currentTimeMillis() else null,
                    createdAtEpochMs = System.currentTimeMillis() + 1000 + i,
                    subtasksJson = subtasksArr.toString(),
                    attendanceStatusName = AttendanceStatus.UNCONFIRMED.name,
                    isSubtasksTodoMode = hasSubtasks,
                    notes = note,
                    iconKey = TaskIconCatalog.autoDetectIconKey(title),
                ),
            )
        }

        var importedUserName: String? = null
        if (strategy == ImportStrategy.REPLACE) {
            taskDao.clearAllTasks()
            focusDao.clearAllSessions()
            recapDao.clearAllRecaps()
            imageHelper.clearAllRecapImages()

            val settingsArr = tables.optJSONArray("settings") ?: JSONArray()
            for (i in 0 until settingsArr.length()) {
                val sObj = settingsArr.optJSONObject(i)
                if (sObj?.optString("key") == "user_name") {
                    val v = sObj.optString("value").trim()
                    if (v.isNotEmpty()) importedUserName = v
                }
            }
        }

        if (parsedTasks.isNotEmpty()) {
            taskDao.insertTasks(parsedTasks)
        }

        val strategyLabel = if (strategy == ImportStrategy.REPLACE) "Timpa Bersih" else "Gabungkan"
        val message = "Impor TugasNih berhasil ($strategyLabel): ${tasksArr.length()} tugas dan ${lessonsArr.length()} jadwal dimuat."
        return ImportResult(message = message, importedUserName = importedUserName)
    }

    private suspend fun importJadwalNih(
        root: JSONObject,
        strategy: ImportStrategy,
    ): ImportResult {
        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        val parsedTasks = mutableListOf<ScheduleTaskEntity>()
        for (i in 0 until tasksArr.length()) {
            val o = tasksArr.getJSONObject(i)
            val title = o.optString("title").trim()
            val dateIso = o.optString("dateIso").trim()
            if (title.isEmpty() || dateIso.isEmpty()) continue
            val startMin = o.optInt("startMinuteOfDay", 9 * 60)
            val endMin = o.optInt("endMinuteOfDay", 10 * 60)
            parsedTasks.add(
                ScheduleTaskEntity(
                    id = 0L,
                    title = title,
                    dateIso = dateIso,
                    startMinuteOfDay = startMin,
                    endMinuteOfDay = endMin,
                    estimatedMinutes = (endMin - startMin).coerceAtLeast(o.optInt("estimatedMinutes", 60)).coerceAtLeast(1),
                    quadrantName = o.optString("quadrantName", "Q2_IMPORTANT_NOT_URGENT"),
                    isCompleted = o.optBoolean("isCompleted", false),
                    isFlexible = o.optBoolean("isFlexible", false),
                    recurrenceName = o.optString("recurrenceName", "NONE"),
                    reminderEnabled = o.optBoolean("reminderEnabled", false),
                    reminderLeadMinutes = o.optInt("reminderLeadMinutes", 10),
                    completedAtEpochMs = if (o.isNull("completedAtEpochMs")) null else o.optLong("completedAtEpochMs"),
                    createdAtEpochMs = o.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    subtasksJson = o.optJSONArray("subtasks")?.toString() ?: "[]",
                    attendanceStatusName = o.optString("attendanceStatusName", "UNCONFIRMED"),
                    isSubtasksTodoMode = o.optBoolean("isSubtasksTodoMode", false),
                    notes = o.optString("notes", ""),
                    iconKey = o.optString("iconKey", "").ifBlank { TaskIconCatalog.autoDetectIconKey(title) },
                    locationName = o.optString("locationName", ""),
                    locationLat = if (o.isNull("locationLat")) null else o.optDouble("locationLat"),
                    locationLng = if (o.isNull("locationLng")) null else o.optDouble("locationLng"),
                    skippedOccurrenceDates = o.optString("skippedOccurrenceDates", ""),
                ),
            )
        }

        val sessionsArr = root.optJSONArray("focusSessions") ?: JSONArray()
        val parsedSessions = mutableListOf<FocusSessionEntity>()
        for (i in 0 until sessionsArr.length()) {
            val o = sessionsArr.getJSONObject(i)
            parsedSessions.add(
                FocusSessionEntity(
                    id = 0L,
                    linkedTaskId = if (o.isNull("linkedTaskId")) null else o.optLong("linkedTaskId"),
                    linkedTaskTitle = if (o.isNull("linkedTaskTitle")) null else o.optString("linkedTaskTitle"),
                    plannedMinutes = o.optInt("plannedMinutes", 25).coerceIn(1, 240),
                    actualSeconds = o.optInt("actualSeconds", 0).coerceAtLeast(0),
                    completedNaturally = o.optBoolean("completedNaturally", true),
                    startedAtEpochMs = o.optLong("startedAtEpochMs", System.currentTimeMillis()),
                    endedAtEpochMs = o.optLong("endedAtEpochMs", System.currentTimeMillis()),
                ),
            )
        }

        val recapsArr = root.optJSONArray("photoRecaps") ?: JSONArray()
        val parsedRecaps = mutableListOf<PhotoRecapEntity>()
        if (strategy == ImportStrategy.REPLACE) {
            imageHelper.clearAllRecapImages()
        }
        for (i in 0 until recapsArr.length()) {
            val o = recapsArr.getJSONObject(i)
            val dateIso = o.optString("dateIso").trim()
            if (dateIso.isEmpty()) continue
            val imagesArr = o.optJSONArray("imagesBase64") ?: JSONArray()
            val savedPaths = JSONArray()
            for (j in 0 until minOf(4, imagesArr.length())) {
                val b64 = imagesArr.optString(j)
                if (b64.isNotBlank()) {
                    imageHelper.saveFromBase64(b64)?.let { savedPaths.put(it) }
                }
            }
            parsedRecaps.add(
                PhotoRecapEntity(
                    dateIso = dateIso,
                    title = o.optString("title", ""),
                    caption = o.optString("caption", ""),
                    imagePathsJson = savedPaths.toString(),
                    updatedAtEpochMs = o.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                    locationName = o.optString("locationName", ""),
                    locationLat = if (o.isNull("locationLat")) null else o.optDouble("locationLat"),
                    locationLng = if (o.isNull("locationLng")) null else o.optDouble("locationLng"),
                ),
            )
        }

        val logsArr = root.optJSONArray("notificationLogs") ?: JSONArray()
        val parsedLogs = mutableListOf<NotificationLogEntity>()
        for (i in 0 until logsArr.length()) {
            val o = logsArr.optJSONObject(i) ?: continue
            parsedLogs.add(
                NotificationLogEntity(
                    id = 0L,
                    category = o.optString("category", "TASK"),
                    title = o.optString("title", ""),
                    message = o.optString("message", ""),
                    createdAtEpochMs = o.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    isRead = o.optBoolean("isRead", true),
                    relatedTaskId = if (o.isNull("relatedTaskId")) null else o.optLong("relatedTaskId"),
                    draftJson = if (o.isNull("draftJson")) null else o.optString("draftJson"),
                ),
            )
        }

        val worshipArr = root.optJSONArray("worshipCheckins") ?: JSONArray()
        val parsedWorship = mutableListOf<WorshipCheckinEntity>()
        for (i in 0 until worshipArr.length()) {
            val o = worshipArr.optJSONObject(i) ?: continue
            val key = o.optString("dateItemKey").trim()
            if (key.isEmpty()) continue
            parsedWorship.add(
                WorshipCheckinEntity(
                    dateItemKey = key,
                    dateIso = o.optString("dateIso", ""),
                    itemKey = o.optString("itemKey", ""),
                    title = o.optString("title", ""),
                    religionName = o.optString("religionName", ReligionOption.ISLAM.name),
                    isCompleted = o.optBoolean("isCompleted", false),
                    completedAtEpochMs = if (o.isNull("completedAtEpochMs")) null else o.optLong("completedAtEpochMs"),
                ),
            )
        }

        if (strategy == ImportStrategy.REPLACE) {
            taskDao.clearAllTasks()
            focusDao.clearAllSessions()
            recapDao.clearAllRecaps()
            notificationLogDao?.clearAllLogs()
            worshipCheckinDao?.clearAllCheckins()
        }

        if (parsedTasks.isNotEmpty()) taskDao.insertTasks(parsedTasks)
        if (parsedSessions.isNotEmpty()) focusDao.insertSessions(parsedSessions)
        if (parsedRecaps.isNotEmpty()) recapDao.insertRecaps(parsedRecaps)
        if (parsedLogs.isNotEmpty()) notificationLogDao?.insertLogs(parsedLogs)
        if (parsedWorship.isNotEmpty()) worshipCheckinDao?.insertCheckins(parsedWorship)

        var importedUserName: String? = null
        val prefsObj = root.optJSONObject("preferences")
        if (prefsObj != null && prefsStore != null) {
            val name = prefsObj.optString("userName", "").trim()
            if (name.isNotEmpty()) {
                importedUserName = name
                prefsStore.setUserName(name)
            }
            if (prefsObj.has("religion")) {
                prefsStore.setReligion(ReligionOption.fromStoredName(prefsObj.optString("religion")))
            }
            if (prefsObj.has("prayerReminderEnabled")) {
                prefsStore.setPrayerReminderEnabled(prefsObj.optBoolean("prayerReminderEnabled", true))
            }
            if (prefsObj.has("worshipNotificationSoundEnabled")) {
                prefsStore.setWorshipNotificationSoundEnabled(prefsObj.optBoolean("worshipNotificationSoundEnabled", true))
            }
            if (prefsObj.has("holidayThemeEnabled")) {
                prefsStore.setHolidayThemeEnabled(prefsObj.optBoolean("holidayThemeEnabled", true))
            }
            val locName = prefsObj.optString("lastLocationName", "").trim()
            if (locName.isNotEmpty()) {
                val lat = if (prefsObj.isNull("lastLatitude")) null else prefsObj.optDouble("lastLatitude")
                val lng = if (prefsObj.isNull("lastLongitude")) null else prefsObj.optDouble("lastLongitude")
                prefsStore.setLastLocation(locName, lat, lng)
            }
            if (prefsObj.has("birthDateIso")) {
                prefsStore.setBirthDateIso(prefsObj.optString("birthDateIso", "").trim())
            }
            if (prefsObj.has("firstUsedAtEpochMs")) {
                val firstUsed = prefsObj.optLong("firstUsedAtEpochMs", 0L)
                if (firstUsed > 0L) {
                    prefsStore.setFirstUsedAtEpochMs(firstUsed)
                }
            }
            if (prefsObj.has("prayerCardVisibilityMode")) {
                prefsStore.setPrayerCardVisibilityMode(
                    PrayerCardVisibilityMode.fromStoredName(prefsObj.optString("prayerCardVisibilityMode")),
                )
            }
            if (prefsObj.has("allowMissedPrayerManualEdit")) {
                prefsStore.setAllowMissedPrayerManualEdit(
                    prefsObj.optBoolean("allowMissedPrayerManualEdit", true),
                )
            }
        }

        val strategyLabel = if (strategy == ImportStrategy.REPLACE) "Timpa Bersih" else "Gabungkan"
        val message = "Impor berhasil ($strategyLabel): ${parsedTasks.size} jadwal, ${parsedSessions.size} sesi fokus, ${parsedRecaps.size} rekap, & ${parsedLogs.size} riwayat."
        return ImportResult(message = message, importedUserName = importedUserName)
    }

    suspend fun importFromUri(uri: Uri): Result<String> {
        return executeImport(uri, ImportStrategy.REPLACE).map { it.message }
    }

    private fun parseMinuteOfDay(timeStr: String, defaultVal: Int): Int {
        val parts = timeStr.trim().split(":", ".")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: return defaultVal
            val m = parts[1].toIntOrNull() ?: return defaultVal
            return (h * 60 + m).coerceIn(0, 1439)
        }
        return defaultVal
    }
}
