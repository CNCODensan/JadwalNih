package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JsonBackupManager(
    private val context: Context,
    private val taskDao: ScheduleTaskDao,
    private val focusDao: FocusSessionDao,
    private val recapDao: PhotoRecapDao,
    private val prefsManager: PreferencesManager,
    private val imageHelper: ImageStorageHelper,
) {
    suspend fun exportToUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tasks = taskDao.getAllTasksOnce()
            val sessions = focusDao.getAllSessionsOnce()
            val recaps = recapDao.getAllRecapsOnce()
            val prefs = prefsManager.getPreferencesOnce()

            val root = JSONObject().apply {
                put("app", "JadwalNih")
                put("version", 1)
                put("exportedAtEpochMs", System.currentTimeMillis())
                put(
                    "preferences",
                    JSONObject().apply {
                        put("isDarkMode", prefs.isDarkMode)
                        put("lastCustomFocusMinutes", prefs.lastCustomFocusMinutes)
                    },
                )
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
                                    put("imagesBase64", encodedImages)
                                },
                            )
                        }
                    },
                )
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Tidak dapat membuka berkas tujuan ekspor.")

            "Berhasil mengekspor ${tasks.size} jadwal, ${sessions.size} sesi fokus, dan ${recaps.size} rekap foto."
        }
    }

    suspend fun importFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Tidak dapat membaca berkas JSON.")

            val root = JSONObject(jsonText)
            if (root.optString("app") != "JadwalNih" && !root.has("tasks")) {
                throw IllegalArgumentException("Format berkas JSON tidak dikenali sebagai cadangan JadwalNih.")
            }

            val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
            val parsedTasks = mutableListOf<ScheduleTaskEntity>()
            for (i in 0 until tasksArr.length()) {
                val o = tasksArr.getJSONObject(i)
                val title = o.optString("title").trim()
                val dateIso = o.optString("dateIso").trim()
                if (title.isEmpty() || dateIso.isEmpty()) continue
                parsedTasks.add(
                    ScheduleTaskEntity(
                        id = 0L,
                        title = title,
                        dateIso = dateIso,
                        startMinuteOfDay = o.optInt("startMinuteOfDay", 9 * 60),
                        endMinuteOfDay = o.optInt("endMinuteOfDay", 10 * 60),
                        estimatedMinutes = o.optInt("estimatedMinutes", 60).coerceAtLeast(1),
                        quadrantName = o.optString("quadrantName", "Q2_IMPORTANT_NOT_URGENT"),
                        isCompleted = o.optBoolean("isCompleted", false),
                        isFlexible = o.optBoolean("isFlexible", false),
                        recurrenceName = o.optString("recurrenceName", "NONE"),
                        reminderEnabled = o.optBoolean("reminderEnabled", false),
                        reminderLeadMinutes = o.optInt("reminderLeadMinutes", 10),
                        completedAtEpochMs = if (o.isNull("completedAtEpochMs")) null else o.optLong("completedAtEpochMs"),
                        createdAtEpochMs = o.optLong("createdAtEpochMs", System.currentTimeMillis()),
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
            imageHelper.clearAllRecapImages()
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
                    ),
                )
            }

            taskDao.clearAllTasks()
            focusDao.clearAllSessions()
            recapDao.clearAllRecaps()

            if (parsedTasks.isNotEmpty()) taskDao.insertTasks(parsedTasks)
            if (parsedSessions.isNotEmpty()) focusDao.insertSessions(parsedSessions)
            if (parsedRecaps.isNotEmpty()) recapDao.insertRecaps(parsedRecaps)

            root.optJSONObject("preferences")?.let { p ->
                if (p.has("isDarkMode")) prefsManager.setDarkMode(p.optBoolean("isDarkMode", false))
                if (p.has("lastCustomFocusMinutes")) {
                    prefsManager.setLastFocusMinutes(p.optInt("lastCustomFocusMinutes", 25))
                }
            }
            prefsManager.setHasSeededInitialData(true)

            "Impor berhasil: ${parsedTasks.size} jadwal, ${parsedSessions.size} sesi fokus, dan ${parsedRecaps.size} rekap dimuat."
        }
    }
}
