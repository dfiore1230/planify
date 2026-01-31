package com.planify.app.data.repository

import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.SaleStatus
import com.planify.app.data.model.TicketSale
import com.planify.app.data.model.TicketSaleEventInfo
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TicketRepository(private val httpClient: HttpClient) {
    data class TicketPagination(
        val currentPage: Int,
        val lastPage: Int,
        val perPage: Int,
        val total: Int
    )

    suspend fun search(eventId: Int?, query: String?, instance: InstanceProfile): List<TicketSale> {
        return searchPage(eventId, query, null, null, instance).first
    }

    suspend fun searchPage(
        eventId: Int?,
        query: String?,
        page: Int?,
        perPage: Int?,
        instance: InstanceProfile
    ): Pair<List<TicketSale>, TicketPagination?> {
        val queryItems = mutableMapOf<String, String?>()
        if (eventId != null) queryItems["event_id"] = eventId.toString()
        if (!query.isNullOrBlank()) queryItems["query"] = query
        if (page != null) queryItems["page"] = page.toString()
        if (perPage != null) queryItems["per_page"] = perPage.toString()
        queryItems["_t"] = (System.currentTimeMillis() / 1000).toString()

        val response = httpClient.request<JsonObject>(
            path = "/api/tickets",
            method = HttpMethod.GET,
            query = queryItems,
            body = null,
            instance = instance,
            additionalHeaders = null
        )

        val data = response["data"] as? JsonArray ?: JsonArray(emptyList())
        val sales = data.mapNotNull { (it as? JsonObject)?.let(TicketSaleParser::parse) }

        val meta = (response["meta"] as? JsonObject)?.let {
            TicketPagination(
                currentPage = it["current_page"]?.jsonPrimitive?.intOrNull ?: 1,
                lastPage = it["last_page"]?.jsonPrimitive?.intOrNull ?: 1,
                perPage = it["per_page"]?.jsonPrimitive?.intOrNull ?: perPage ?: 0,
                total = it["total"]?.jsonPrimitive?.intOrNull ?: sales.size
            )
        }

        return sales to meta
    }

    suspend fun fetch(id: Int, instance: InstanceProfile): TicketSale {
        val response = httpClient.request<JsonObject>(
            path = "/api/tickets/$id",
            method = HttpMethod.GET,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        return TicketSaleParser.parse(response)
    }

    suspend fun updateStatus(id: Int, action: String, instance: InstanceProfile): TicketSale {
        val response = httpClient.request<JsonObject>(
            path = "/api/tickets/$id",
            method = HttpMethod.PATCH,
            query = null,
            body = buildJsonObject { put("action", action) }.toString(),
            instance = instance,
            additionalHeaders = null
        )

        val status = when (response["data"]?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull) {
            "paid" -> SaleStatus.paid
            "unpaid" -> SaleStatus.unpaid
            "cancelled" -> SaleStatus.cancelled
            "refunded" -> SaleStatus.refunded
            "expired" -> SaleStatus.expired
            "deleted" -> SaleStatus.deleted
            else -> SaleStatus.pending
        }

        return TicketSale(
            id = id,
            status = status,
            name = "",
            email = "",
            eventId = 0,
            event = null,
            tickets = emptyList()
        )
    }

    suspend fun reassign(id: Int, newHolder: String, newEmail: String, instance: InstanceProfile): TicketSale {
        val body = buildJsonObject {
            put("new_holder_name", newHolder)
            put("new_holder_email", newEmail)
        }
        val response = httpClient.request<JsonObject>(
            path = "/api/tickets/$id/reassign",
            method = HttpMethod.POST,
            query = null,
            body = body.toString(),
            instance = instance,
            additionalHeaders = null
        )
        return when {
            response["data"] is JsonObject -> TicketSaleParser.parse(response["data"] as JsonObject)
            else -> TicketSaleParser.parse(response)
        }
    }

    suspend fun markAsPaid(id: Int, instance: InstanceProfile) = updateStatus(id, "mark_paid", instance)
    suspend fun markAsUnpaid(id: Int, instance: InstanceProfile) = updateStatus(id, "mark_unpaid", instance)
    suspend fun refund(id: Int, instance: InstanceProfile) = updateStatus(id, "refund", instance)
    suspend fun cancel(id: Int, instance: InstanceProfile) = updateStatus(id, "cancel", instance)
    suspend fun markAsUsed(id: Int, instance: InstanceProfile) = updateStatus(id, "mark_used", instance)
    suspend fun markAsUnused(id: Int, instance: InstanceProfile) = updateStatus(id, "mark_unused", instance)
    suspend fun delete(id: Int, instance: InstanceProfile) = updateStatus(id, "delete", instance)

    suspend fun addNote(id: Int, note: String, instance: InstanceProfile) {
        val body = buildJsonObject { put("note", note) }
        httpClient.requestVoid(
            path = "/api/tickets/$id/notes",
            method = HttpMethod.POST,
            query = null,
            body = body.toString(),
            instance = instance,
            additionalHeaders = null
        )
    }
}
