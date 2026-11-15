package com.dts.splitthebill.ui

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Material date pickers use UTC midnight; operation dates have no time-zone component. */
internal object OperationDate {
    fun toPickerMillis(value: String): Long? {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return null
        return runCatching { isoFormatter().parse(value)?.time }.getOrNull()
    }

    fun fromPickerMillis(value: Long): String = isoFormatter().format(Date(value))

    fun display(value: String, locale: Locale): String {
        val millis = toPickerMillis(value) ?: return value
        return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    }

    private fun isoFormatter() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
