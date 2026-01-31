package com.planify.app.data.repository

import com.planify.app.data.model.*
import com.planify.app.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object TicketSaleParser {
    fun parse(obj: JsonObject): TicketSale {
        val id = obj.int("id") ?: 0
        val status = when (obj.string("status")?.lowercase()) {
            "paid" -> SaleStatus.paid
            "unpaid" -> SaleStatus.unpaid
            "cancelled" -> SaleStatus.cancelled
            "refunded" -> SaleStatus.refunded
            "expired" -> SaleStatus.expired
            "deleted" -> SaleStatus.deleted
            else -> SaleStatus.pending
        }
        val name = obj.string("name") ?: ""
        val email = obj.string("email") ?: ""
        val eventId = obj.intAny(listOf("event_id", "eventId")) ?: 0
        val event = obj.objectOrNull("event")?.let { TicketSaleEventInfo(
            id = it.string("id") ?: "",
            name = it.string("name") ?: ""
        ) }
        val tickets = parseTickets(obj["tickets"] as? JsonArray)
        return TicketSale(id, status, name, email, eventId, event, tickets)
    }

    private fun parseTickets(array: JsonArray?): List<SaleTicket> {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.int("id") ?: 0
            val ticketId = obj.intAny(listOf("ticket_id", "ticketId")) ?: 0
            val quantity = obj.int("quantity") ?: 0
            val usageStatus = obj.stringAny(listOf("usage_status", "usageStatus")) ?: "unused"
            SaleTicket(id, ticketId, quantity, usageStatus)
        } ?: emptyList()
    }
}
