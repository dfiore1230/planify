package com.planify.app.data.repository

import com.planify.app.data.model.BrandingResponse
import com.planify.app.data.model.CapabilitiesDocument
import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.ThemeDTO
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class BrandingService(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchBranding(capabilities: CapabilitiesDocument): BrandingResponse {
        val brandingUrl = capabilities.brandingEndpoint
        val uri = java.net.URI(brandingUrl)
        val base = "${uri.scheme}://${uri.authority}"
        val path = uri.path.ifBlank { "/" }
        val query = uri.rawQuery?.split("&")?.mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.isEmpty()) null else pieces[0] to (pieces.getOrNull(1))
        }?.toMap()

        val bootstrap = InstanceProfile(
            id = java.util.UUID.randomUUID().toString(),
            displayName = base,
            baseUrl = base,
            featureFlags = capabilities.features,
            minAppVersion = capabilities.minAppVersion,
            rateLimits = capabilities.rateLimits,
            theme = ThemeDTO.default()
        )

        val (bytes, _) = httpClient.requestRaw(
            path = path,
            method = HttpMethod.GET,
            query = query,
            body = null,
            instance = bootstrap,
            additionalHeaders = null
        )
        val obj = json.parseToJsonElement(bytes.decodeToString()) as JsonObject
        return BrandingResponse.fromJson(obj)
    }
}
