package com.planify.app.data.repository

import com.planify.app.data.model.*
import com.planify.app.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object VenueParser {
    fun parseDetail(obj: JsonObject): VenueDetail {
        return VenueDetail(
            id = obj.int("id") ?: 0,
            name = obj.string("name") ?: "",
            email = obj.string("email"),
            phone = obj.string("phone"),
            website = obj.string("website"),
            description = obj.string("description"),
            address1 = obj.string("address1"),
            address2 = obj.string("address2"),
            city = obj.string("city"),
            state = obj.string("state"),
            postalCode = obj.stringAny(listOf("postal_code", "postalCode")),
            countryCode = obj.stringAny(listOf("country_code", "countryCode")),
            formattedAddress = obj.stringAny(listOf("formatted_address", "formattedAddress")),
            geoLat = obj.double("geo_lat") ?: obj.double("geoLat"),
            geoLon = obj.double("geo_lon") ?: obj.double("geoLon"),
            timezone = obj.string("timezone"),
            subdomain = obj.string("subdomain"),
            createdAt = obj.instantAny(listOf("created_at", "createdAt")),
            updatedAt = obj.instantAny(listOf("updated_at", "updatedAt")),
            profileImageUrl = obj.stringAny(listOf("profile_image_url", "profileImageUrl")),
            headerImageUrl = obj.stringAny(listOf("header_image_url", "headerImageUrl")),
            backgroundImageUrl = obj.stringAny(listOf("background_image_url", "backgroundImageUrl")),
            showEmail = obj.boolean("show_email") ?: obj.boolean("showEmail"),
            scheduleBackgroundType = obj.stringAny(listOf("schedule_background_type", "scheduleBackgroundType")),
            scheduleBackgroundImageUrl = obj.stringAny(listOf("schedule_background_image_url", "scheduleBackgroundImageUrl")),
            scheduleAccentColor = obj.stringAny(listOf("schedule_accent_color", "scheduleAccentColor")),
            scheduleLanguage = obj.stringAny(listOf("schedule_language", "scheduleLanguage")),
            scheduleTimezone = obj.stringAny(listOf("schedule_timezone", "scheduleTimezone")),
            schedule24Hour = obj.boolean("schedule_24_hour") ?: obj.boolean("schedule24Hour"),
            subschedules = parseStringList(obj["subschedules"] as? JsonArray),
            autoImportUrls = parseStringList(obj["auto_import_urls"] as? JsonArray),
            autoImportCities = parseStringList(obj["auto_import_cities"] as? JsonArray),
            rooms = parseRooms(obj["rooms"] as? JsonArray),
            contacts = parseContacts(obj["contacts"] as? JsonArray)
        )
    }

    private fun parseStringList(array: JsonArray?): List<String>? {
        return array?.mapNotNull { it.jsonPrimitive.contentOrNull }
    }

    private fun parseRooms(array: JsonArray?): List<VenueRoom>? {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            VenueRoom(
                id = obj.string("id") ?: java.util.UUID.randomUUID().toString(),
                name = obj.string("name") ?: "",
                capacity = obj.int("capacity"),
                description = obj.string("description")
            )
        }
    }

    private fun parseContacts(array: JsonArray?): List<VenueContact>? {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            VenueContact(
                id = obj.string("id") ?: java.util.UUID.randomUUID().toString(),
                name = obj.string("name") ?: "",
                role = obj.string("role"),
                email = obj.string("email"),
                phone = obj.string("phone")
            )
        }
    }
}
