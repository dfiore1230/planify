package com.planify.app.data.repository

import com.planify.app.data.model.Event
import com.planify.app.data.model.PublishState
import com.planify.app.data.model.TicketType
import com.planify.app.util.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object EventParser {
    fun parseEvent(obj: JsonObject): Event {
        val id = obj.string("id") ?: ""
        val name = obj.string("name") ?: ""
        val description = obj.string("description")

        val rawStarts = obj.string("starts_at")
        val rawEnds = obj.string("ends_at")
        val timezone = obj.string("timezone")

        val startAt = obj.instantAny(listOf("startAt", "startsAt", "startTime", "start", "starts_at"))
            ?: Instant.fromEpochSeconds(System.currentTimeMillis() / 1000)

        val durationHours = obj.intAny(listOf("durationMinutes", "duration"))
        val durationMinutes = durationHours?.let { it * 60 }

        val endAt = obj.instantAny(listOf("endAt", "endsAt", "endTime", "end", "ends_at"))
            ?: durationMinutes?.let { startAt.plus(it * 60L, DateTimeUnit.SECOND) }
            ?: startAt.plus(3600, DateTimeUnit.SECOND)

        val venueId = obj.string("venueId") ?: obj.objectOrNull("venue")?.string("id")
        val venueName = obj.objectOrNull("venue")?.string("name")

        val roomId = obj.string("roomId")
        val images = obj.stringList("images")

        val flyerImageUrl = obj.stringAny(listOf("flyerImageUrl", "flyer_image_url"))
        val flyerImageId = obj.stringAny(listOf("flyerImageId", "flyer_image_id"))

        val capacity = obj.int("capacity")

        val publishState = when (obj.string("publishState")?.lowercase()) {
            "published" -> PublishState.published
            "archived" -> PublishState.archived
            else -> PublishState.draft
        }

        val category = when {
            obj["category"] is JsonPrimitive -> obj.string("category")
            obj.objectOrNull("category") != null -> obj.objectOrNull("category")?.string("name")
                ?: obj.objectOrNull("category")?.int("id")?.toString()
            else -> null
        }

        val groupSlug = obj.string("group_slug") ?: obj.string("groupSlug")

        val onlineUrl = obj.stringAny(listOf("online_url", "event_url", "url", "onlineUrl", "eventUrl"))

        val isRecurring = obj.boolean("is_recurring") ?: obj.boolean("isRecurring")
        val attendeesVisible = obj.boolean("attendees_visible") ?: obj.boolean("attendeesVisible")

        val curatorId = obj.stringAny(listOf("curatorId", "curator_role_id", "curator_id"))
            ?: obj.objectOrNull("curator")?.string("id")

        val talentIds = parseTalentIds(obj)

        val ticketTypes = parseTicketTypes(obj)

        return Event(
            id = id,
            name = name,
            description = description,
            startAt = startAt,
            endAt = endAt,
            durationMinutes = durationMinutes,
            venueId = venueId,
            venueName = venueName,
            roomId = roomId,
            images = images,
            flyerImageUrl = flyerImageUrl,
            flyerImageId = flyerImageId,
            capacity = capacity,
            ticketTypes = ticketTypes,
            publishState = publishState,
            timezone = timezone,
            curatorId = curatorId,
            talentIds = talentIds,
            category = category,
            groupSlug = groupSlug,
            onlineUrl = onlineUrl,
            isRecurring = isRecurring,
            attendeesVisible = attendeesVisible,
            rawStartsAtString = rawStarts,
            rawEndsAtString = rawEnds,
            rawTimezoneIdentifier = timezone
        )
    }

    private fun parseTalentIds(obj: JsonObject): List<String> {
        obj.arrayOrNull("talentIds")?.let { return it.mapNotNull { el -> el.jsonPrimitive.contentOrNull } }
        obj.arrayOrNull("talent_ids")?.let { return it.mapNotNull { el -> el.jsonPrimitive.contentOrNull } }
        obj.arrayOrNull("member_role_ids")?.let { return it.mapNotNull { el -> el.jsonPrimitive.contentOrNull } }
        val members = obj["members"]
        if (members is JsonObject) {
            return members.keys.toList()
        }
        if (members is JsonArray) {
            return members.mapNotNull { item ->
                (item as? JsonObject)?.string("id") ?: item.jsonPrimitive.contentOrNull
            }
        }
        return emptyList()
    }

    private fun parseTicketTypes(obj: JsonObject): List<TicketType> {
        obj.arrayOrNull("ticketTypes")?.let { array ->
            return array.mapNotNull { item ->
                val ticket = item as? JsonObject ?: return@mapNotNull null
                val id = ticket.string("id") ?: return@mapNotNull null
                val name = ticket.stringAny(listOf("name", "type")) ?: "Ticket"
                val price = ticket.double("price")
                val currency = ticket.string("currency")
                val quantity = ticket.int("quantity")
                TicketType(id, name, price, currency, quantity)
            }
        }

        obj.arrayOrNull("tickets")?.let { array ->
            return array.mapNotNull { item ->
                val ticket = item as? JsonObject ?: return@mapNotNull null
                val id = ticket.string("id") ?: return@mapNotNull null
                val name = ticket.stringAny(listOf("name", "type")) ?: "Ticket"
                val price = ticket.double("price")
                val currency = ticket.string("currency")
                val quantity = ticket.int("quantity")
                TicketType(id, name, price, currency, quantity)
            }
        }

        return emptyList()
    }
}
