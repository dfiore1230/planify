package com.planify.app.data.repository

import com.planify.app.data.model.CapabilitiesDocument
import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.ThemeDTO
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class DiscoveryService(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchCapabilities(baseUrl: String): CapabilitiesDocument {
        val bootstrap = InstanceProfile(
            id = java.util.UUID.randomUUID().toString(),
            displayName = baseUrl,
            baseUrl = baseUrl,
            theme = ThemeDTO.default()
        )
        val (bytes, _) = httpClient.requestRaw(
            path = "/.well-known/planify.json",
            method = HttpMethod.GET,
            query = null,
            body = null,
            instance = bootstrap,
            additionalHeaders = null
        )
        val obj = json.parseToJsonElement(bytes.decodeToString()) as JsonObject
        return CapabilitiesDocument.fromJson(obj)
    }
}
