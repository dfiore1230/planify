package com.planify.app.data.repository

import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.VenueDetail
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class VenueRepository(private val httpClient: HttpClient) {
    suspend fun fetchAll(instance: InstanceProfile): List<VenueDetail> {
        val response = httpClient.request<JsonObject>(
            path = "/api/venues",
            method = HttpMethod.GET,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        val data = response["data"] as? JsonArray ?: JsonArray(emptyList())
        return data.mapNotNull { (it as? JsonObject)?.let(VenueParser::parseDetail) }
    }

    suspend fun fetch(id: Int, instance: InstanceProfile): VenueDetail {
        val response = httpClient.request<JsonObject>(
            path = "/api/venues/$id",
            method = HttpMethod.GET,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
        return VenueParser.parseDetail(response)
    }

    suspend fun create(venue: VenueDetail, instance: InstanceProfile): VenueDetail {
        val response = httpClient.request<JsonObject>(
            path = "/api/venues",
            method = HttpMethod.POST,
            query = null,
            body = buildVenuePayload(venue).toString(),
            instance = instance,
            additionalHeaders = null
        )
        return VenueParser.parseDetail(response)
    }

    suspend fun update(venue: VenueDetail, instance: InstanceProfile): VenueDetail {
        val response = httpClient.request<JsonObject>(
            path = "/api/venues/${venue.id}",
            method = HttpMethod.PUT,
            query = null,
            body = buildVenuePayload(venue).toString(),
            instance = instance,
            additionalHeaders = null
        )
        return VenueParser.parseDetail(response)
    }

    suspend fun delete(id: Int, instance: InstanceProfile) {
        httpClient.requestVoid(
            path = "/api/venues/$id",
            method = HttpMethod.DELETE,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
    }

    private fun buildVenuePayload(venue: VenueDetail): JsonObject {
        return buildJsonObject {
            put("name", venue.name)
            venue.email?.let { put("email", it) }
            venue.phone?.let { put("phone", it) }
            venue.website?.let { put("website", it) }
            venue.description?.let { put("description", it) }
            venue.address1?.let { put("address1", it) }
            venue.address2?.let { put("address2", it) }
            venue.city?.let { put("city", it) }
            venue.state?.let { put("state", it) }
            venue.postalCode?.let { put("postal_code", it) }
            venue.countryCode?.let { put("country_code", it) }
            venue.timezone?.let { put("timezone", it) }
            venue.subdomain?.let { put("subdomain", it) }
            venue.profileImageUrl?.let { put("profile_image_url", it) }
            venue.headerImageUrl?.let { put("header_image_url", it) }
            venue.backgroundImageUrl?.let { put("background_image_url", it) }
            venue.showEmail?.let { put("show_email", it) }
            venue.scheduleBackgroundType?.let { put("schedule_background_type", it) }
            venue.scheduleBackgroundImageUrl?.let { put("schedule_background_image_url", it) }
            venue.scheduleAccentColor?.let { put("schedule_accent_color", it) }
            venue.scheduleLanguage?.let { put("schedule_language", it) }
            venue.scheduleTimezone?.let { put("schedule_timezone", it) }
            venue.schedule24Hour?.let { put("schedule_24_hour", it) }
            venue.subschedules?.let { put("subschedules", it.joinToString(",")) }
            venue.autoImportUrls?.let { put("auto_import_urls", it.joinToString(",")) }
            venue.autoImportCities?.let { put("auto_import_cities", it.joinToString(",")) }
        }
    }
}
