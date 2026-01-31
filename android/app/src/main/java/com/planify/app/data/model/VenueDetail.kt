package com.planify.app.data.model

import kotlinx.datetime.Instant


data class VenueRoom(
    val id: String,
    val name: String,
    val capacity: Int? = null,
    val description: String? = null
)

data class VenueContact(
    val id: String,
    val name: String,
    val role: String? = null,
    val email: String? = null,
    val phone: String? = null
)

data class VenueDetail(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val description: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
    val formattedAddress: String? = null,
    val geoLat: Double? = null,
    val geoLon: Double? = null,
    val timezone: String? = null,
    val subdomain: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val profileImageUrl: String? = null,
    val headerImageUrl: String? = null,
    val backgroundImageUrl: String? = null,
    val showEmail: Boolean? = null,
    val scheduleBackgroundType: String? = null,
    val scheduleBackgroundImageUrl: String? = null,
    val scheduleAccentColor: String? = null,
    val scheduleLanguage: String? = null,
    val scheduleTimezone: String? = null,
    val schedule24Hour: Boolean? = null,
    val subschedules: List<String>? = null,
    val autoImportUrls: List<String>? = null,
    val autoImportCities: List<String>? = null,
    val rooms: List<VenueRoom>? = null,
    val contacts: List<VenueContact>? = null
) {
    val displayAddress: String
        get() {
            if (!formattedAddress.isNullOrBlank()) return formattedAddress
            val parts = listOfNotNull(address1, city, state, postalCode).filter { it.isNotBlank() }
            return parts.joinToString(", ")
        }

    val hasLocation: Boolean
        get() = geoLat != null && geoLon != null

    fun toVenue(): Venue = Venue(id.toString(), name)
}
