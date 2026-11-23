package com.example.domain

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object PublicHolidayService {

    private val yearCache = ConcurrentHashMap<Int, List<HolidayInfo>>()

    suspend fun getHolidaysForYear(year: Int): List<HolidayInfo> = withContext(Dispatchers.IO) {
        yearCache[year]?.let { return@withContext it }

        val fromPrimaryApi = runCatching { fetchFromHariLiburVercel(year) }.getOrNull()
        val resolved = when {
            !fromPrimaryApi.isNullOrEmpty() -> fromPrimaryApi
            else -> runCatching { fetchFromNagerDate(year) }.getOrNull()?.takeIf { it.isNotEmpty() }
                ?: embeddedHolidaysForYear(year)
        }
        yearCache[year] = resolved
        resolved
    }

    fun getCachedOrEmbeddedHolidaysForYear(year: Int): List<HolidayInfo> {
        return yearCache[year] ?: embeddedHolidaysForYear(year)
    }

    fun resolveHolidayForDate(dateIso: String, holidays: List<HolidayInfo>): HolidayInfo? {
        return holidays.firstOrNull { it.dateIso == dateIso }
            ?: embeddedHolidays().firstOrNull { it.dateIso == dateIso }
    }

    fun resolveActiveHolidayTheme(
        todayIso: String,
        holidays: List<HolidayInfo>,
        holidayThemeEnabled: Boolean,
        previewTheme: HolidayThemeType = HolidayThemeType.NONE,
    ): HolidayThemeType {
        if (previewTheme != HolidayThemeType.NONE) return previewTheme
        if (!holidayThemeEnabled) return HolidayThemeType.NONE
        val holiday = resolveHolidayForDate(todayIso, holidays) ?: return HolidayThemeType.NONE
        if (holiday.isCutiBersama) return HolidayThemeType.NONE
        return holiday.themeType
    }

    fun mapHolidayNameToTheme(name: String): HolidayThemeType {
        val lower = name.lowercase()
        return when {
            lower.contains("kemerdekaan") || lower.contains("proklamasi") || lower.contains("pancasila") ->
                HolidayThemeType.INDEPENDENCE_DAY
            lower.contains("idul fitri") || lower.contains("idul adha") || lower.contains("maulid") ||
                lower.contains("isra") || lower.contains("tahun baru islam") || lower.contains("hijriah") ->
                HolidayThemeType.IDUL_FITRI
            lower.contains("imlek") -> HolidayThemeType.IMLEK
            lower.contains("natal") -> HolidayThemeType.NATAL
            lower.contains("nyepi") || lower.contains("waisak") -> HolidayThemeType.NYEPI_WAISAK
            else -> HolidayThemeType.NONE
        }
    }

    fun buildGreeting(name: String, themeType: HolidayThemeType): String {
        return when (themeType) {
            HolidayThemeType.INDEPENDENCE_DAY -> "Dirgahayu Republik Indonesia! Semangat merah putih hari ini."
            HolidayThemeType.IDUL_FITRI -> "Selamat $name! Semoga harimu penuh berkah dan damai."
            HolidayThemeType.IMLEK -> "Gong Xi Fa Cai! Selamat $name, semoga bawa hoki & sukacita."
            HolidayThemeType.NATAL -> "Selamat Hari Raya Natal! Semoga damai dan hangat menyertaimu."
            HolidayThemeType.NYEPI_WAISAK -> "Selamat $name! Semoga hari ini terasa tenang dan penuh makna."
            HolidayThemeType.NONE -> "Hari ini ada $name! Jangan lupa atur waktumu santai ya."
        }
    }

    private fun fetchFromHariLiburVercel(year: Int): List<HolidayInfo> {
        val url = URL("https://api-hari-libur.vercel.app/api?year=$year")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseHariLiburJson(body, year)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseHariLiburJson(rawJson: String, fallbackYear: Int): List<HolidayInfo> {
        val trimmed = rawJson.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("data") ?: obj.optJSONArray("holidays") ?: JSONArray()
            }
            else -> JSONArray()
        }
        val list = mutableListOf<HolidayInfo>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val rawDate = item.optString("event_date").ifBlank { item.optString("date") }
            val name = item.optString("event_name").ifBlank { item.optString("name") }.trim()
            if (rawDate.isBlank() || name.isBlank()) continue

            val normalizedDate = normalizeIsoDate(rawDate, fallbackYear) ?: continue
            val isNational = if (item.has("is_national_holiday")) {
                item.optBoolean("is_national_holiday", true)
            } else {
                true
            }
            val isCuti = name.lowercase().contains("cuti bersama")
            val theme = if (isCuti) HolidayThemeType.NONE else mapHolidayNameToTheme(name)
            list.add(
                HolidayInfo(
                    dateIso = normalizedDate,
                    name = name,
                    isNationalHoliday = isNational,
                    isCutiBersama = isCuti,
                    themeType = theme,
                    bannerGreeting = buildGreeting(name, theme),
                ),
            )
        }
        return list.sortedBy { it.dateIso }
    }

    private fun fetchFromNagerDate(year: Int): List<HolidayInfo> {
        val url = URL("https://date.nager.at/api/v3/PublicHolidays/$year/ID")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val list = mutableListOf<HolidayInfo>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val date = obj.optString("date").trim()
                val name = obj.optString("localName").ifBlank { obj.optString("name") }.trim()
                if (date.length == 10 && name.isNotBlank()) {
                    val theme = mapHolidayNameToTheme(name)
                    list.add(
                        HolidayInfo(
                            dateIso = date,
                            name = name,
                            isNationalHoliday = true,
                            isCutiBersama = false,
                            themeType = theme,
                            bannerGreeting = buildGreeting(name, theme),
                        ),
                    )
                }
            }
            list.sortedBy { it.dateIso }
        } finally {
            conn.disconnect()
        }
    }

    private fun normalizeIsoDate(raw: String, fallbackYear: Int): String? {
        val match = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""").find(raw) ?: return null
        val y = match.groupValues[1].toIntOrNull() ?: fallbackYear
        val m = match.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: return null
        val d = match.groupValues[3].toIntOrNull()?.coerceIn(1, 31) ?: return null
        return "%04d-%02d-%02d".format(y, m, d)
    }

    private fun embeddedHolidaysForYear(year: Int): List<HolidayInfo> {
        val exact = embeddedHolidays().filter { it.dateIso.startsWith("$year-") }
        if (exact.isNotEmpty()) return exact
        // Fixed annual national holidays as fallback for arbitrary future years
        return listOf(
            holiday("$year-01-01", "Tahun Baru Masehi"),
            holiday("$year-05-01", "Hari Buruh Internasional"),
            holiday("$year-06-01", "Hari Lahir Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
            holiday("$year-08-17", "Hari Kemerdekaan RI", HolidayThemeType.INDEPENDENCE_DAY),
            holiday("$year-12-25", "Hari Raya Natal", HolidayThemeType.NATAL),
        )
    }

    private fun embeddedHolidays(): List<HolidayInfo> = listOf(
        // 2025
        holiday("2025-01-01", "Tahun Baru 2025"),
        holiday("2025-01-27", "Isra Mikraj Nabi Muhammad SAW", HolidayThemeType.IDUL_FITRI),
        holiday("2025-01-29", "Tahun Baru Imlek 2576", HolidayThemeType.IMLEK),
        holiday("2025-03-29", "Hari Suci Nyepi", HolidayThemeType.NYEPI_WAISAK),
        holiday("2025-03-31", "Hari Raya Idul Fitri 1446 H", HolidayThemeType.IDUL_FITRI),
        holiday("2025-04-01", "Hari Raya Idul Fitri 1446 H", HolidayThemeType.IDUL_FITRI),
        holiday("2025-05-01", "Hari Buruh Internasional"),
        holiday("2025-05-12", "Hari Raya Waisak 2569 BE", HolidayThemeType.NYEPI_WAISAK),
        holiday("2025-06-01", "Hari Lahir Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2025-06-06", "Hari Raya Idul Adha 1446 H", HolidayThemeType.IDUL_FITRI),
        holiday("2025-06-27", "Tahun Baru Islam 1447 H", HolidayThemeType.IDUL_FITRI),
        holiday("2025-08-17", "Hari Kemerdekaan RI ke-80", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2025-09-05", "Maulid Nabi Muhammad SAW", HolidayThemeType.IDUL_FITRI),
        holiday("2025-12-25", "Hari Raya Natal", HolidayThemeType.NATAL),
        // 2026
        holiday("2026-01-01", "Tahun Baru 2026"),
        holiday("2026-01-16", "Isra Mikraj Nabi Muhammad SAW", HolidayThemeType.IDUL_FITRI),
        holiday("2026-02-17", "Tahun Baru Imlek 2577", HolidayThemeType.IMLEK),
        holiday("2026-03-19", "Hari Suci Nyepi", HolidayThemeType.NYEPI_WAISAK),
        holiday("2026-03-21", "Hari Raya Idul Fitri 1447 H", HolidayThemeType.IDUL_FITRI),
        holiday("2026-03-22", "Hari Raya Idul Fitri 1447 H", HolidayThemeType.IDUL_FITRI),
        holiday("2026-04-03", "Wafat Yesus Kristus"),
        holiday("2026-05-01", "Hari Buruh Internasional"),
        holiday("2026-05-27", "Hari Raya Idul Adha 1447 H", HolidayThemeType.IDUL_FITRI),
        holiday("2026-05-31", "Hari Raya Waisak 2570 BE", HolidayThemeType.NYEPI_WAISAK),
        holiday("2026-06-01", "Hari Lahir Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2026-06-16", "Tahun Baru Islam 1448 H", HolidayThemeType.IDUL_FITRI),
        holiday("2026-08-17", "Hari Kemerdekaan RI ke-81", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2026-08-25", "Maulid Nabi Muhammad SAW", HolidayThemeType.IDUL_FITRI),
        holiday("2026-12-25", "Hari Raya Natal", HolidayThemeType.NATAL),
        // 2027
        holiday("2027-01-01", "Tahun Baru 2027"),
        holiday("2027-02-06", "Tahun Baru Imlek 2578", HolidayThemeType.IMLEK),
        holiday("2027-03-09", "Hari Raya Idul Fitri 1448 H", HolidayThemeType.IDUL_FITRI),
        holiday("2027-03-10", "Hari Raya Idul Fitri 1448 H", HolidayThemeType.IDUL_FITRI),
        holiday("2027-05-01", "Hari Buruh Internasional"),
        holiday("2027-05-16", "Hari Raya Idul Adha 1448 H", HolidayThemeType.IDUL_FITRI),
        holiday("2027-05-20", "Hari Raya Waisak", HolidayThemeType.NYEPI_WAISAK),
        holiday("2027-06-01", "Hari Lahir Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2027-08-17", "Hari Kemerdekaan RI ke-82", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2027-12-25", "Hari Raya Natal", HolidayThemeType.NATAL),
        // 2028
        holiday("2028-01-01", "Tahun Baru 2028"),
        holiday("2028-01-26", "Tahun Baru Imlek 2579", HolidayThemeType.IMLEK),
        holiday("2028-02-27", "Hari Raya Idul Fitri 1449 H", HolidayThemeType.IDUL_FITRI),
        holiday("2028-05-01", "Hari Buruh Internasional"),
        holiday("2028-06-01", "Hari Lahir Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2028-08-17", "Hari Kemerdekaan RI ke-83", HolidayThemeType.INDEPENDENCE_DAY),
        holiday("2028-12-25", "Hari Raya Natal", HolidayThemeType.NATAL),
    )

    private fun holiday(
        dateIso: String,
        name: String,
        theme: HolidayThemeType? = null,
    ): HolidayInfo {
        val resolvedTheme = theme ?: mapHolidayNameToTheme(name)
        return HolidayInfo(
            dateIso = dateIso,
            name = name,
            isNationalHoliday = true,
            isCutiBersama = false,
            themeType = resolvedTheme,
            bannerGreeting = buildGreeting(name, resolvedTheme),
        )
    }
}
