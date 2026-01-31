package com.planify.app.data.model

import kotlinx.datetime.Instant
import java.util.UUID


enum class CheckInAction(val displayName: String) {
    checkin("Check In"),
    checkout("Check Out")
}

data class CheckIn(
    val id: String = UUID.randomUUID().toString(),
    val ticketId: String,
    val ticketCode: String? = null,
    val eventId: String,
    val attendeeName: String? = null,
    val gateId: String? = null,
    val deviceId: String? = null,
    val action: CheckInAction,
    val timestamp: Instant = Instant.fromEpochSeconds(System.currentTimeMillis() / 1000),
    val performedBy: String? = null,
    val notes: String? = null,
    val isOffline: Boolean = false,
    val syncedAt: Instant? = null
) {
    fun idempotencyKey(): String {
        val devicePart = deviceId ?: "unknown"
        val epochSecond = timestamp.epochSeconds
        return "$devicePart:$ticketId:$epochSecond"
    }
}

data class ScanResult(
    val status: ScanStatus,
    val ticketId: String? = null,
    val holder: String? = null,
    val eventId: String? = null,
    val eventName: String? = null,
    val checkedInAt: Instant? = null,
    val gateId: String? = null,
    val serverTime: Instant = Instant.fromEpochSeconds(System.currentTimeMillis() / 1000),
    val message: String? = null,
    val saleTicketId: Int? = null
) {
    val isSuccess: Boolean
        get() = status == ScanStatus.admitted
}

enum class ScanStatus(val displayName: String) {
    admitted("Admitted"),
    already_used("Already Used"),
    refunded("Refunded"),
    voided("Voided"),
    wrong_event("Wrong Event"),
    wrong_date("Wrong Date"),
    unpaid("Unpaid"),
    cancelled("Cancelled"),
    unknown("Unknown"),
    expired("Expired"),
    invalid("Invalid")
}
