package com.planify.app.data.model

import kotlinx.datetime.Instant

enum class PublishState {
    draft,
    published,
    archived
}

data class TicketType(
    val id: String,
    val name: String,
    val price: Double? = null,
    val currency: String? = null,
    val quantity: Int? = null
)

data class Event(
    val id: String,
    val name: String,
    val description: String? = null,
    val startAt: Instant,
    val endAt: Instant,
    val durationMinutes: Int? = null,
    val venueId: String? = null,
    val venueName: String? = null,
    val roomId: String? = null,
    val images: List<String> = emptyList(),
    val flyerImageUrl: String? = null,
    val flyerImageId: String? = null,
    val capacity: Int? = null,
    val ticketTypes: List<TicketType> = emptyList(),
    val publishState: PublishState = PublishState.draft,
    val timezone: String? = null,
    val curatorId: String? = null,
    val talentIds: List<String> = emptyList(),
    val category: String? = null,
    val groupSlug: String? = null,
    val onlineUrl: String? = null,
    val isRecurring: Boolean? = null,
    val attendeesVisible: Boolean? = null,
    val rawStartsAtString: String? = null,
    val rawEndsAtString: String? = null,
    val rawTimezoneIdentifier: String? = null
) {
    val venueDisplayDescription: String
        get() = when {
            !venueName.isNullOrBlank() -> venueName
            !venueId.isNullOrBlank() -> venueId
            else -> "Online"
        }
}
