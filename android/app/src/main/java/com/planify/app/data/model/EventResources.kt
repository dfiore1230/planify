package com.planify.app.data.model

import com.planify.app.util.objectOrNull
import com.planify.app.util.string
import com.planify.app.util.stringAny
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject


data class EventRole(
    val id: String,
    val name: String,
    val type: String
)

data class EventGroup(
    val id: String,
    val name: String,
    val slug: String
)

data class EventResources(
    val venues: List<EventRole> = emptyList(),
    val curators: List<EventRole> = emptyList(),
    val talent: List<EventRole> = emptyList(),
    val categories: List<String> = emptyList(),
    val groups: List<EventGroup> = emptyList()
) {
    companion object {
        fun fromJson(obj: JsonObject): EventResources {
            val nested = obj.objectOrNull("data") ?: obj
            val venues = parseRoles(nested["venues"] as? JsonArray)
            val curators = parseRoles(nested["curators"] as? JsonArray)
            val talent = parseRoles(nested["talent"] as? JsonArray)
            val categories = (nested["categories"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList()
            val groups = (nested["groups"] as? JsonArray)
                ?.mapNotNull { item ->
                    val group = item as? JsonObject ?: return@mapNotNull null
                    val id = group.stringAny(listOf("id", "slug")) ?: return@mapNotNull null
                    val name = group.string("name") ?: id
                    val slug = group.string("slug") ?: id
                    EventGroup(id, name, slug)
                }
                ?: emptyList()
            return EventResources(venues, curators, talent, categories, groups)
        }

        private fun parseRoles(array: JsonArray?): List<EventRole> {
            return array?.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                val id = obj.stringAny(listOf("encodedId", "id")) ?: return@mapNotNull null
                val name = obj.stringAny(listOf("name", "title", "label")) ?: id
                val type = obj.stringAny(listOf("type", "roleType")) ?: ""
                EventRole(id, name.ifBlank { id }, type)
            } ?: emptyList()
        }
    }
}
