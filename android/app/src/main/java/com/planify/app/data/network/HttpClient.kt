package com.planify.app.data.network

import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.storage.ApiKeyStore
import com.planify.app.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLEncoder

class HttpClient(private val apiKeyStore: ApiKeyStore) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder().build()

    suspend inline fun <reified T> request(
        path: String,
        method: HttpMethod = HttpMethod.GET,
        query: Map<String, String?>? = null,
        body: Any? = null,
        instance: InstanceProfile,
        additionalHeaders: Map<String, String>? = null
    ): T {
        val (data, response) = requestRaw(path, method, query, body, instance, additionalHeaders)
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        if (response.isSuccessful && !contentType.contains("json")) {
            val preview = data.decodeToString().take(1024)
            DebugLogger.error("Non-JSON 2xx response for ${response.request.url}")
            throw ApiError.DecodingError("Non-JSON response", preview)
        }
        if (data.isEmpty()) {
            throw ApiError.ServerError(response.code, "Empty response body")
        }
        return try {
            json.decodeFromString(data.decodeToString())
        } catch (e: SerializationException) {
            val preview = data.decodeToString().take(1024)
            DebugLogger.error("Decoding failed for ${T::class}")
            throw ApiError.DecodingError(e.message ?: "decode error", preview)
        }
    }

    suspend fun requestVoid(
        path: String,
        method: HttpMethod,
        query: Map<String, String?>? = null,
        body: Any? = null,
        instance: InstanceProfile,
        additionalHeaders: Map<String, String>? = null
    ) {
        requestRaw(path, method, query, body, instance, additionalHeaders)
    }

    suspend fun requestRaw(
        path: String,
        method: HttpMethod,
        query: Map<String, String?>? = null,
        body: Any? = null,
        instance: InstanceProfile,
        additionalHeaders: Map<String, String>? = null
    ): Pair<ByteArray, Response> {
        return withContext(Dispatchers.IO) {
            val url = buildUrl(instance.baseUrl, path, query)
            val requestBuilder = Request.Builder().url(url)
            requestBuilder.addHeader("Accept", "application/json")
            requestBuilder.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            requestBuilder.addHeader("Pragma", "no-cache")

            val apiKey = apiKeyStore.load(instance)
            if (apiKey != null) {
                requestBuilder.addHeader("X-API-Key", apiKey)
            }

            additionalHeaders?.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val requestBody = body?.let { payload ->
                when (payload) {
                    is String -> payload.toRequestBody("application/json".toMediaType())
                    is ByteArray -> payload.toRequestBody("application/json".toMediaType())
                    else -> {
                        @OptIn(ExperimentalSerializationApi::class)
                        val serializer = json.serializersModule.serializer(payload::class.java)
                        val jsonString = json.encodeToString(serializer, payload)
                        jsonString.toRequestBody("application/json".toMediaType())
                    }
                }
            }

            requestBuilder.method(method.value, requestBody)
            val request = requestBuilder.build()

            DebugLogger.network("HTTP ➡️ ${method.value} ${request.url}")

            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes() ?: ByteArray(0)

            when (response.code) {
                in 200..299 -> Unit
                401 -> throw ApiError.Unauthorized
                403 -> throw ApiError.Forbidden
                429 -> {
                    val retryAfter = response.header("Retry-After")?.toDoubleOrNull()
                    throw ApiError.RateLimited(retryAfter)
                }
                else -> {
                    val message = bytes.decodeToString()
                    throw ApiError.ServerError(response.code, message)
                }
            }

            Pair(bytes, response)
        }
    }

    private fun buildUrl(baseUrl: String, path: String, query: Map<String, String?>?): String {
        val baseUri = java.net.URI(baseUrl)
        val resolvedUri = if (path.startsWith("/")) {
            java.net.URI(baseUri.scheme, baseUri.authority, path, null, null)
        } else {
            val basePath = baseUri.path?.trimEnd('/') ?: ""
            val combined = if (basePath.isEmpty()) "/$path" else "$basePath/$path"
            java.net.URI(baseUri.scheme, baseUri.authority, combined, null, null)
        }

        val url = StringBuilder(resolvedUri.toString())
        if (!query.isNullOrEmpty()) {
            val encoded = query.entries
                .filter { it.value != null }
                .joinToString("&") { (key, value) ->
                    val encodedValue = URLEncoder.encode(value, "UTF-8")
                    "$key=$encodedValue"
                }
            if (encoded.isNotEmpty()) {
                url.append("?").append(encoded)
            }
        }
        return url.toString()
    }
}

enum class HttpMethod(val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE")
}

sealed class ApiError(message: String? = null) : Exception(message) {
    object Unauthorized : ApiError("Unauthorized")
    object Forbidden : ApiError("Forbidden")
    data class RateLimited(val retryAfterSeconds: Double?) : ApiError("Rate limited")
    data class ServerError(val statusCode: Int, val body: String?) : ApiError("Server error")
    data class DecodingError(val reason: String, val bodyPreview: String) : ApiError("Decoding error")
    object InvalidUrl : ApiError("Invalid URL")
    data class NetworkError(val reason: String) : ApiError("Network error")
}
