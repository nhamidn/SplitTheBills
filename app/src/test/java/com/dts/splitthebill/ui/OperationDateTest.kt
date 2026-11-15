package com.dts.splitthebill.ui

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OperationDateTest {
    @Test
    fun validCalendarDatesRoundTripIncludingLeapDays() {
        for (date in listOf("2024-02-29", "2026-03-29", "2026-12-31")) {
            assertEquals(date, OperationDate.fromPickerMillis(requireNotNull(OperationDate.toPickerMillis(date))))
        }
    }

    @Test
    fun invalidOrTypedDateFormatsAreRejected() {
        for (date in listOf("2025-02-29", "2026-04-31", "15/11/2026", "2026-1-01", "2026-01-01garbage")) {
            assertNull(date, OperationDate.toPickerMillis(date))
        }
    }

    @Test
    fun pickerDatesDoNotShiftInPositiveOrNegativeTimeZones() {
        val original = TimeZone.getDefault()
        try {
            for (zone in listOf("America/Los_Angeles", "Pacific/Kiritimati", "Africa/Casablanca")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(1709164800000L, OperationDate.toPickerMillis("2024-02-29"))
                assertEquals("2024-02-29", OperationDate.fromPickerMillis(1709164800000L))
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
