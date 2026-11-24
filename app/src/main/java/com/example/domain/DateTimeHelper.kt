package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeHelper {
    private val indonesiaLocale = Locale.forLanguageTag("id-ID")
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    private val fullDayNames = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    private val shortMonths = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
    )
    private val fullMonths = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
    )

    fun todayIso(): String = calendarToIso(Calendar.getInstance())

    fun nowMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun calendarToIso(cal: Calendar): String = synchronized(isoFormat) {
        isoFormat.format(cal.time)
    }

    fun parseIsoToCalendar(dateIso: String): Calendar {
        val cal = Calendar.getInstance()
        val parts = dateIso.split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val day = parts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal
            }
        }
        return cal
    }

    fun isoToUtcDatePickerMillis(dateIso: String): Long {
        val parts = dateIso.split("-")
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val day = parts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                utc.clear()
                utc.set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31), 0, 0, 0)
                return utc.timeInMillis
            }
        }
        return utc.timeInMillis
    }

    fun utcDatePickerMillisToIso(millis: Long): String {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val year = utc.get(Calendar.YEAR)
        val month = utc.get(Calendar.MONTH) + 1
        val day = utc.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun addDays(dateIso: String, days: Int): String {
        val cal = parseIsoToCalendar(dateIso)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return calendarToIso(cal)
    }

    fun addMonths(dateIso: String, months: Int): String {
        val cal = parseIsoToCalendar(dateIso)
        cal.add(Calendar.MONTH, months)
        return calendarToIso(cal)
    }

    fun addYears(dateIso: String, years: Int): String {
        val cal = parseIsoToCalendar(dateIso)
        val origMonth = cal.get(Calendar.MONTH)
        val origDay = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.YEAR, years)
        // Guard Feb 29 -> Feb 28 in non-leap year cleanly
        if (origMonth == Calendar.FEBRUARY && origDay == 29 && cal.get(Calendar.MONTH) != Calendar.FEBRUARY) {
            cal.set(Calendar.MONTH, Calendar.FEBRUARY)
            cal.set(Calendar.DAY_OF_MONTH, 28)
        }
        return calendarToIso(cal)
    }

    fun monthDayKey(dateIso: String): String {
        val parts = dateIso.split("-")
        return if (parts.size == 3) "${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}" else ""
    }

    fun yearOf(dateIso: String): Int? = dateIso.split("-").firstOrNull()?.toIntOrNull()

    fun resolveBirthdayOccurrenceInYear(birthDateIso: String, targetYear: Int): String? {
        val parts = birthDateIso.split("-")
        if (parts.size != 3) return null
        val m = parts[1].toIntOrNull()?.coerceIn(1, 12) ?: return null
        val d = parts[2].toIntOrNull()?.coerceIn(1, 31) ?: return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, d.coerceAtMost(maxD))
        }
        return calendarToIso(cal)
    }

    fun isSameMonthAndDay(dateIsoA: String, dateIsoB: String): Boolean {
        val keyA = monthDayKey(dateIsoA)
        val keyB = monthDayKey(dateIsoB)
        return keyA.isNotEmpty() && keyA == keyB
    }

    fun daysUsingSince(firstUsedAtEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Int {
        if (firstUsedAtEpochMs <= 0L || nowEpochMs <= firstUsedAtEpochMs) return 1
        val diffDays = ((nowEpochMs - firstUsedAtEpochMs) / 86_400_000L).toInt()
        return (diffDays + 1).coerceAtLeast(1)
    }

    fun formatUsageDuration(daysCount: Int): String {
        val safe = daysCount.coerceAtLeast(1)
        return when {
            safe >= 365 -> {
                val years = safe / 365
                val remMonths = (safe % 365) / 30
                if (remMonths > 0) "$years tahun $remMonths bulan" else "$years tahun"
            }
            safe >= 30 -> {
                val months = safe / 30
                val remDays = safe % 30
                if (remDays > 0) "$months bulan $remDays hari" else "$months bulan"
            }
            else -> "$safe hari"
        }
    }

    fun weekDaysFor(anchorIso: String): List<String> {
        val cal = parseIsoToCalendar(anchorIso)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffFromMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, diffFromMonday)
        return (0..6).map {
            val iso = calendarToIso(cal)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            iso
        }
    }

    fun monthRangeFor(anchorIso: String): Pair<String, String> {
        val cal = parseIsoToCalendar(anchorIso)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = calendarToIso(cal)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        val end = calendarToIso(cal)
        return start to end
    }

    data class MonthCalendarCell(
        val dateIso: String?,
        val dayOfMonth: Int?,
    )

    fun monthGridCells(anchorIso: String): List<MonthCalendarCell> {
        val cal = parseIsoToCalendar(anchorIso)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingBlanks = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<MonthCalendarCell>()
        repeat(leadingBlanks) { cells.add(MonthCalendarCell(null, null)) }
        for (d in 1..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            cells.add(MonthCalendarCell(calendarToIso(cal), d))
        }
        while (cells.size % 7 != 0) {
            cells.add(MonthCalendarCell(null, null))
        }
        return cells
    }

    fun formatIndonesianFullDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val dayName = fullDayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = fullMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$dayName, $day $month $year"
    }

    fun formatShortDate(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = shortMonths[cal.get(Calendar.MONTH)]
        return "$day $month"
    }

    fun formatDayShortLabel(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        return dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun dayOfMonthNumber(dateIso: String): Int {
        return parseIsoToCalendar(dateIso).get(Calendar.DAY_OF_MONTH)
    }

    fun formatMonthYear(dateIso: String): String {
        val cal = parseIsoToCalendar(dateIso)
        val month = fullMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$month $year"
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 23 * 60 + 59)
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun formatMinuteDot(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, 23 * 60 + 59)
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d.%02d", h, m)
    }

    fun formatHourSlot(hour: Int): String {
        val h = hour.coerceIn(0, 23)
        return String.format(Locale.US, "%02d.00", h)
    }

    fun formatTimeRange(startMin: Int, endMin: Int): String =
        "${formatMinuteOfDay(startMin)} - ${formatMinuteOfDay(endMin)}"

    fun formatDuration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(1)
        val h = safe / 60
        val m = safe % 60
        return when {
            h > 0 && m > 0 -> "${h}j ${m}m"
            h > 0 -> "${h} jam"
            else -> "$m mnt"
        }
    }

    fun formatTimelineDuration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(1)
        val h = safe / 60
        val m = safe % 60
        return when {
            h > 0 && m > 0 -> "$h jam $m mnt"
            h > 0 -> "$h jam"
            else -> "$m mnt"
        }
    }

    fun formatCountdown(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    fun formatEpochTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", indonesiaLocale)
        return sdf.format(Date(epochMs))
    }

    fun epochMillisFor(dateIso: String, minuteOfDay: Int): Long {
        val cal = parseIsoToCalendar(dateIso)
        cal.set(Calendar.HOUR_OF_DAY, (minuteOfDay / 60).coerceIn(0, 23))
        cal.set(Calendar.MINUTE, (minuteOfDay % 60).coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
