package com.planify.app.data.repository

import com.planify.app.data.model.Talent
import com.planify.app.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object TalentParser {
    fun parse(obj: JsonObject): Talent {
        return Talent(
            id = obj.intAny(listOf("id")) ?: 0,
            name = obj.string("name") ?: "",
            email = obj.string("email"),
            phone = obj.string("phone"),
            website = obj.string("website"),
            description = obj.string("description"),
            address1 = obj.string("address1"),
            city = obj.string("city"),
            state = obj.string("state"),
            postalCode = obj.stringAny(listOf("postalCode", "postal_code")),
            countryCode = obj.stringAny(listOf("countryCode", "country_code")),
            timezone = obj.string("timezone"),
            subdomain = obj.string("subdomain"),
            createdAt = obj.instantAny(listOf("created_at", "createdAt")),
            updatedAt = obj.instantAny(listOf("updated_at", "updatedAt")),
            profileImageUrl = obj.stringAny(listOf("profileImageUrl", "profile_image_url")),
            headerImageUrl = obj.stringAny(listOf("headerImageUrl", "header_image_url")),
            backgroundImageUrl = obj.stringAny(listOf("backgroundImageUrl", "background_image_url")),
            showEmail = obj.boolean("show_email"),
            scheduleBackgroundType = obj.stringAny(listOf("schedule_background_type", "scheduleBackgroundType")),
            scheduleBackgroundImageUrl = obj.stringAny(listOf("schedule_background_image_url", "scheduleBackgroundImageUrl")),
            scheduleAccentColor = obj.stringAny(listOf("schedule_accent_color", "scheduleAccentColor")),
            scheduleLanguage = obj.stringAny(listOf("schedule_language", "scheduleLanguage")),
            scheduleTimezone = obj.stringAny(listOf("schedule_timezone", "scheduleTimezone")),
            schedule24Hour = obj.boolean("schedule_24_hour") ?: obj.boolean("schedule24Hour"),
            subschedules = parseStringList(obj["subschedules"] as? JsonArray),
            autoImportUrls = parseStringList(obj["auto_import_urls"] as? JsonArray),
            autoImportCities = parseStringList(obj["auto_import_cities"] as? JsonArray)
        )
    }

    private fun parseStringList(array: JsonArray?): List<String>? {
        return array?.mapNotNull { it.jsonPrimitive.contentOrNull }
    }
}
