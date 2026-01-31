package com.planify.app.util

import kotlinx.datetime.Instant
import kotlinx.serialization.json.*

fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
fun JsonObject.stringAny(keys: List<String>): String? {
    for (key in keys) {
        val value = string(key)
        if (!value.isNullOrBlank()) return value
    }
    return null
}

fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
fun JsonObject.intAny(keys: List<String>): Int? {
    for (key in keys) {
        val value = int(key)
        if (value != null) return value
    }
    return null
}

fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

fun JsonObject.boolean(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

fun JsonObject.instantAny(keys: List<String>): Instant? {
    for (key in keys) {
        val value = this[key]
        if (value != null) {
            val primitive = value.jsonPrimitive
            val content = primitive.contentOrNull
            if (content != null) {
                try {
                    return Instant.parse(content)
                } catch (_: Exception) {
                    try {
                        val odt = java.time.OffsetDateTime.parse(content)
                        return Instant.fromEpochMilliseconds(odt.toInstant().toEpochMilli())
                    } catch (_: Exception) {
                        try {
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            val ldt = java.time.LocalDateTime.parse(content, formatter)
                            return Instant.fromEpochMilliseconds(ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                        } catch (_: Exception) {
                            try {
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                                val ldt = java.time.LocalDateTime.parse(content, formatter)
                                return Instant.fromEpochMilliseconds(ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
                            } catch (_: Exception) {
                                // continue
                            }
                        }
                    }
                }
            }
            val doubleValue = primitive.doubleOrNull
            if (doubleValue != null) {
                val seconds = if (doubleValue > 1_000_000_000_000) doubleValue / 1000.0 else doubleValue
                return Instant.fromEpochSeconds(seconds.toLong())
            }
        }
    }
    return null
}

fun JsonObject.stringList(key: String): List<String> {
    val arr = this[key] as? JsonArray ?: return emptyList()
    return arr.mapNotNull { it.jsonPrimitive.contentOrNull }
}

fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.arrayOrNull(key: String): JsonArray? = this[key] as? JsonArray
