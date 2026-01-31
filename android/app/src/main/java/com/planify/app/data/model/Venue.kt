package com.planify.app.data.model

import com.planify.app.util.stringAny
import kotlinx.serialization.json.JsonObject


data class Venue(
    val id: String,
    val name: String
) {
    companion object {
        fun fromJson(obj: JsonObject): Venue {
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            val name = obj.stringAny(listOf("name", "title", "label")) ?: id
            return Venue(id, name.ifBlank { id })
        }
    }
}
