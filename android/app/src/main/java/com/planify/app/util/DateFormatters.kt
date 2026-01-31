package com.planify.app.util

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatters {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

    fun formatDisplay(instant: Instant, timeZoneId: String?): String {
        val zone = timeZoneId?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        return displayFormatter.withZone(zone).format(instant.toJavaInstant())
    }
}
