package com.planify.app.data.model


enum class SaleStatus(val displayName: String) {
    pending("Pending"),
    paid("Paid"),
    unpaid("Unpaid"),
    cancelled("Cancelled"),
    refunded("Refunded"),
    expired("Expired"),
    deleted("Deleted")
}

data class SaleTicket(
    val id: Int,
    val ticketId: Int,
    val quantity: Int,
    val usageStatus: String
)

data class TicketSaleEventInfo(
    val id: String,
    val name: String
)

data class TicketSale(
    val id: Int,
    val status: SaleStatus,
    val name: String,
    val email: String,
    val eventId: Int,
    val event: TicketSaleEventInfo? = null,
    val tickets: List<SaleTicket> = emptyList()
) {
    val displayStatus: String
        get() = status.displayName

    val totalQuantity: Int
        get() = tickets.sumOf { it.quantity }
}
