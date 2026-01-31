package com.planify.app.data.repository

import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.MediaItem
import com.planify.app.data.model.MediaLibraryResponse
import com.planify.app.data.network.HttpClient
import com.planify.app.data.network.HttpMethod
import com.planify.app.data.storage.ApiKeyStore
import com.planify.app.util.DebugLogger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

class MediaLibraryRepository(
    private val httpClient: HttpClient,
    private val apiKeyStore: ApiKeyStore
) {
    suspend fun fetchMedia(instance: InstanceProfile, page: Int? = null, perPage: Int? = null): MediaLibraryResponse {
        val queryParams = mutableMapOf<String, String?>()
        if (page != null) queryParams["page"] = page.toString()
        queryParams["per_page"] = perPage?.toString() ?: "100"

        val response = httpClient.request<JsonObject>(
            path = "/api/media",
            method = HttpMethod.GET,
            query = queryParams,
            body = null,
            instance = instance,
            additionalHeaders = null
        )

        val data = response["data"] as? JsonArray ?: JsonArray(emptyList())
        val items = data.mapNotNull { (it as? JsonObject)?.let(MediaParser::parseItem) }
        val pagination = response["pagination"] as? JsonObject

        val meta = MediaLibraryResponse.PaginationMeta(
            currentPage = pagination?.get("currentPage")?.jsonPrimitive?.intOrNull ?: page ?: 1,
            lastPage = pagination?.get("lastPage")?.jsonPrimitive?.intOrNull ?: page ?: 1,
            perPage = pagination?.get("perPage")?.jsonPrimitive?.intOrNull ?: perPage ?: 100,
            total = pagination?.get("total")?.jsonPrimitive?.intOrNull ?: items.size
        )

        return MediaLibraryResponse(items, meta)
    }

    suspend fun fetchAllMedia(instance: InstanceProfile): List<MediaItem> {
        var page = 1
        val perPage = 1000
        val all = mutableListOf<MediaItem>()
        while (true) {
            DebugLogger.network("MediaLibrary: fetching page=$page per_page=$perPage")
            val resp = fetchMedia(instance, page, perPage)
            DebugLogger.network("MediaLibrary: page=${resp.pagination.currentPage} last=${resp.pagination.lastPage} items=${resp.data.size}")
            all.addAll(resp.data)
            if (resp.pagination.currentPage >= resp.pagination.lastPage) break
            page += 1
            if (page > 1000) break
        }
        return all
    }

    suspend fun deleteMedia(id: String, instance: InstanceProfile) {
        httpClient.requestVoid(
            path = "/api/media/$id",
            method = HttpMethod.DELETE,
            query = null,
            body = null,
            instance = instance,
            additionalHeaders = null
        )
    }
}
