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

    // Default fallback coordinates: Jakarta, Indonesia
    const val DEFAULT_LATITUDE = -6.2088
    const val DEFAULT_LONGITUDE = 106.8456

    private val islamicCache = ConcurrentHashMap<String, List<Pair<String, Int>>>()

    suspend fun getDailyWorshipSchedule(
        dateIso: String,
        religion: ReligionOption,
        latitude: Double?,
        longitude: Double?,
        completedKeys: Set<String> = emptySet(),
    ): List<WorshipScheduleItem> {
        val lat = latitude ?: DEFAULT_LATITUDE
        val lng = longitude ?: DEFAULT_LONGITUDE

        val rawSlots: List<Triple<String, String, Int>> = if (religion.usesIslamicPrayerTimes) {
            val times = getOrComputeIslamicTimes(dateIso, lat, lng)
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
    ): List<WorshipScheduleItem> {
        val lat = latitude ?: DEFAULT_LATITUDE
        val lng = longitude ?: DEFAULT_LONGITUDE

        if (religion.usesIslamicPrayerTimes) {
            val cacheKey = buildCacheKey(dateIso, lat, lng)
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

    private suspend fun getOrComputeIslamicTimes(
        dateIso: String,
        lat: Double,
        lng: Double,
    ): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(dateIso, lat, lng)
        islamicCache[cacheKey]?.let { return@withContext it }

        val fromApi = runCatching { fetchAladhanTimes(dateIso, lat, lng) }.getOrNull()
        val resolved = if (!fromApi.isNullOrEmpty()) {
            fromApi
        } else {
            computeOfflineIslamicPrayerMinutes(dateIso, lat, lng)
        }
        islamicCache[cacheKey] = resolved
        resolved
    }

    private fun buildCacheKey(dateIso: String, lat: Double, lng: Double): String {
        return String.format(Locale.US, "%s_%.2f_%.2f", dateIso, lat, lng)
    }

    private fun fetchAladhanTimes(dateIso: String, lat: Double, lng: Double): List<Pair<String, Int>> {
        val parts = dateIso.split("-")
        if (parts.size != 3) return emptyList()
        val dmy = "${parts[2]}-${parts[1]}-${parts[0]}"
        val url = URL("https://api.aladhan.com/v1/timings/$dmy?latitude=$lat&longitude=$lng&method=20")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseAladhanJson(body)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseAladhanJson(rawJson: String): List<Pair<String, Int>> {
        val root = JSONObject(rawJson)
        val timings = root.optJSONObject("data")?.optJSONObject("timings") ?: return emptyList()
        fun parseHHmm(field: String): Int? {
            val raw = timings.optString(field).take(5)
            val match = Regex("""(\d{2}):(\d{2})""").find(raw) ?: return null
            val h = match.groupValues[1].toIntOrNull() ?: return null
            val m = match.groupValues[2].toIntOrNull() ?: return null
            return (h * 60 + m).coerceIn(0, 23 * 60 + 59)
        }
        val fajr = parseHHmm("Fajr") ?: return emptyList()
        val dhuhr = parseHHmm("Dhuhr") ?: return emptyList()
        val asr = parseHHmm("Asr") ?: return emptyList()
        val maghrib = parseHHmm("Maghrib") ?: return emptyList()
        val isha = parseHHmm("Isha") ?: return emptyList()

        return listOf(
            "subuh" to fajr,
            "dzuhur" to dhuhr,
            "ashar" to asr,
            "maghrib" to maghrib,
            "isya" to isha,
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
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))

        val midDay = fixHour(12.0 - eqTime + tzOffsetHours - longitude / 15.0)

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
