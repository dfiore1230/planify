package com.planify.app.data.repository

import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.Talent
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TalentRepository(private val httpClient: HttpClient) {
    suspend fun fetchAll(instance: InstanceProfile): List<Talent> {
        val response = httpClient.request<JsonObject>(
            path = "/api/talent",
            method = HttpMethod.GET,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        val data = response["data"] as? JsonArray ?: JsonArray(emptyList())
        return data.mapNotNull { (it as? JsonObject)?.let(TalentParser::parse) }
    }

    suspend fun fetch(id: Int, instance: InstanceProfile): Talent {
        val response = httpClient.request<JsonObject>(
            path = "/api/talent/$id",
            method = HttpMethod.GET,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        return TalentParser.parse(response)
    }

    suspend fun create(talent: Talent, instance: InstanceProfile): Talent {
        val response = httpClient.request<JsonObject>(
            path = "/api/talent",
            method = HttpMethod.POST,
            query = null,
            body = buildTalentPayload(talent).toString(),
            instance = instance,
            additionalHeaders = null
        )
        return TalentParser.parse(response)
    }

    suspend fun update(talent: Talent, instance: InstanceProfile): Talent {
        val response = httpClient.request<JsonObject>(
            path = "/api/talent/${talent.id}",
            method = HttpMethod.PUT,
            query = null,
            body = buildTalentPayload(talent).toString(),
            instance = instance,
            additionalHeaders = null
        )
        return TalentParser.parse(response)
    }

    suspend fun delete(id: Int, instance: InstanceProfile) {
        httpClient.requestVoid(
            path = "/api/talent/$id",
            method = HttpMethod.DELETE,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
    }

    private fun buildTalentPayload(talent: Talent): JsonObject {
        return buildJsonObject {
            put("name", talent.name)
            talent.email?.let { put("email", it) }
            talent.phone?.let { put("phone", it) }
            talent.website?.let { put("website", it) }
            talent.description?.let { put("description", it) }
            talent.address1?.let { put("address1", it) }
            talent.city?.let { put("city", it) }
            talent.state?.let { put("state", it) }
            talent.postalCode?.let { put("postal_code", it) }
            talent.countryCode?.let { put("country_code", it) }
            talent.timezone?.let { put("timezone", it) }
            talent.subdomain?.let { put("subdomain", it) }
            talent.profileImageUrl?.let { put("profile_image_url", it) }
            talent.headerImageUrl?.let { put("header_image_url", it) }
            talent.backgroundImageUrl?.let { put("background_image_url", it) }
            talent.showEmail?.let { put("show_email", it) }
            talent.scheduleBackgroundType?.let { put("schedule_background_type", it) }
            talent.scheduleBackgroundImageUrl?.let { put("schedule_background_image_url", it) }
            talent.scheduleAccentColor?.let { put("schedule_accent_color", it) }
            talent.scheduleLanguage?.let { put("schedule_language", it) }
            talent.scheduleTimezone?.let { put("schedule_timezone", it) }
            talent.schedule24Hour?.let { put("schedule_24_hour", it) }
            talent.subschedules?.let { put("subschedules", it.joinToString(",")) }
            talent.autoImportUrls?.let { put("auto_import_urls", it.joinToString(",")) }
            talent.autoImportCities?.let { put("auto_import_cities", it.joinToString(",")) }
        }
    }
}
