package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val ISO_FORMAT = "yyyy-MM-dd"
    private const val DISPLAY_FORMAT = "dd MMM"

    private fun isoFormatter() = SimpleDateFormat(ISO_FORMAT, Locale.US)
    private fun displayFormatter() = SimpleDateFormat(DISPLAY_FORMAT, Locale("es", "MX"))

    fun todayLogicalDate(nowMillis: Long = System.currentTimeMillis()): String {
        return isoFormatter().format(Date(nowMillis))
    }

    fun previousLogicalDate(logicalDate: String): String {
        return shiftLogicalDate(logicalDate, -1)
    }

    fun shiftLogicalDate(logicalDate: String, deltaDays: Int): String {
        val date = runCatching { isoFormatter().parse(logicalDate) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        return isoFormatter().format(cal.time)
    }

    fun formatShortDate(logicalDate: String): String {
        val date = runCatching { isoFormatter().parse(logicalDate) }.getOrNull() ?: return logicalDate
        return displayFormatter().format(date)
    }

    fun isValidIsoDate(value: String): Boolean {
        return runCatching {
            val sdf = isoFormatter().apply { isLenient = false }
            sdf.parse(value) != null
        }.getOrDefault(false)
    }

    fun normalizeExcelDate(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (isValidIsoDate(trimmed)) return trimmed
        val candidates = listOf("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy/MM/dd")
        for (pattern in candidates) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(trimmed)
            }.getOrNull()
            if (parsed != null) return isoFormatter().format(parsed)
        }
        return null
    }
}
