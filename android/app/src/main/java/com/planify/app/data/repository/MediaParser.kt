package com.planify.app.data.repository

import com.planify.app.data.model.*
import com.planify.app.util.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object MediaParser {
    fun parseItem(obj: JsonObject): MediaItem {
        val id = obj.int("id") ?: 0
        val uuid = obj.string("uuid") ?: ""
        val url = obj.string("url") ?: ""
        val originalFilename = obj.stringAny(listOf("originalFilename", "original_filename")) ?: ""
        val width = obj.int("width")
        val height = obj.int("height")
        val folder = obj.string("folder")
        val usageCount = obj.intAny(listOf("usageCount", "usage_count")) ?: 0
        val usages = parseUsages(obj["usages"] as? JsonArray)
        val tags = parseTags(obj["tags"] as? JsonArray)
        val variants = parseVariants(obj["variants"] as? JsonArray)
        return MediaItem(
            id = id,
            uuid = uuid,
            url = url,
            originalFilename = originalFilename,
            width = width,
            height = height,
            folder = folder,
            usageCount = usageCount,
            usages = usages,
            tags = tags,
            variants = variants
        )
    }

    private fun parseUsages(array: JsonArray?): List<MediaUsage> {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            MediaUsage(
                id = obj.int("id") ?: 0,
                context = obj.string("context"),
                contextLabel = obj.stringAny(listOf("contextLabel", "context_label")),
                type = obj.string("type"),
                displayName = obj.stringAny(listOf("displayName", "display_name")),
                usableId = obj.intAny(listOf("usable_id", "usableId")),
                variantId = obj.intAny(listOf("variant_id", "variantId"))
            )
        } ?: emptyList()
    }

    private fun parseTags(array: JsonArray?): List<MediaTag> {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            MediaTag(
                id = obj.int("id") ?: 0,
                name = obj.string("name") ?: "",
                slug = obj.string("slug") ?: ""
            )
        } ?: emptyList()
    }

    private fun parseVariants(array: JsonArray?): List<MediaVariant> {
        return array?.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            MediaVariant(
                id = obj.int("id") ?: 0,
                label = obj.string("label") ?: "",
                url = obj.string("url") ?: "",
                width = obj.int("width"),
                height = obj.int("height")
            )
        } ?: emptyList()
    }
}
