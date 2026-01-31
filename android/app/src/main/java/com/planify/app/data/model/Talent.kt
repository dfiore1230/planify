package com.planify.app.data.model

import kotlinx.datetime.Instant


data class Talent(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val description: String? = null,
    val address1: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
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
    val autoImportCities: List<String>? = null
) {
    val role: String?
        get() = description

    val bio: String?
        get() = description
}
