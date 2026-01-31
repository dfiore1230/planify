package com.planify.app.data.repository

import com.planify.app.data.model.*
import com.planify.app.util.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object CheckInParser {
    fun parseCheckIn(obj: JsonObject): CheckIn {
        val id = obj.string("id") ?: java.util.UUID.randomUUID().toString()
        val ticketId = obj.stringAny(listOf("ticket_id", "ticketId")) ?: ""
        val ticketCode = obj.stringAny(listOf("ticket_code", "code", "ticketCode"))
        val eventId = obj.stringAny(listOf("event_id", "eventId")) ?: ""
        val attendeeName = obj.stringAny(listOf("attendee_name", "attendeeName"))
        val gateId = obj.stringAny(listOf("gate_id", "gateId"))
        val deviceId = obj.stringAny(listOf("device_id", "deviceId"))
        val action = when (obj.string("action")?.lowercase()) {
            "checkout" -> CheckInAction.checkout
            else -> CheckInAction.checkin
        }
        val timestamp = obj.instantAny(listOf("timestamp", "ts"))
            ?: Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)
        val performedBy = obj.stringAny(listOf("performed_by", "performedBy"))
        val notes = obj.string("notes")
        val isOffline = obj.boolean("is_offline") ?: obj.boolean("isOffline") ?: false
        val syncedAt = obj.instantAny(listOf("synced_at", "syncedAt"))

        return CheckIn(
            id = id,
            ticketId = ticketId,
            ticketCode = ticketCode,
            eventId = eventId,
            attendeeName = attendeeName,
            gateId = gateId,
            deviceId = deviceId,
            action = action,
            timestamp = timestamp,
            performedBy = performedBy,
            notes = notes,
            isOffline = isOffline,
            syncedAt = syncedAt
        )
    }

    fun parseScanResult(obj: JsonObject): ScanResult {
        val status = when (obj.string("status")?.lowercase()) {
            "admitted" -> ScanStatus.admitted
            "already_used" -> ScanStatus.already_used
            "refunded" -> ScanStatus.refunded
            "voided" -> ScanStatus.voided
            "wrong_event" -> ScanStatus.wrong_event
            "wrong_date" -> ScanStatus.wrong_date
            "unpaid" -> ScanStatus.unpaid
            "cancelled" -> ScanStatus.cancelled
            "expired" -> ScanStatus.expired
            "invalid" -> ScanStatus.invalid
            else -> ScanStatus.unknown
        }
        val ticketId = obj.stringAny(listOf("ticket_id", "ticketId"))
        val holder = obj.stringAny(listOf("holder", "holder_name"))
        val eventId = obj.stringAny(listOf("event_id", "eventId"))
        val eventName = obj.stringAny(listOf("event_name", "eventName"))
        val checkedInAt = obj.instantAny(listOf("checked_in_at", "checkedInAt"))
        val gateId = obj.stringAny(listOf("gate_id", "gateId"))
        val serverTime = obj.instantAny(listOf("server_time", "serverTime"))
            ?: Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)
        val message = obj.string("message")
        val saleTicketId = obj.intAny(listOf("sale_ticket_id", "saleTicketId"))

        return ScanResult(
            status = status,
            ticketId = ticketId,
            holder = holder,
            eventId = eventId,
            eventName = eventName,
            checkedInAt = checkedInAt,
            gateId = gateId,
            serverTime = serverTime,
            message = message,
            saleTicketId = saleTicketId
        )
    }
}
