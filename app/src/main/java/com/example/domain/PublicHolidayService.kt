package com.example.domain

import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object PublicHolidayService {

    private val yearCache = ConcurrentHashMap<Int, List<HolidayInfo>>()

    suspend fun getHolidaysForYear(year: Int): List<HolidayInfo> = withContext(Dispatchers.IO) {
        yearCache[year]?.let { return@withContext it }

        val fromDayOff = runCatching { fetchFromIndonesianEndpoint("https://dayoffapi.vercel.app/api?year=$year", year) }.getOrNull()
        val fromHariLibur = if (fromDayOff.isNullOrEmpty()) {
            runCatching { fetchFromIndonesianEndpoint("https://api-harilibur.vercel.app/api?year=$year", year) }.getOrNull()
        } else {
            null
        }
        val baseHolidays = when {
            !fromDayOff.isNullOrEmpty() -> fromDayOff
            !fromHariLibur.isNullOrEmpty() -> fromHariLibur
            else -> embeddedHolidaysForYear(year)
        }
        val resolved = mergeWithObservances(baseHolidays, year)
        yearCache[year] = resolved
        resolved
    }

    fun getCachedOrEmbeddedHolidaysForYear(year: Int): List<HolidayInfo> {
        return yearCache[year] ?: mergeWithObservances(embeddedHolidaysForYear(year), year)
    }

    fun resolveHolidaysForDate(dateIso: String, holidays: List<HolidayInfo>): List<HolidayInfo> {
        val exact = holidays.filter { it.dateIso == dateIso }
        if (exact.isNotEmpty()) return exact
        val year = dateIso.take(4).toIntOrNull() ?: return emptyList()
        return getCachedOrEmbeddedHolidaysForYear(year).filter { it.dateIso == dateIso }
    }

    fun resolveHolidayForDate(dateIso: String, holidays: List<HolidayInfo>): HolidayInfo? {
        val matches = resolveHolidaysForDate(dateIso, holidays)
        if (matches.isEmpty()) return null
        return matches.firstOrNull { it.isNationalHoliday && !it.isCutiBersama }
            ?: matches.firstOrNull { !it.isNationalObservance }
            ?: matches.first()
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
            lower.contains("kemerdekaan") || lower.contains("proklamasi") || lower.contains("pancasila") ||
                lower.contains("pahlawan") || lower.contains("sumpah pemuda") || lower.contains("kebangkitan") ->
                HolidayThemeType.INDEPENDENCE_DAY
            lower.contains("idul fitri") || lower.contains("idul adha") || lower.contains("maulid") ||
                lower.contains("isra") || lower.contains("tahun baru islam") || lower.contains("hijriah") ||
                lower.contains("santri") ->
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
            HolidayThemeType.NONE -> "Hari ini memperingati $name! Atur waktumu dengan bermakna."
        }
    }

    internal fun mergeWithObservances(baseHolidays: List<HolidayInfo>, year: Int): List<HolidayInfo> {
        val normalizedBase = baseHolidays.map { item ->
            val cat = when {
                item.categoryLabel.isNotBlank() -> item.categoryLabel
                item.isCutiBersama -> "Cuti Bersama"
                item.isNationalObservance -> "Hari Nasional"
                else -> "Libur Nasional"
            }
            item.copy(categoryLabel = cat)
        }
        val existingSignatures = normalizedBase
            .map { "${it.dateIso}|${it.name.lowercase().take(12)}" }
            .toSet()
        val observances = embeddedObservancesForYear(year).filter { obs ->
            val sameDayAndSimilar = normalizedBase.any { base ->
                base.dateIso == obs.dateIso &&
                    (base.name.equals(obs.name, ignoreCase = true) ||
                        (obs.dateIso.endsWith("-06-01") && base.name.contains("pancasila", ignoreCase = true)) ||
                        (obs.dateIso.endsWith("-08-17") && base.name.contains("kemerdekaan", ignoreCase = true)))
            }
            val sig = "${obs.dateIso}|${obs.name.lowercase().take(12)}"
            !sameDayAndSimilar && sig !in existingSignatures
        }
        return (normalizedBase + observances).sortedWith(
            compareBy<HolidayInfo>({ it.dateIso }, { it.isNationalObservance }, { it.isCutiBersama }),
        )
    }

    private fun fetchFromIndonesianEndpoint(endpoint: String, year: Int): List<HolidayInfo> {
        val url = URL(endpoint)
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
            val rawDate = item.optString("tanggal")
                .ifBlank { item.optString("holiday_date") }
                .ifBlank { item.optString("event_date") }
                .ifBlank { item.optString("date") }
            val name = item.optString("keterangan")
                .ifBlank { item.optString("holiday_name") }
                .ifBlank { item.optString("event_name") }
                .ifBlank { item.optString("name") }
                .trim()
            if (rawDate.isBlank() || name.isBlank()) continue

            val normalizedDate = normalizeIsoDate(rawDate, fallbackYear) ?: continue
            val isCuti = if (item.has("is_cuti")) {
                item.optBoolean("is_cuti", false)
            } else {
                name.lowercase().contains("cuti bersama")
            }
            val isNational = if (item.has("is_national_holiday")) {
                item.optBoolean("is_national_holiday", true)
            } else {
                !isCuti
            }
            val isObs = !isNational && !isCuti
            val theme = if (isCuti) HolidayThemeType.NONE else mapHolidayNameToTheme(name)
            val category = when {
                isCuti -> "Cuti Bersama"
                isObs -> "Hari Nasional"
                else -> "Libur Nasional"
            }
            list.add(
                HolidayInfo(
                    dateIso = normalizedDate,
                    name = name,
                    isNationalHoliday = isNational,
                    isCutiBersama = isCuti,
                    isNationalObservance = isObs,
                    categoryLabel = category,
                    themeType = theme,
                    bannerGreeting = buildGreeting(name, theme),
                ),
            )
        }
        return list.sortedBy { it.dateIso }
    }

    internal fun normalizeIsoDate(raw: String, fallbackYear: Int = 2026): String? {
        val match = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""").find(raw) ?: return null
        val y = match.groupValues[1].toIntOrNull() ?: fallbackYear
        val m = match.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: return null
        val d = match.groupValues[3].toIntOrNull()?.coerceIn(1, 31) ?: return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
        }
        val clampedDay = d.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return "%04d-%02d-%02d".format(y, m, clampedDay)
    }

    private fun embeddedHolidaysForYear(year: Int): List<HolidayInfo> {
        val exact = embeddedHolidays().filter { it.dateIso.startsWith("$year-") }
        if (exact.isNotEmpty()) return exact
        return listOf(
            holiday("$year-01-01", "Tahun Baru Masehi"),
            holiday("$year-05-01", "Hari Buruh Internasional"),
            holiday("$year-06-01", "Hari Lahir Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
            holiday("$year-08-17", "Hari Kemerdekaan RI", HolidayThemeType.INDEPENDENCE_DAY),
            holiday("$year-12-25", "Hari Raya Natal", HolidayThemeType.NATAL),
        )
    }

    fun embeddedObservancesForYear(year: Int): List<HolidayInfo> = listOf(
        observance("$year-01-25", "Hari Gizi Nasional"),
        observance("$year-02-09", "Hari Pers Nasional"),
        observance("$year-04-21", "Hari Kartini"),
        observance("$year-05-02", "Hari Pendidikan Nasional"),
        observance("$year-05-20", "Hari Kebangkitan Nasional", HolidayThemeType.INDEPENDENCE_DAY),
        observance("$year-07-01", "Hari Bhayangkara"),
        observance("$year-07-23", "Hari Anak Nasional"),
        observance("$year-08-14", "Hari Pramuka"),
        observance("$year-09-09", "Hari Olahraga Nasional"),
        observance("$year-10-01", "Hari Kesaktian Pancasila", HolidayThemeType.INDEPENDENCE_DAY),
        observance("$year-10-02", "Hari Batik Nasional"),
        observance("$year-10-05", "Hari Tentara Nasional Indonesia"),
        observance("$year-10-22", "Hari Santri Nasional", HolidayThemeType.IDUL_FITRI),
        observance("$year-10-28", "Hari Sumpah Pemuda", HolidayThemeType.INDEPENDENCE_DAY),
        observance("$year-11-10", "Hari Pahlawan", HolidayThemeType.INDEPENDENCE_DAY),
        observance("$year-11-25", "Hari Guru Nasional"),
        observance("$year-12-22", "Hari Ibu Nasional"),
    )

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
            isNationalObservance = false,
            categoryLabel = "Libur Nasional",
            themeType = resolvedTheme,
            bannerGreeting = buildGreeting(name, resolvedTheme),
        )
    }

    private fun observance(
        dateIso: String,
        name: String,
        theme: HolidayThemeType = HolidayThemeType.NONE,
    ): HolidayInfo {
        return HolidayInfo(
            dateIso = dateIso,
            name = name,
            isNationalHoliday = false,
            isCutiBersama = false,
            isNationalObservance = true,
            categoryLabel = "Hari Nasional",
            themeType = theme,
            bannerGreeting = buildGreeting(name, theme),
        )
    }
}
