package com.example.domain

import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object PrayerTimesService {

    // Default fallback coordinates & city ID: Jakarta Pusat, Indonesia
    const val DEFAULT_LATITUDE = -6.2088
    const val DEFAULT_LONGITUDE = 106.8456
    const val DEFAULT_MYQURAN_CITY_ID = "1301"

    private val islamicCache = ConcurrentHashMap<String, List<Pair<String, Int>>>()
    private val citiesCache = mutableListOf<MyQuranCity>()

    suspend fun getDailyWorshipSchedule(
        dateIso: String,
        religion: ReligionOption,
        latitude: Double?,
        longitude: Double?,
        completedKeys: Set<String> = emptySet(),
        locationName: String? = null,
    ): List<WorshipScheduleItem> {
        val lat = latitude ?: DEFAULT_LATITUDE
        val lng = longitude ?: DEFAULT_LONGITUDE

        val rawSlots: List<Triple<String, String, Int>> = if (religion.usesIslamicPrayerTimes) {
            val times = getOrComputeIslamicTimes(dateIso, lat, lng, locationName)
            val subtitles = mapOf(
                "subuh" to "Sholat Subuh • 2 rakaat",
                "dzuhur" to "Sholat Dzuhur • 4 rakaat",
                "ashar" to "Sholat Ashar • 4 rakaat",
                "maghrib" to "Sholat Maghrib • 3 rakaat",
                "isya" to "Sholat Isya • 4 rakaat",
            )
            val labels = mapOf(
                "subuh" to "Subuh",
                "dzuhur" to "Dzuhur",
                "ashar" to "Ashar",
                "maghrib" to "Maghrib",
                "isya" to "Isya",
            )
            times.map { (key, minute) ->
                Triple(key, labels[key] ?: key, minute)
            }.let { list ->
                return list.map { (key, title, min) ->
                    val composite = "$dateIso|$key"
                    WorshipScheduleItem(
                        key = key,
                        title = title,
                        subtitle = subtitles[key] ?: "Waktu sholat",
                        minuteOfDay = min,
                        dateIso = dateIso,
                        isCompleted = composite in completedKeys || key in completedKeys,
                        isWeeklySpecial = false,
                    )
                }
            }
        } else {
            buildNonIslamicSlots(dateIso, religion)
        }

        val cal = DateTimeHelper.parseIsoToCalendar(dateIso)
        val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        return rawSlots.map { (key, title, min) ->
            val composite = "$dateIso|$key"
            val isWeekly = key.startsWith("weekly_")
            val subtitle = when {
                isWeekly && isSunday -> "Ibadah mingguan hari ini"
                else -> when (religion) {
                    ReligionOption.KRISTEN -> "Saat teduh & doa harian"
                    ReligionOption.KATOLIK -> "Doa Angelus / refleksi harian"
                    ReligionOption.HINDU -> "Trisandya & puja harian"
                    ReligionOption.BUDDHA -> "Sutta & meditasi harian"
                    ReligionOption.KONGHUCU -> "Sembahyang & renungan harian"
                    else -> "Refleksi syukur harian"
                }
            }
            WorshipScheduleItem(
                key = key,
                title = title,
                subtitle = subtitle,
                minuteOfDay = min,
                dateIso = dateIso,
                isCompleted = composite in completedKeys || key in completedKeys,
                isWeeklySpecial = isWeekly,
            )
        }
    }

    fun getCachedOrOfflineWorshipSchedule(
        dateIso: String,
        religion: ReligionOption,
        latitude: Double?,
        longitude: Double?,
        completedKeys: Set<String> = emptySet(),
        locationName: String? = null,
    ): List<WorshipScheduleItem> {
        val lat = latitude ?: DEFAULT_LATITUDE
        val lng = longitude ?: DEFAULT_LONGITUDE

        if (religion.usesIslamicPrayerTimes) {
            val cityKey = normalizeCityHint(locationName)
            val cacheKey = buildCacheKey(dateIso, lat, lng, cityKey)
            val times = islamicCache[cacheKey] ?: computeOfflineIslamicPrayerMinutes(dateIso, lat, lng)
            val subtitles = mapOf(
                "subuh" to "Sholat Subuh • 2 rakaat",
                "dzuhur" to "Sholat Dzuhur • 4 rakaat",
                "ashar" to "Sholat Ashar • 4 rakaat",
                "maghrib" to "Sholat Maghrib • 3 rakaat",
                "isya" to "Sholat Isya • 4 rakaat",
            )
            val labels = mapOf(
                "subuh" to "Subuh",
                "dzuhur" to "Dzuhur",
                "ashar" to "Ashar",
                "maghrib" to "Maghrib",
                "isya" to "Isya",
            )
            return times.map { (key, min) ->
                val composite = "$dateIso|$key"
                WorshipScheduleItem(
                    key = key,
                    title = labels[key] ?: key,
                    subtitle = subtitles[key] ?: "Waktu sholat",
                    minuteOfDay = min,
                    dateIso = dateIso,
                    isCompleted = composite in completedKeys || key in completedKeys,
                )
            }
        }

        val slots = buildNonIslamicSlots(dateIso, religion)
        return slots.map { (key, title, min) ->
            val composite = "$dateIso|$key"
            WorshipScheduleItem(
                key = key,
                title = title,
                subtitle = if (key.startsWith("weekly_")) "Ibadah mingguan" else "Doa harian",
                minuteOfDay = min,
                dateIso = dateIso,
                isCompleted = composite in completedKeys || key in completedKeys,
                isWeeklySpecial = key.startsWith("weekly_"),
            )
        }
    }

    fun findNextPendingWorship(
        items: List<WorshipScheduleItem>,
        nowMinuteOfDay: Int,
    ): Pair<WorshipScheduleItem?, String> {
        if (items.isEmpty()) return null to ""
        val activeOrUpcoming = items.firstOrNull { !it.isCompleted && it.minuteOfDay >= nowMinuteOfDay - 20 }
            ?: items.firstOrNull { !it.isCompleted }
            ?: items.first()

        val diff = activeOrUpcoming.minuteOfDay - nowMinuteOfDay
        val label = when {
            activeOrUpcoming.isCompleted -> "Sudah ibadah"
            diff in -20..0 -> "Sedang waktunya sekarang"
            diff > 0 -> {
                val h = diff / 60
                val m = diff % 60
                if (h > 0) "${h}j ${m}m lagi" else "$m mnt lagi"
            }
            else -> "Lewat ${DateTimeHelper.formatMinuteOfDay(activeOrUpcoming.minuteOfDay)}"
        }
        return activeOrUpcoming to label
    }

    fun computeExpectedWeeklyWorshipCount(religion: ReligionOption): Int {
        // Islam: 5 * 7 = 35; other religions: 2 daily * 7 + 1 weekly = 15
        return if (religion.usesIslamicPrayerTimes) 35 else 15
    }

    /**
     * Resolves the natural active worship window for today's schedule.
     * - Islamic prayer windows:
     *   Subuh -> Syuruq (sunrise), Dzuhur -> Ashar - 1, Ashar -> Maghrib - 1,
     *   Maghrib -> Isya - 1, Isya -> 23:59.
     * - Non-Islamic windows: [slot.minuteOfDay .. slot.minuteOfDay + 120].
     */
    fun resolveActiveWorshipWindow(
        items: List<WorshipScheduleItem>,
        religion: ReligionOption,
        dateIso: String,
        nowMinuteOfDay: Int,
        latitude: Double? = null,
        longitude: Double? = null,
    ): WorshipActiveWindow {
        if (items.isEmpty()) return WorshipActiveWindow()
        val (nextItem, nextLabel) = findNextPendingWorship(items, nowMinuteOfDay)

        if (religion.usesIslamicPrayerTimes) {
            val subuh = items.firstOrNull { it.key == "subuh" }
            val dzuhur = items.firstOrNull { it.key == "dzuhur" }
            val ashar = items.firstOrNull { it.key == "ashar" }
            val maghrib = items.firstOrNull { it.key == "maghrib" }
            val isya = items.firstOrNull { it.key == "isya" }

            val sunrise = computeOfflineSunriseMinute(
                dateIso = dateIso,
                latitude = latitude ?: DEFAULT_LATITUDE,
                longitude = longitude ?: DEFAULT_LONGITUDE,
            ).coerceIn(
                minimumValue = (subuh?.minuteOfDay ?: 270) + 55,
                maximumValue = ((dzuhur?.minuteOfDay ?: 720) - 60).coerceAtLeast((subuh?.minuteOfDay ?: 270) + 60),
            )

            val windows = buildList {
                if (subuh != null) add(Triple(subuh, subuh.minuteOfDay, sunrise))
                if (dzuhur != null) add(Triple(dzuhur, dzuhur.minuteOfDay, ((ashar?.minuteOfDay ?: 915) - 1).coerceAtLeast(dzuhur.minuteOfDay + 30)))
                if (ashar != null) add(Triple(ashar, ashar.minuteOfDay, ((maghrib?.minuteOfDay ?: 1080) - 1).coerceAtLeast(ashar.minuteOfDay + 30)))
                if (maghrib != null) add(Triple(maghrib, maghrib.minuteOfDay, ((isya?.minuteOfDay ?: 1150) - 1).coerceAtLeast(maghrib.minuteOfDay + 20)))
                if (isya != null) add(Triple(isya, isya.minuteOfDay, 23 * 60 + 59))
            }

            val activeTriple = windows.firstOrNull { (_, startMin, endMin) ->
                nowMinuteOfDay in startMin..endMin
            }
            return WorshipActiveWindow(
                currentItem = activeTriple?.first,
                windowStartMinute = activeTriple?.second ?: 0,
                windowEndMinute = activeTriple?.third ?: 0,
                nextItem = nextItem,
                nextCountdownLabel = nextLabel,
                isWithinNaturalWindow = activeTriple != null,
                todayItems = items,
            )
        }

        val activeSlot = items.firstOrNull { slot ->
            val end = (slot.minuteOfDay + 120).coerceAtMost(23 * 60 + 59)
            nowMinuteOfDay in slot.minuteOfDay..end
        }
        return WorshipActiveWindow(
            currentItem = activeSlot,
            windowStartMinute = activeSlot?.minuteOfDay ?: 0,
            windowEndMinute = activeSlot?.let { (it.minuteOfDay + 120).coerceAtMost(23 * 60 + 59) } ?: 0,
            nextItem = nextItem,
            nextCountdownLabel = nextLabel,
            isWithinNaturalWindow = activeSlot != null,
            todayItems = items,
        )
    }

    fun buildWorshipHistoryDays(
        todayIso: String,
        daysBack: Int = 7,
        religion: ReligionOption,
        latitude: Double?,
        longitude: Double?,
        completedKeys: Set<String>,
        locationName: String?,
        allowMissedManualEdit: Boolean,
    ): List<WorshipHistoryDay> {
        return (0..daysBack.coerceAtLeast(0)).map { offset ->
            val dayIso = DateTimeHelper.addDays(todayIso, -offset)
            val isToday = offset == 0
            val schedule = getCachedOrOfflineWorshipSchedule(
                dateIso = dayIso,
                religion = religion,
                latitude = latitude,
                longitude = longitude,
                completedKeys = completedKeys,
                locationName = locationName,
            )
            WorshipHistoryDay(
                dateIso = dayIso,
                items = schedule,
                isToday = isToday,
                isEditable = isToday || allowMissedManualEdit,
            )
        }
    }

    private suspend fun getOrComputeIslamicTimes(
        dateIso: String,
        lat: Double,
        lng: Double,
        locationName: String?,
    ): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val cityHint = normalizeCityHint(locationName)
        val cacheKey = buildCacheKey(dateIso, lat, lng, cityHint)
        islamicCache[cacheKey]?.let { return@withContext it }

        val resolvedCityId = runCatching { resolveMyQuranCityId(cityHint, lat, lng) }.getOrNull()
        val fromMyQuran = if (resolvedCityId != null) {
            runCatching { fetchMyQuranTimes(cityId = resolvedCityId, dateIso = dateIso) }.getOrNull()
        } else {
            null
        }
        val resolved = if (!fromMyQuran.isNullOrEmpty()) {
            fromMyQuran
        } else {
            computeOfflineIslamicPrayerMinutes(dateIso, lat, lng)
        }
        islamicCache[cacheKey] = resolved
        resolved
    }

    private fun normalizeCityHint(locationName: String?): String {
        return locationName.orEmpty().trim()
    }

    private fun buildCacheKey(dateIso: String, lat: Double, lng: Double, cityHint: String): String {
        val cityToken = cityHint.lowercase(Locale.ROOT).replace(Regex("""\s+"""), "_")
        return String.format(Locale.US, "%s_%.2f_%.2f_%s", dateIso, lat, lng, cityToken)
    }

    private fun resolveMyQuranCityId(cityHint: String, lat: Double, lng: Double): String? {
        if (cityHint.isBlank()) {
            val isNearJakarta = kotlin.math.abs(lat - DEFAULT_LATITUDE) < 0.35 &&
                kotlin.math.abs(lng - DEFAULT_LONGITUDE) < 0.35
            return if (isNearJakarta) DEFAULT_MYQURAN_CITY_ID else null
        }

        val cities = getOrFetchMyQuranCities()
        return matchMyQuranCityId(cityHint, cities)
    }

    private fun getOrFetchMyQuranCities(): List<MyQuranCity> {
        synchronized(citiesCache) {
            if (citiesCache.isNotEmpty()) return citiesCache.toList()
        }

        val v2Body = runCatching { fetchUrl("https://api.myquran.com/v2/sholat/kota/semua") }.getOrNull()
        val parsedV2 = v2Body?.let { parseMyQuranCitiesJson(it) }.orEmpty()
        val resolved = if (parsedV2.isNotEmpty()) {
            parsedV2
        } else {
            val v3Body = runCatching { fetchUrl("https://api.myquran.com/v3/sholat/kabkota/all") }.getOrNull()
            v3Body?.let { parseMyQuranCitiesJson(it) }.orEmpty()
        }
        if (resolved.isNotEmpty()) {
            synchronized(citiesCache) {
                citiesCache.clear()
                citiesCache.addAll(resolved)
            }
        }
        return resolved
    }

    internal fun matchMyQuranCityId(locationHint: String, cities: List<MyQuranCity>): String? {
        val raw = locationHint.trim()
        if (raw.isEmpty() || cities.isEmpty()) return null

        val segments = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val candidates = (segments + raw).distinct()

        fun normalizeName(input: String): String {
            return input.uppercase(Locale.ROOT)
                .replace("KABUPATEN ", "KAB. ")
                .replace("KAB ", "KAB. ")
                .replace(Regex("""[^A-Z0-9.\s]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        fun stripPrefix(input: String): String {
            return input
                .removePrefix("KOTA ")
                .removePrefix("KAB. ")
                .removePrefix("KAB ")
                .removePrefix("DKI ")
                .removePrefix("DI ")
                .trim()
        }

        for (candidate in candidates) {
            val norm = normalizeName(candidate)
            if (norm.length < 3) continue
            val bare = stripPrefix(norm)

            // 1. Exact normalized match
            cities.firstOrNull { normalizeName(it.lokasi) == norm }?.let { return it.id }

            // 2. Jakarta special handling
            if (bare == "JAKARTA" || norm == "DKI JAKARTA") {
                cities.firstOrNull { normalizeName(it.lokasi).contains("JAKARTA PUSAT") }?.let { return it.id }
            }

            // 3. Exact bare match preferring KOTA over KAB. when generic name given
            val bareMatches = cities.filter { stripPrefix(normalizeName(it.lokasi)) == bare }
            if (bareMatches.isNotEmpty()) {
                val preferKota = norm.startsWith("KOTA ") || !norm.startsWith("KAB.")
                return (if (preferKota) {
                    bareMatches.firstOrNull { it.lokasi.uppercase(Locale.ROOT).startsWith("KOTA ") }
                } else {
                    bareMatches.firstOrNull { it.lokasi.uppercase(Locale.ROOT).startsWith("KAB") }
                } ?: bareMatches.first()).id
            }
        }

        // 4. Substring fallback across segments
        for (candidate in candidates) {
            val bare = stripPrefix(normalizeName(candidate))
            if (bare.length < 4) continue
            cities.firstOrNull { normalizeName(it.lokasi).contains(bare) }?.let { return it.id }
        }
        return null
    }

    private fun fetchMyQuranTimes(cityId: String, dateIso: String): List<Pair<String, Int>> {
        val v2Body = runCatching {
            fetchUrl("https://api.myquran.com/v2/sholat/jadwal/$cityId/$dateIso")
        }.getOrNull()
        val fromV2 = v2Body?.let { parseMyQuranJadwalJson(it) }.orEmpty()
        if (fromV2.isNotEmpty()) return fromV2

        val v3Body = runCatching {
            fetchUrl("https://api.myquran.com/v3/sholat/jadwal/$cityId/$dateIso")
        }.getOrNull()
        return v3Body?.let { parseMyQuranJadwalJson(it) }.orEmpty()
    }

    private fun fetchUrl(endpoint: String): String? {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseMyQuranCitiesJson(rawJson: String): List<MyQuranCity> {
        val root = JSONObject(rawJson)
        val arr = root.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<MyQuranCity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val lokasi = obj.optString("lokasi").ifBlank { obj.optString("nama") }.trim()
            if (id.isNotEmpty() && lokasi.isNotEmpty()) {
                list.add(MyQuranCity(id = id, lokasi = lokasi))
            }
        }
        return list
    }

    internal fun parseMyQuranJadwalJson(rawJson: String): List<Pair<String, Int>> {
        val root = JSONObject(rawJson)
        val data = root.optJSONObject("data") ?: return emptyList()
        val jadwal = data.optJSONObject("jadwal") ?: return emptyList()

        fun parseHHmm(vararg fields: String): Int? {
            for (field in fields) {
                val raw = jadwal.optString(field).take(5)
                val match = Regex("""(\d{1,2}):(\d{2})""").find(raw) ?: continue
                val h = match.groupValues[1].toIntOrNull() ?: continue
                val m = match.groupValues[2].toIntOrNull() ?: continue
                return (h * 60 + m).coerceIn(0, 23 * 60 + 59)
            }
            return null
        }

        val subuh = parseHHmm("subuh", "fajr") ?: return emptyList()
        val dzuhur = parseHHmm("dzuhur", "dhuhr", "zuhur") ?: return emptyList()
        val ashar = parseHHmm("ashar", "asr") ?: return emptyList()
        val maghrib = parseHHmm("maghrib") ?: return emptyList()
        val isya = parseHHmm("isya", "isha") ?: return emptyList()

        return listOf(
            "subuh" to subuh,
            "dzuhur" to dzuhur,
            "ashar" to ashar,
            "maghrib" to maghrib,
            "isya" to isya,
        )
    }

    /**
     * Offline astronomical solar calculation calibrated to Indonesian Kemenag angles
     * (Fajr 20.0°, Isha 18.0°, Asr Shafi'i shadow=1).
     */
    fun computeOfflineIslamicPrayerMinutes(
        dateIso: String,
        latitude: Double = DEFAULT_LATITUDE,
        longitude: Double = DEFAULT_LONGITUDE,
    ): List<Pair<String, Int>> {
        val cal = DateTimeHelper.parseIsoToCalendar(dateIso)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val tzOffsetHours = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3_600_000.0
        val resolvedTz = if (kotlin.math.abs(tzOffsetHours) < 0.01 && kotlin.math.abs(longitude) > 15.0) {
            kotlin.math.round(longitude / 15.0)
        } else {
            tzOffsetHours
        }
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))

        val midDay = fixHour(12.0 - eqTime + resolvedTz - longitude / 15.0)

        fun sunAngleTime(angle: Double, isMorning: Boolean): Double {
            val num = -sinDeg(angle) - sinDeg(decl) * sinDeg(latitude)
            val den = cosDeg(decl) * cosDeg(latitude)
            val ratio = (num / den).coerceIn(-0.999, 0.999)
            val t = acosDeg(ratio) / 15.0
            return if (isMorning) midDay - t else midDay + t
        }

        fun asrTime(): Double {
            val angle = -atanDeg(1.0 / (1.0 + tanDeg(kotlin.math.abs(latitude - decl))))
            val num = -sinDeg(angle) - sinDeg(decl) * sinDeg(latitude)
            val den = cosDeg(decl) * cosDeg(latitude)
            val ratio = (num / den).coerceIn(-0.999, 0.999)
            val t = acosDeg(ratio) / 15.0
            return midDay + t
        }

        val fajr = hoursToMinutes(sunAngleTime(20.0, isMorning = true))
        val dhuhr = hoursToMinutes(midDay + 2.0 / 60.0) // 2 min ihtiyat safety
        val asr = hoursToMinutes(asrTime())
        val maghrib = hoursToMinutes(sunAngleTime(0.833, isMorning = false) + 2.0 / 60.0)
        val isha = hoursToMinutes(sunAngleTime(18.0, isMorning = false))

        return listOf(
            "subuh" to fajr,
            "dzuhur" to dhuhr,
            "ashar" to asr,
            "maghrib" to maghrib,
            "isya" to isha,
        )
    }

    private fun computeOfflineSunriseMinute(
        dateIso: String,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double = TimeZone.getDefault().getOffset(DateTimeHelper.parseIsoToCalendar(dateIso).timeInMillis) / 3_600_000.0,
    ): Int {
        val resolvedTz = if (kotlin.math.abs(tzOffsetHours) < 0.01 && kotlin.math.abs(longitude) > 15.0) {
            kotlin.math.round(longitude / 15.0)
        } else {
            tzOffsetHours
        }
        val parts = dateIso.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
        val jd = julianDate(y, m, day) - longitude / (15.0 * 24.0)
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))
        val midDay = fixHour(12.0 - eqTime + resolvedTz - longitude / 15.0)
        val num = -sinDeg(0.833) - sinDeg(decl) * sinDeg(latitude)
        val den = cosDeg(decl) * cosDeg(latitude)
        val ratio = (num / den).coerceIn(-0.999, 0.999)
        val t = acosDeg(ratio) / 15.0
        return hoursToMinutes(midDay - t)
    }

    private fun buildNonIslamicSlots(
        dateIso: String,
        religion: ReligionOption,
    ): List<Triple<String, String, Int>> {
        val cal = DateTimeHelper.parseIsoToCalendar(dateIso)
        val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        val base = when (religion) {
            ReligionOption.KRISTEN -> listOf(
                Triple("morning", "Doa Pagi & Renungan", 6 * 60 + 30),
                Triple("evening", "Doa Malam", 20 * 60 + 30),
            )
            ReligionOption.KATOLIK -> listOf(
                Triple("morning", "Doa Pagi", 6 * 60 + 30),
                Triple("evening", "Doa Malam", 20 * 60 + 30),
            )
            ReligionOption.HINDU -> listOf(
                Triple("morning", "Puja Trisandya Pagi", 6 * 60),
                Triple("evening", "Puja Trisandya Sore", 18 * 60),
            )
            ReligionOption.BUDDHA -> listOf(
                Triple("morning", "Puja & Meditasi Pagi", 6 * 60),
                Triple("evening", "Puja & Refleksi Malam", 20 * 60),
            )
            ReligionOption.KONGHUCU -> listOf(
                Triple("morning", "Sembahyang Pagi", 6 * 60 + 30),
                Triple("evening", "Sembahyang Malam", 19 * 60 + 30),
            )
            else -> listOf(
                Triple("morning", "Doa / Refleksi Pagi", 6 * 60 + 30),
                Triple("evening", "Doa / Refleksi Malam", 20 * 60 + 30),
            )
        }

        if (!isSunday) return base

        val weeklyLabel = when (religion) {
            ReligionOption.KRISTEN -> "Ibadah Minggu"
            ReligionOption.KATOLIK -> "Misa Minggu"
            ReligionOption.HINDU -> "Sembahyang Mingguan Pura"
            ReligionOption.BUDDHA -> "Kebaktian Vihara"
            ReligionOption.KONGHUCU -> "Sembahyang Klenteng"
            else -> "Ibadah Mingguan"
        }
        return listOf(base[0], Triple("weekly_sunday", weeklyLabel, 9 * 60), base[1])
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun hoursToMinutes(hours: Double): Int {
        val h = fixHour(hours)
        return (h * 60.0).toInt().coerceIn(0, 23 * 60 + 59)
    }

    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double): Double = a - 24.0 * floor(a / 24.0)
    private fun deg2rad(d: Double): Double = d * Math.PI / 180.0
    private fun rad2deg(r: Double): Double = r * 180.0 / Math.PI
    private fun sinDeg(d: Double): Double = sin(deg2rad(d))
    private fun cosDeg(d: Double): Double = cos(deg2rad(d))
    private fun tanDeg(d: Double): Double = tan(deg2rad(d))
    private fun asinDeg(x: Double): Double = rad2deg(asin(x.coerceIn(-1.0, 1.0)))
    private fun acosDeg(x: Double): Double = rad2deg(acos(x.coerceIn(-1.0, 1.0)))
    private fun atanDeg(x: Double): Double = rad2deg(atan(x))
    private fun atan2Deg(y: Double, x: Double): Double = rad2deg(atan2(y, x))
}
