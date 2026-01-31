package com.planify.app.data.repository

import com.planify.app.data.model.CheckIn
import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.ScanResult
import com.planify.app.data.network.ApiError
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import com.planify.app.util.DebugLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CheckInRepository(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun performCheckIn(checkIn: CheckIn, instance: InstanceProfile): ScanResult {
        val body = buildJsonObject {
            put("ticket_id", checkIn.ticketId)
            checkIn.ticketCode?.let { put("ticket_code", it) }
            put("event_id", checkIn.eventId)
            checkIn.attendeeName?.let { put("attendee_name", it) }
            checkIn.gateId?.let { put("gate_id", it) }
            checkIn.deviceId?.let { put("device_id", it) }
            put("action", checkIn.action.name)
            put("ts", checkIn.timestamp.toString())
            put("idempotency_key", checkIn.idempotencyKey())
        }

        val response = httpClient.request<JsonObject>(
            path = "/api/checkins",
            method = HttpMethod.POST,
            query = null,
            body = body.toString(),
            instance = instance,
            additionalHeaders = null
        )
        return CheckInParser.parseScanResult(response)
    }

    suspend fun fetchCheckIns(eventId: String, instance: InstanceProfile): List<CheckIn> {
        val response = httpClient.request<JsonObject>(
            path = "/api/checkins",
            method = HttpMethod.GET,
            query = mapOf("event_id" to eventId),
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        val data = response["data"] as? JsonArray
        return data?.mapNotNull { (it as? JsonObject)?.let(CheckInParser::parseCheckIn) } ?: emptyList()
    }

    suspend fun scanTicket(
        code: String,
        eventId: String,
        gateId: String?,
        deviceId: String?,
        instance: InstanceProfile
    ): ScanResult {
        val ticketCode = parseTicketCode(code)
        val body = buildJsonObject { put("ticket_code", ticketCode) }
        return try {
            val (rawData, rawResponse) = httpClient.requestRaw(
                path = "/api/tickets/scan",
                method = HttpMethod.POST,
                query = null,
                body = body.toString(),
                instance = instance,
                additionalHeaders = null
            )

            if (rawResponse.isSuccessful) {
                val element = json.parseToJsonElement(rawData.decodeToString())
                val obj = element as? JsonObject
                if (obj != null) {
                    val data = obj["data"] as? JsonObject
                    if (data != null) {
                        return CheckInParser.parseScanResult(buildScanResultFromScanData(data, gateId))
                    }
                }
                return ScanResult(status = com.planify.app.data.model.ScanStatus.admitted, message = "Ticket scanned (server confirmed).")
            }
            throw ApiError.ServerError(rawResponse.code, rawData.decodeToString())
        } catch (e: ApiError) {
            return when (e) {
                is ApiError.ServerError -> {
                    val text = e.body ?: "Ticket scan failed"
                    when (e.statusCode) {
                        404 -> ScanResult(status = com.planify.app.data.model.ScanStatus.invalid, message = "Ticket not found")
                        403 -> ScanResult(status = com.planify.app.data.model.ScanStatus.invalid, message = "Not authorized to scan this ticket")
                        400 -> {
                            val lowered = text.lowercase()
                            when {
                                lowered.contains("not valid for today") -> ScanResult(status = com.planify.app.data.model.ScanStatus.wrong_date, message = "Not valid for today")
                                lowered.contains("not paid") -> ScanResult(status = com.planify.app.data.model.ScanStatus.unpaid, message = "Ticket not paid")
                                lowered.contains("cancelled") -> ScanResult(status = com.planify.app.data.model.ScanStatus.cancelled, message = "Ticket cancelled")
                                lowered.contains("refunded") -> ScanResult(status = com.planify.app.data.model.ScanStatus.refunded, message = "Ticket refunded")
                                else -> ScanResult(status = com.planify.app.data.model.ScanStatus.invalid, message = text)
                            }
                        }
                        else -> ScanResult(status = com.planify.app.data.model.ScanStatus.unknown, message = text)
                    }
                }
                ApiError.Unauthorized -> ScanResult(status = com.planify.app.data.model.ScanStatus.invalid, message = "Unauthorized")
                ApiError.Forbidden -> ScanResult(status = com.planify.app.data.model.ScanStatus.invalid, message = "Forbidden")
                else -> ScanResult(status = com.planify.app.data.model.ScanStatus.unknown, message = e.message)
            }
        }
    }

    private fun buildScanResultFromScanData(scanData: JsonObject, gateId: String?): JsonObject {
        val sale = scanData["sale"] as? JsonObject
        val holder = sale?.get("name")?.jsonPrimitive?.contentOrNull
        val event = sale?.get("event") as? JsonObject
        val eventName = event?.get("name")?.jsonPrimitive?.contentOrNull
        val eventId = sale?.get("event_id")?.jsonPrimitive?.intOrNull?.toString()
        val ticketId = scanData["sale_id"]?.jsonPrimitive?.intOrNull?.toString()
        val checkedAt = scanData["scanned_at"]?.jsonPrimitive?.contentOrNull

        return buildJsonObject {
            put("status", "admitted")
            ticketId?.let { put("ticket_id", it) }
            holder?.let { put("holder", it) }
            eventId?.let { put("event_id", it) }
            eventName?.let { put("event_name", it) }
            checkedAt?.let { put("checked_in_at", it) }
            gateId?.let { put("gate_id", it) }
        }
    }

    private fun parseTicketCode(code: String): String {
        val trimmed = code.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                val uri = java.net.URI(trimmed)
                val parts = uri.path.split("/").filter { it.isNotBlank() }
                if (parts.size >= 4 && parts[0] == "ticket" && parts[1] == "view") {
                    java.net.URLDecoder.decode(parts[3], "UTF-8")
                } else {
                    trimmed
                }
            } catch (e: Exception) {
                trimmed
            }
        } else {
            trimmed
        }
    }
}
