package com.planify.app.data.repository

import com.planify.app.data.AppSettings
import com.planify.app.data.model.*
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import com.planify.app.util.DebugLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class EventRepository(
    private val httpClient: HttpClient,
    private val appSettings: AppSettings
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val cache: MutableMap<String, List<Event>> = mutableMapOf()
    private val venueCache: MutableMap<String, List<Venue>> = mutableMapOf()
    private val resourcesCache: MutableMap<String, EventResources> = mutableMapOf()
    private val subdomainCache: MutableMap<String, Pair<String, String?>> = mutableMapOf()
    private val mutex = Mutex()

    data class ExtendedEventOptions(
        val categoryName: String? = null,
        val ticketsEnabled: Boolean? = null,
        val ticketCurrencyCode: String? = null,
        val totalTicketsMode: String? = null,
        val ticketNotes: String? = null,
        val paymentMethod: String? = null,
        val paymentInstructions: String? = null,
        val expireUnpaidTickets: Boolean? = null,
        val remindUnpaidTicketsEvery: Int? = null,
        val registrationUrl: String? = null,
        val eventPassword: String? = null,
        val flyerImageId: String? = null,
        val flyerImageData: ByteArray? = null,
        val clearFlyerImage: Boolean? = null,
        val guestListVisibility: String? = null,
        val members: List<MemberDTO> = emptyList(),
        val schedule: String? = null,
        val tickets: List<TicketDTO>? = null,
        val paymentUrl: String? = null
    )

    data class MemberDTO(
        val name: String,
        val email: String? = null,
        val youtubeUrl: String? = null
    )

    data class TicketDTO(
        val type: String? = null,
        val price: Int,
        val quantity: Int,
        val description: String? = null
    )

    suspend fun listEvents(instance: InstanceProfile): List<Event> {
        return try {
            val (bytes, _) = httpClient.requestRaw(
                path = "/api/events",
                method = HttpMethod.GET,
                query = null,
                body = null,
                instance = instance,
                additionalHeaders = null
            )
            val element = json.parseToJsonElement(bytes.decodeToString())
            val events = parseEventList(element)
            val enriched = enrichVenueNames(events, instance)
            mutex.withLock { cache[instance.id] = enriched }
            enriched
        } catch (e: Exception) {
            DebugLogger.error("EventRepository: listEvents failed", e)
            mutex.withLock { cache[instance.id] } ?: throw e
        }
    }

    suspend fun getEvent(id: String, instance: InstanceProfile): Event {
        mutex.withLock {
            cache[instance.id]?.firstOrNull { it.id == id }?.let { return it }
        }
        val events = listEvents(instance)
        return events.firstOrNull { it.id == id } ?: throw IllegalStateException("Event not found")
    }

    suspend fun listEventResources(instance: InstanceProfile): EventResources {
        return try {
            val (bytes, _) = httpClient.requestRaw(
                path = "/api/events/resources",
                method = HttpMethod.GET,
                query = mapOf("per_page" to "1000"),
                body = null,
                instance = instance,
                additionalHeaders = null
            )
            val element = json.parseToJsonElement(bytes.decodeToString()) as JsonObject
            val resources = EventResources.fromJson(element)
            mutex.withLock {
                resourcesCache[instance.id] = resources
                venueCache[instance.id] = resources.venues.map { Venue(it.id, it.name) }
            }
            resources
        } catch (e: Exception) {
            DebugLogger.error("EventRepository: listEventResources failed", e)
            mutex.withLock { resourcesCache[instance.id] } ?: throw e
        }
    }

    suspend fun createEvent(
        event: Event,
        instance: InstanceProfile,
        timeZoneOverride: String? = null,
        options: ExtendedEventOptions? = null
    ): Event {
        val (subdomain, _) = resolveSubdomain(instance)
        val payload = buildEventPayload(event, timeZoneOverride, options, isUpdate = false)
        val response = httpClient.request<JsonObject>(
            path = "/api/events/$subdomain",
            method = HttpMethod.POST,
            query = null,
            body = payload.toString(),
            instance = instance,
            additionalHeaders = null
        )
        val eventJson = extractEventJson(response)
        val parsed = EventParser.parseEvent(eventJson)
        val enriched = enrichVenueName(parsed, instance)
        upsert(enriched, instance)
        return enriched
    }

    suspend fun updateEvent(
        event: Event,
        instance: InstanceProfile,
        timeZoneOverride: String? = null,
        options: ExtendedEventOptions? = null
    ): Event {
        val payload = buildEventPayload(event, timeZoneOverride, options, isUpdate = true)
        val response = httpClient.request<JsonObject>(
            path = "/api/events/${event.id}",
            method = HttpMethod.PATCH,
            query = null,
            body = payload.toString(),
            instance = instance,
            additionalHeaders = null
        )
        val eventJson = extractEventJson(response)
        val parsed = EventParser.parseEvent(eventJson)
        val enriched = enrichVenueName(parsed, instance)
        upsert(enriched, instance)
        return enriched
    }

    suspend fun deleteEvent(id: String, instance: InstanceProfile) {
        httpClient.requestVoid(
            path = "/api/events/$id",
            method = HttpMethod.DELETE,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        removeFromCache(id, instance)
    }

    suspend fun patchEvent(id: String, body: JsonObject, instance: InstanceProfile): Event {
        val response = httpClient.request<JsonObject>(
            path = "/api/events/$id",
            method = HttpMethod.PATCH,
            query = null,
            body = body.toString(),
            instance = instance,
            additionalHeaders = null
        )
        val eventJson = extractEventJson(response)
        val parsed = EventParser.parseEvent(eventJson)
        upsert(parsed, instance)
        return parsed
    }

    private suspend fun resolveSubdomain(instance: InstanceProfile): Pair<String, String?> {
        mutex.withLock {
            subdomainCache[instance.id]?.let { return it }
        }
        val response = httpClient.request<JsonObject>(
            path = "/api/schedules",
            method = HttpMethod.GET,
            query = mapOf("per_page" to "1000"),
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        val data = response["data"] as? JsonArray ?: JsonArray(emptyList())
        val candidates = data.mapNotNull { it as? JsonObject }
        val chosen = candidates.firstOrNull { it["type"]?.jsonPrimitive?.contentOrNull?.lowercase() == "venue" }
            ?: candidates.firstOrNull()
            ?: throw IllegalStateException("No schedules available for event creation")
        val subdomain = chosen["subdomain"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing schedule subdomain")
        val type = chosen["type"]?.jsonPrimitive?.contentOrNull
        val pair = subdomain to type
        mutex.withLock { subdomainCache[instance.id] = pair }
        return pair
    }

    private fun buildEventPayload(
        event: Event,
        timeZoneOverride: String?,
        options: ExtendedEventOptions?,
        isUpdate: Boolean
    ): JsonObject {
        val tz = timeZoneOverride ?: event.timezone ?: appSettings.timeZoneId
        val payload = buildJsonObject {
            put("id", event.id)
            put("name", event.name)
            event.description?.let { put("description", it) }
            put("starts_at", event.startAt.toString())
            put("end_at", event.endAt.toString())
            if (event.durationMinutes != null) {
                val hours = event.durationMinutes / 60
                put("duration", hours)
                put("duration_minutes", hours)
            }
            put("timezone", tz)

            // Venue + online
            if (!event.venueId.isNullOrBlank()) {
                put("venue_id", event.venueId)
            } else {
                put("venue_id", JsonNull)
            }
            if (!event.roomId.isNullOrBlank()) {
                put("room_id", event.roomId)
            }

            if (!event.onlineUrl.isNullOrBlank()) {
                put("event_url", event.onlineUrl)
                put("online_url", event.onlineUrl)
            } else if (isUpdate) {
                put("event_url", "")
            }

            if (event.images.isNotEmpty()) {
                putJsonArray("images") { event.images.forEach { add(JsonPrimitive(it)) } }
            }

            if (event.capacity != null) put("capacity", event.capacity)
            if (event.ticketTypes.isNotEmpty()) {
                putJsonArray("ticket_types") {
                    event.ticketTypes.forEach {
                        add(buildJsonObject {
                            put("id", it.id)
                            put("name", it.name)
                            if (it.price != null) put("price", it.price)
                            if (it.currency != null) put("currency", it.currency)
                            if (it.quantity != null) put("quantity", it.quantity)
                        })
                    }
                }
            }
            put("publish_state", event.publishState.name)
            if (!event.curatorId.isNullOrBlank()) put("curator_id", event.curatorId)
            if (!event.category.isNullOrBlank()) put("category", event.category)
            if (!event.groupSlug.isNullOrBlank()) put("group_slug", event.groupSlug)
            if (event.isRecurring != null) put("is_recurring", event.isRecurring)
            if (event.attendeesVisible != null) put("attendees_visible", event.attendeesVisible)

            if (event.talentIds.isNotEmpty()) {
                putJsonArray("members") { event.talentIds.forEach { add(JsonPrimitive(it)) } }
            }

            if (options != null) {
                if (!options.categoryName.isNullOrBlank()) put("category_name", options.categoryName)
                if (options.ticketsEnabled != null) put("tickets_enabled", options.ticketsEnabled)
                if (!options.ticketCurrencyCode.isNullOrBlank()) put("ticket_currency_code", options.ticketCurrencyCode)
                if (!options.totalTicketsMode.isNullOrBlank()) put("total_tickets_mode", options.totalTicketsMode)
                if (!options.ticketNotes.isNullOrBlank()) put("ticket_notes", options.ticketNotes)
                if (!options.paymentMethod.isNullOrBlank()) put("payment_method", options.paymentMethod?.lowercase())
                if (!options.paymentInstructions.isNullOrBlank()) put("payment_instructions", options.paymentInstructions)
                if (options.expireUnpaidTickets != null) put("expire_unpaid_tickets", options.expireUnpaidTickets)
                if (options.remindUnpaidTicketsEvery != null) put("remind_unpaid_tickets_every", options.remindUnpaidTicketsEvery)
                if (!options.registrationUrl.isNullOrBlank()) put("registration_url", options.registrationUrl)
                if (!options.eventPassword.isNullOrBlank()) put("event_password", options.eventPassword)
                if (!options.guestListVisibility.isNullOrBlank()) put("guest_list_visibility", options.guestListVisibility)
                if (!options.schedule.isNullOrBlank()) put("schedule", options.schedule)
                if (!options.paymentUrl.isNullOrBlank()) put("payment_url", options.paymentUrl)

                if (options.tickets != null) {
                    putJsonArray("tickets") {
                        options.tickets.forEach { ticket ->
                            add(buildJsonObject {
                                ticket.type?.let { put("type", it) }
                                put("price", ticket.price)
                                put("quantity", ticket.quantity)
                                if (!ticket.description.isNullOrBlank()) put("description", ticket.description)
                            })
                        }
                    }
                }

                if (options.members.isNotEmpty()) {
                    putJsonArray("members") {
                        options.members.forEach { member ->
                            add(buildJsonObject {
                                put("name", member.name)
                                if (!member.email.isNullOrBlank()) put("email", member.email)
                                if (!member.youtubeUrl.isNullOrBlank()) put("youtube_url", member.youtubeUrl)
                            })
                        }
                    }
                }

                if (options.flyerImageId != null) {
                    put("flyer_image_id", options.flyerImageId)
                } else if (options.clearFlyerImage == true) {
                    put("flyer_image_url", JsonNull)
                }
            }
        }
        return payload
    }

    private fun parseEventList(element: JsonElement): List<Event> {
        return when (element) {
            is JsonArray -> element.mapNotNull { (it as? JsonObject)?.let(EventParser::parseEvent) }
            is JsonObject -> {
                val events = (element["events"] as? JsonArray)
                    ?: (element["data"] as? JsonArray)
                events?.mapNotNull { (it as? JsonObject)?.let(EventParser::parseEvent) } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private suspend fun enrichVenueNames(events: List<Event>, instance: InstanceProfile): List<Event> {
        val venues = mutex.withLock { venueCache[instance.id] } ?: run {
            val resources = listEventResources(instance)
            resources.venues.map { Venue(it.id, it.name) }
        }
        val map = venues.associateBy { it.id }
        return events.map { event ->
            if (event.venueName == null && !event.venueId.isNullOrBlank()) {
                val name = map[event.venueId]?.name
                if (name != null) event.copy(venueName = name) else event
            } else {
                event
            }
        }
    }

    private suspend fun enrichVenueName(event: Event, instance: InstanceProfile): Event {
        if (event.venueName != null) return event
        val venues = mutex.withLock { venueCache[instance.id] } ?: run {
            val resources = listEventResources(instance)
            resources.venues.map { Venue(it.id, it.name) }
        }
        val name = venues.firstOrNull { it.id == event.venueId }?.name
        return if (name != null) event.copy(venueName = name) else event
    }

    private fun upsert(event: Event, instance: InstanceProfile) {
        val events = cache[instance.id]?.toMutableList() ?: mutableListOf()
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) events[index] = event else events.add(event)
        cache[instance.id] = events
    }

    private fun removeFromCache(id: String, instance: InstanceProfile) {
        cache[instance.id] = (cache[instance.id] ?: emptyList()).filterNot { it.id == id }
    }

    private fun extractEventJson(response: JsonObject): JsonObject {
        return when {
            response["event"] is JsonObject -> response["event"] as JsonObject
            response["data"] is JsonObject -> response["data"] as JsonObject
            else -> response
        }
    }
}
