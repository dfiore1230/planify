package com.planify.app.data.repository

import com.planify.app.data.model.*
import com.planify.app.util.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object TicketParser {
    fun parseTicket(obj: JsonObject): Ticket {
        val id = obj.string("id") ?: ""
        val code = obj.string("code") ?: ""
        val eventId = obj.stringAny(listOf("event_id", "eventId")) ?: ""
        val eventName = obj.stringAny(listOf("event_name", "eventName"))
        val ticketTypeId = obj.stringAny(listOf("ticket_type_id", "ticketTypeId")) ?: ""

        val ticketTypeName = obj.stringAny(listOf("ticket_type_name", "type_name"))
            ?: obj.objectOrNull("ticket_type")?.string("name")
            ?: "General Admission"

        val holderName = obj.stringAny(listOf("holder_name", "holderName"))
        val holderEmail = obj.stringAny(listOf("holder_email", "holderEmail"))

        val status = when (obj.string("status")?.lowercase()) {
            "used" -> TicketStatus.used
            "refunded" -> TicketStatus.refunded
            "voided" -> TicketStatus.voided
            "expired" -> TicketStatus.expired
            "transferred" -> TicketStatus.transferred
            else -> TicketStatus.valid
        }

        val price = obj.double("price")
        val currency = obj.string("currency")
        val seat = obj.string("seat")
        val zone = obj.string("zone")

        val purchasedAt = obj.instantAny(listOf("purchased_at", "purchasedAt"))
        val checkedInAt = obj.instantAny(listOf("checked_in_at", "checkedInAt"))
        val checkedOutAt = obj.instantAny(listOf("checked_out_at", "checkedOutAt"))
        val notes = obj.string("notes")

        val history = parseHistory(obj["history"] as? JsonArray)

        val qrCodeUrl = obj.stringAny(listOf("qr_code_url", "qr_code", "qrCodeUrl"))

        return Ticket(
            id = id,
            code = code,
            eventId = eventId,
            eventName = eventName,
            ticketTypeId = ticketTypeId,
            ticketTypeName = ticketTypeName,
            holderName = holderName,
            holderEmail = holderEmail,
            status = status,
            price = price,
            currency = currency,
            seat = seat,
            zone = zone,
            purchasedAt = purchasedAt,
            checkedInAt = checkedInAt,
            checkedOutAt = checkedOutAt,
            notes = notes,
            history = history,
            qrCodeUrl = qrCodeUrl
        )
    }

    private fun parseHistory(array: JsonArray?): List<TicketHistoryEntry> {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = obj.string("id") ?: java.util.UUID.randomUUID().toString()
            val action = obj.string("action") ?: ""
            val performedBy = obj.stringAny(listOf("performed_by", "performedBy"))
            val performedAt = obj.instantAny(listOf("performed_at", "performedAt"))
                ?: Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)
            val notes = obj.string("notes")
            TicketHistoryEntry(id, action, performedBy, performedAt, notes)
        } ?: emptyList()
    }
}
