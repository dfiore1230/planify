package com.planify.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class InstanceEnvironment {
    prod,
    staging,
    dev
}

@Serializable
enum class AuthMethod {
    sanctum,
    oauth2,
    jwt
}

@Serializable
data class ThemeDTO(
    val primaryHex: String = "#007AFF",
    val secondaryHex: String = "#8E8E93",
    val accentHex: String = "#34C759",
    val textHex: String = "#000000",
    val backgroundHex: String = "#FFFFFF",
    val buttonRadius: Float = 10f,
    val legalFooter: String? = null
) {
    companion object {
        fun default(): ThemeDTO = ThemeDTO()
    }
}

@Serializable
data class InstanceProfile(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val environment: InstanceEnvironment = InstanceEnvironment.prod,
    val authMethod: AuthMethod = AuthMethod.sanctum,
    val authEndpoints: Map<String, String>? = null,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val minAppVersion: String? = null,
    val rateLimits: Map<String, Int>? = null,
    val tokenIdentifier: String? = null,
    val theme: ThemeDTO? = null
)
