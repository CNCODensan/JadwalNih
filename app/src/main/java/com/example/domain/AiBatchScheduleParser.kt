package com.example.domain

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object AiBatchScheduleParser {

    data class ImportedConflictItem(
        val importedIndex: Int,
        val importedTask: ScheduleTask,
        val conflictingExistingTasks: List<ScheduleTask>,
    )

    data class BatchParseResult(
        val tasks: List<ScheduleTask>,
        val conflictMessages: List<String>,
        val detailedConflicts: List<ImportedConflictItem> = emptyList(),
        val skippedCount: Int = 0,
        val errorMessage: String? = null,
    )

    fun buildAiPrompt(todayIso: String = DateTimeHelper.todayIso()): String {
        return """
            Halo! Tolong bantu ubah cerita atau rencana kegiatanku menjadi daftar jadwal terstruktur dalam format JSON Array murni (tanpa teks pembuka atau penutup tambahan) agar bisa langsung aku tempel ke aplikasi JadwalNih.

            Sebagai patokan waktu, tanggal hari ini adalah: $todayIso.
            Jika aku menceritakan banyak kegiatan sekaligus (baik untuk hari ini, besok, lusa, maupun beberapa hari ke depan), pecah semuanya menjadi objek terpisah di dalam satu array JSON.

            Gunakan format array JSON persis seperti ini:
            [
              {
                "title": "Nama kegiatan singkat dan jelas",
                "date": "YYYY-MM-DD",
                "startTime": "HH:MM",
                "endTime": "HH:MM",
                "priority": "K1 | K2 | K3 | K4",
                "icon": "work | school | mosque | fitness | meeting_room | restaurant | shopping_cart | music | flight | wallet",
                "location": "Nama tempat atau alamat (opsional)",
                "reminderMinutes": 15,
                "notes": "Catatan tambahan jika ada"
              }
            ]

            Panduan pemilihan prioritas:
            - K1: Penting, Mendesak
            - K2: Penting, Kapan aja
            - K3: Ga Penting/Biasa, Mendesak
            - K4: Ga penting/Biasa, Kapan aja

            Jika aku belum menyebutkan tanggal, gunakan tanggal hari ini ($todayIso). Jika aku belum menyebutkan jam selesai, perkirakan durasi yang masuk akal. Sekarang, tanyakan padaku: "Ceritain aja rencana kegiatanmu hari ini atau jadwal apa aja yang mau ditambahin!"
        """.trimIndent()
    }

    fun parse(
        rawText: String,
        defaultDateIso: String = DateTimeHelper.todayIso(),
        existingTasks: List<ScheduleTask> = emptyList(),
    ): BatchParseResult {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) {
            return BatchParseResult(
                tasks = emptyList(),
                conflictMessages = emptyList(),
                errorMessage = "Tempel hasil JSON dari AI dulu di kotak atas ya.",
            )
        }

        val extractedObjects = extractAllScheduleObjects(cleaned)
        if (extractedObjects == null) {
            return BatchParseResult(
                tasks = emptyList(),
                conflictMessages = emptyList(),
                errorMessage = "Format harus berupa JSON (contoh: [{\"title\":\"Rapat\",\"date\":\"$defaultDateIso\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"priority\":\"K1\"}]). Salin prompt di atas lalu tempel balasan JSON dari AI di sini ya.",
            )
        }

        if (extractedObjects.isEmpty()) {
            return BatchParseResult(
                tasks = emptyList(),
                conflictMessages = emptyList(),
                errorMessage = "Daftar jadwal JSON masih kosong ([]). Pastikan ada minimal 1 kegiatan di dalamnya.",
            )
        }

        val parsedTasks = mutableListOf<ScheduleTask>()
        val conflictMessages = mutableListOf<String>()
        val detailedConflicts = mutableListOf<ImportedConflictItem>()
        var skippedCount = 0
        val now = System.currentTimeMillis()

        extractedObjects.forEachIndexed { i, obj ->
            val title = firstNonBlank(
                obj.optString("title"),
                obj.optString("name"),
                obj.optString("kegiatan"),
                obj.optString("activity"),
                obj.optString("agenda"),
                obj.optString("judul"),
            )
            if (title.isEmpty()) {
                skippedCount++
                return@forEachIndexed
            }

            val rawDate = firstNonBlank(
                obj.optString("date"),
                obj.optString("dateIso"),
                obj.optString("tanggal"),
                obj.optString("hari"),
            )
            val dateIso = normalizeDateIso(rawDate, defaultDateIso)

            val startStr = firstNonBlank(
                obj.optString("startTime"),
                obj.optString("start_time"),
                obj.optString("start"),
                obj.optString("jamMulai"),
                obj.optString("mulai"),
                obj.optString("jam"),
            )
            val endStr = firstNonBlank(
                obj.optString("endTime"),
                obj.optString("end_time"),
                obj.optString("end"),
                obj.optString("jamSelesai"),
                obj.optString("selesai"),
            )

            val startMin = parseMinuteOfDay(startStr) ?: 9 * 60
            val rawEndMin = parseMinuteOfDay(endStr)
            val endMin = when {
                rawEndMin != null && rawEndMin > startMin -> rawEndMin
                else -> (startMin + 60).coerceAtMost(23 * 60 + 59)
            }

            val priorityStr = firstNonBlank(
                obj.optString("priority"),
                obj.optString("quadrant"),
                obj.optString("prioritas"),
            )
            val quadrant = parsePriority(priorityStr)

            val (reminderEnabled, reminderLeadMinutes) = parseReminder(obj)

            val notes = firstNonBlank(
                obj.optString("notes"),
                obj.optString("note"),
                obj.optString("catatan"),
                obj.optString("deskripsi"),
            )

            val rawIcon = firstNonBlank(
                obj.optString("icon"),
                obj.optString("iconKey"),
                obj.optString("ikon"),
            )
            val resolvedIconKey = TaskIconCatalog.byKey(rawIcon)?.key ?: TaskIconCatalog.autoDetectIconKey(title)

            val locationName = firstNonBlank(
                obj.optString("location"),
                obj.optString("locationName"),
                obj.optString("lokasi"),
                obj.optString("tempat"),
                obj.optString("alamat"),
            )

            val subtasks = parseSubtasks(obj)
            val isFlexible = obj.optBoolean("isFlexible", false) || obj.optBoolean("flexible", false)
            val recurrence = parseRecurrence(
                firstNonBlank(obj.optString("recurrence"), obj.optString("pengulangan")),
            )

            val task = ScheduleTask(
                id = 0L,
                title = title,
                dateIso = dateIso,
                startMinuteOfDay = startMin,
                endMinuteOfDay = endMin,
                estimatedMinutes = (endMin - startMin).coerceAtLeast(1),
                quadrant = quadrant,
                isCompleted = false,
                isFlexible = isFlexible,
                recurrence = recurrence,
                reminderEnabled = reminderEnabled,
                reminderLeadMinutes = reminderLeadMinutes,
                completedAtEpochMs = null,
                createdAtEpochMs = now + i,
                subtasks = subtasks,
                attendanceStatus = AttendanceStatus.UNCONFIRMED,
                isSubtasksTodoMode = subtasks.isNotEmpty(),
                notes = notes,
                iconKey = resolvedIconKey,
                locationName = locationName,
            )

            val existingConflicts = ScheduleAnalyzer.detectConflictsForDraft(
                dateIso = task.dateIso,
                startMinuteOfDay = task.startMinuteOfDay,
                endMinuteOfDay = task.endMinuteOfDay,
                excludeTaskId = -1L,
                existingTasks = existingTasks,
            )
            val batchConflicts = ScheduleAnalyzer.detectConflictsForDraft(
                dateIso = task.dateIso,
                startMinuteOfDay = task.startMinuteOfDay,
                endMinuteOfDay = task.endMinuteOfDay,
                excludeTaskId = -1L,
                existingTasks = parsedTasks,
            )
            val allConflicts = existingConflicts + batchConflicts
            if (allConflicts.isNotEmpty()) {
                val names = allConflicts.joinToString(", ") { it.title }
                conflictMessages.add(
                    "${task.title} (${DateTimeHelper.formatTimeRange(task.startMinuteOfDay, task.endMinuteOfDay)}) bentrok dengan: $names",
                )
            }
            if (existingConflicts.isNotEmpty()) {
                detailedConflicts.add(
                    ImportedConflictItem(
                        importedIndex = parsedTasks.size,
                        importedTask = task,
                        conflictingExistingTasks = existingConflicts,
                    ),
                )
            }

            parsedTasks.add(task)
        }

        if (parsedTasks.isEmpty()) {
            return BatchParseResult(
                tasks = emptyList(),
                conflictMessages = emptyList(),
                skippedCount = skippedCount,
                errorMessage = "JSON terbaca, tapi belum ada item jadwal yang punya \"title\" / \"judul\" kegiatan.",
            )
        }

        return BatchParseResult(
            tasks = parsedTasks,
            conflictMessages = conflictMessages,
            detailedConflicts = detailedConflicts,
            skippedCount = skippedCount,
            errorMessage = null,
        )
    }

    fun detectDetailedConflicts(
        importedTasks: List<ScheduleTask>,
        existingTasks: List<ScheduleTask>,
    ): List<ImportedConflictItem> {
        val items = mutableListOf<ImportedConflictItem>()
        importedTasks.forEachIndexed { idx, task ->
            val conflicts = ScheduleAnalyzer.detectConflictsForDraft(
                dateIso = task.dateIso,
                startMinuteOfDay = task.startMinuteOfDay,
                endMinuteOfDay = task.endMinuteOfDay,
                excludeTaskId = -1L,
                existingTasks = existingTasks,
            )
            if (conflicts.isNotEmpty()) {
                items.add(
                    ImportedConflictItem(
                        importedIndex = idx,
                        importedTask = task,
                        conflictingExistingTasks = conflicts,
                    ),
                )
            }
        }
        return items
    }

    /**
     * Extracts ALL JSON arrays and standalone JSON objects from AI responses,
     * fixing the multi-day/multi-block bug where `lastIndexOf(']')` or single-object
     * extraction dropped everything except the first/last item.
     */
    private fun extractAllScheduleObjects(rawText: String): List<JSONObject>? {
        val withoutFences = rawText
            .replace(Regex("```(?:json)?\\s*", RegexOption.IGNORE_CASE), "\n")
            .replace("```", "\n")
            .trim()

        val collected = mutableListOf<JSONObject>()
        var foundAnyJsonSyntax = false

        var i = 0
        val len = withoutFences.length
        while (i < len) {
            val ch = withoutFences[i]
            if (ch == '[' || ch == '{') {
                val endIndex = findMatchingDelimiter(withoutFences, i)
                if (endIndex > i) {
                    val slice = sanitizeTrailingCommas(withoutFences.substring(i, endIndex + 1))
                    if (ch == '[') {
                        val parsedArray = runCatching { JSONArray(slice) }.getOrNull()
                        if (parsedArray != null) {
                            foundAnyJsonSyntax = true
                            collectFromArray(parsedArray, collected)
                            i = endIndex + 1
                            continue
                        }
                    } else {
                        val parsedObj = runCatching { JSONObject(slice) }.getOrNull()
                        if (parsedObj != null) {
                            foundAnyJsonSyntax = true
                            collectFromObject(parsedObj, collected)
                            i = endIndex + 1
                            continue
                        }
                    }
                }
            }
            i++
        }

        if (!foundAnyJsonSyntax) {
            return null
        }
        return collected
    }

    private fun findMatchingDelimiter(text: String, start: Int): Int {
        val open = text[start]
        val close = if (open == '[') ']' else '}'
        var depth = 0
        var inString = false
        var escaped = false

        for (idx in start until text.length) {
            val c = text[idx]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\' && inString) {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (inString) continue

            if (c == open) {
                depth++
            } else if (c == close) {
                depth--
                if (depth == 0) return idx
            }
        }
        return -1
    }

    private fun collectFromArray(arr: JSONArray, out: MutableList<JSONObject>) {
        for (idx in 0 until arr.length()) {
            val elem = arr.opt(idx)
            when (elem) {
                is JSONObject -> collectFromObject(elem, out)
                is JSONArray -> collectFromArray(elem, out)
            }
        }
    }

    private fun collectFromObject(obj: JSONObject, out: MutableList<JSONObject>) {
        // Direct schedule object if it has title/name/kegiatan/etc.
        val hasTitleKey = listOf("title", "name", "kegiatan", "activity", "agenda", "judul")
            .any { key -> obj.optString(key).let { it.isNotBlank() && it != "null" } }
        if (hasTitleKey) {
            out.add(obj)
            return
        }

        // Otherwise unwrap any nested arrays/objects (e.g., {"schedules": [...], "hari_1": [...], "days": {...}})
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            when (val value = obj.opt(k)) {
                is JSONArray -> collectFromArray(value, out)
                is JSONObject -> collectFromObject(value, out)
            }
        }
    }

    private fun sanitizeTrailingCommas(json: String): String {
        return json
            .replace(Regex(",\\s*\\]"), "]")
            .replace(Regex(",\\s*\\}"), "}")
    }

    private fun normalizeDateIso(rawDate: String, fallback: String): String {
        val isoMatch = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""").find(rawDate)
        if (isoMatch != null) {
            val y = isoMatch.groupValues[1].toIntOrNull() ?: return fallback
            val m = isoMatch.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: return fallback
            val d = isoMatch.groupValues[3].toIntOrNull()?.coerceIn(1, 31) ?: return fallback
            return "%04d-%02d-%02d".format(y, m, d)
        }
        val dmyMatch = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{4})\b""").find(rawDate)
        if (dmyMatch != null) {
            val d = dmyMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 31) ?: return fallback
            val m = dmyMatch.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: return fallback
            val y = dmyMatch.groupValues[3].toIntOrNull() ?: return fallback
            return "%04d-%02d-%02d".format(y, m, d)
        }
        return fallback
    }

    private fun parseMinuteOfDay(rawTime: String): Int? {
        if (rawTime.isBlank()) return null
        val match = Regex("""(\d{1,2})[:.](\d{2})""").find(rawTime.trim()) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun parsePriority(raw: String): PriorityQuadrant {
        val upper = raw.trim().uppercase()
        return when {
            upper.startsWith("K1") || upper.startsWith("Q1") -> PriorityQuadrant.Q1_IMPORTANT_URGENT
            upper.startsWith("K2") || upper.startsWith("Q2") -> PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT
            upper.startsWith("K3") || upper.startsWith("Q3") -> PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT
            upper.startsWith("K4") || upper.startsWith("Q4") -> PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT
            upper.contains("KRITIS") || (upper.contains("PENTING") && upper.contains("MENDESAK") && !upper.contains("GA ")) ->
                PriorityQuadrant.Q1_IMPORTANT_URGENT
            upper.contains("CEPAT") -> PriorityQuadrant.Q3_NOT_IMPORTANT_URGENT
            upper.contains("SANTAI") -> PriorityQuadrant.Q4_NOT_IMPORTANT_NOT_URGENT
            else -> PriorityQuadrant.Q2_IMPORTANT_NOT_URGENT
        }
    }

    private fun parseReminder(obj: JSONObject): Pair<Boolean, Int> {
        val key = listOf("reminderMinutes", "reminder_minutes", "reminderLeadMinutes", "pengingatMenit")
            .firstOrNull { obj.has(it) }
        if (key != null) {
            if (obj.isNull(key)) return false to 15
            val rawVal = obj.opt(key)
            val mins = when (rawVal) {
                is Number -> rawVal.toInt()
                is String -> rawVal.trim().toIntOrNull()
                is Boolean -> if (rawVal) 15 else -1
                else -> null
            }
            return if (mins != null && mins >= 0) {
                true to mins.coerceIn(0, 180)
            } else {
                false to 15
            }
        }
        val reminderBool = obj.optBoolean("reminderEnabled", false)
        return reminderBool to 15
    }

    private fun parseSubtasks(obj: JSONObject): List<SubTaskItem> {
        val arr = obj.optJSONArray("checklist")
            ?: obj.optJSONArray("subtasks")
            ?: obj.optJSONArray("steps")
            ?: return emptyList()

        val list = mutableListOf<SubTaskItem>()
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            when (item) {
                is String -> {
                    val text = item.trim()
                    if (text.isNotEmpty()) {
                        list.add(
                            SubTaskItem(
                                id = "ai_sub_${UUID.randomUUID()}",
                                title = text,
                                isDone = false,
                            ),
                        )
                    }
                }
                is JSONObject -> {
                    val text = firstNonBlank(
                        item.optString("title"),
                        item.optString("t"),
                        item.optString("text"),
                        item.optString("name"),
                    )
                    if (text.isNotEmpty()) {
                        val done = item.optBoolean("isDone", item.optBoolean("d", item.optBoolean("done", false)))
                        list.add(
                            SubTaskItem(
                                id = item.optString("id").ifBlank { "ai_sub_${UUID.randomUUID()}" },
                                title = text,
                                isDone = done,
                            ),
                        )
                    }
                }
            }
        }
        return list
    }

    private fun parseRecurrence(raw: String): RecurrenceRule {
        val upper = raw.trim().uppercase()
        return when {
            upper.contains("DAILY") || upper.contains("HARIAN") -> RecurrenceRule.DAILY
            upper.contains("WEEKLY") || upper.contains("MINGGUAN") -> RecurrenceRule.WEEKLY
            upper.contains("MONTHLY") || upper.contains("BULANAN") -> RecurrenceRule.MONTHLY
            else -> RecurrenceRule.NONE
        }
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() && it != "null" }?.trim().orEmpty()
    }
}
