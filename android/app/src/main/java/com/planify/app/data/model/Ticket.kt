package com.planify.app.data.model

import kotlinx.datetime.Instant


enum class TicketStatus(val displayName: String) {
    valid("Valid"),
    used("Used"),
    refunded("Refunded"),
    voided("Voided"),
    expired("Expired"),
    transferred("Transferred")
}

data class TicketHistoryEntry(
    val id: String,
    val action: String,
    val performedBy: String? = null,
    val performedAt: Instant,
    val notes: String? = null
)

data class Ticket(
    val id: String,
    val code: String,
    val eventId: String,
    val eventName: String? = null,
    val ticketTypeId: String,
    val ticketTypeName: String,
    val holderName: String? = null,
    val holderEmail: String? = null,
    val status: TicketStatus = TicketStatus.valid,
    val price: Double? = null,
    val currency: String? = null,
    val seat: String? = null,
    val zone: String? = null,
    val purchasedAt: Instant? = null,
    val checkedInAt: Instant? = null,
    val checkedOutAt: Instant? = null,
    val notes: String? = null,
    val history: List<TicketHistoryEntry> = emptyList(),
    val qrCodeUrl: String? = null
) {
    val displayStatus: String
        get() = status.displayName

    val canCheckIn: Boolean
        get() = status == TicketStatus.valid && checkedInAt == null

    val canCheckOut: Boolean
        get() = status == TicketStatus.used && checkedOutAt == null
}
