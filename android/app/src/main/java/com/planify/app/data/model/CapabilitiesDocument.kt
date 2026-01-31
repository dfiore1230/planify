package com.planify.app.data.model

import com.planify.app.util.objectOrNull
import com.planify.app.util.string
import com.planify.app.util.stringAny
import kotlinx.serialization.json.JsonObject


data class CapabilitiesDocument(
    val apiBaseUrl: String,
    val auth: AuthConfig,
    val brandingEndpoint: String,
    val features: Map<String, Boolean>,
    val versions: Map<String, String>?,
    val minAppVersion: String?,
    val rateLimits: Map<String, Int>?
) {
    data class AuthConfig(
        val type: AuthType,
        val endpoints: Map<String, String>
    )

    enum class AuthType {
        sanctum,
        passport,
        jwt
    }

    companion object {
        fun fromJson(obj: JsonObject): CapabilitiesDocument {
            val apiBaseUrl = obj.stringAny(listOf("apiBaseURL", "api_base_url")) ?: "https://localhost"

            val auth = obj.objectOrNull("auth")?.let { authObj ->
                val typeValue = authObj.string("type")?.lowercase() ?: "jwt"
                val type = when (typeValue) {
                    "sanctum" -> AuthType.sanctum
                    "passport" -> AuthType.passport
                    else -> AuthType.jwt
                }
                val endpoints = authObj["endpoints"]
                    ?.let { it as? JsonObject }
                    ?.entries
                    ?.associate { (key, value) -> key to value.jsonPrimitive.content }
                    ?: emptyMap()
                AuthConfig(type, endpoints)
            } ?: AuthConfig(AuthType.jwt, emptyMap())

            val brandingRaw = obj.stringAny(
                listOf("brandingEndpoint", "branding_endpoint", "brandingendpoint")
            ) ?: apiBaseUrl
            val brandingEndpoint = if (brandingRaw.startsWith("http://") || brandingRaw.startsWith("https://")) {
                brandingRaw
            } else {
                try {
                    val baseUri = java.net.URI(apiBaseUrl)
                    baseUri.resolve(brandingRaw).toString()
                } catch (_: Exception) {
                    brandingRaw
                }
            }

            val features = when (val f = obj["features"]) {
                is JsonObject -> f.entries.associate { it.key to (it.value.jsonPrimitive.booleanOrNull ?: false) }
                else -> emptyMap()
            }

            val versions = (obj["versions"] as? JsonObject)
                ?.entries
                ?.associate { it.key to it.value.jsonPrimitive.content }

            val minAppVersion = obj.stringAny(listOf("min_app_version", "minAppVersion"))
            val rateLimits = (obj["rate_limits"] as? JsonObject)
                ?.entries
                ?.mapNotNull { (k, v) ->
                    val num = v.jsonPrimitive.intOrNull
                    if (num != null) k to num else null
                }
                ?.toMap()

            return CapabilitiesDocument(
                apiBaseUrl = apiBaseUrl,
                auth = auth,
                brandingEndpoint = brandingEndpoint,
                features = features,
                versions = versions,
                minAppVersion = minAppVersion,
                rateLimits = rateLimits
            )
        }
    }
}
