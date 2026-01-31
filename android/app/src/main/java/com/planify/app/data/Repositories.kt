package com.planify.app.data

import com.planify.app.data.network.HttpClient
import com.planify.app.data.repository.BrandingService
import com.planify.app.data.repository.CheckInRepository
import com.planify.app.data.repository.DiscoveryService
import com.planify.app.data.repository.EventRepository
import com.planify.app.data.repository.MediaLibraryRepository
import com.planify.app.data.repository.TalentRepository
import com.planify.app.data.repository.TicketRepository
import com.planify.app.data.repository.VenueRepository
import com.planify.app.data.storage.ApiKeyStore

class Repositories(
    httpClient: HttpClient,
    appSettings: AppSettings,
    apiKeyStore: ApiKeyStore
) {
    val discoveryService = DiscoveryService(httpClient)
    val brandingService = BrandingService(httpClient)
    val eventRepository = EventRepository(httpClient, appSettings)
    val venueRepository = VenueRepository(httpClient)
    val talentRepository = TalentRepository(httpClient)
    val ticketRepository = TicketRepository(httpClient)
    val checkInRepository = CheckInRepository(httpClient)
    val mediaLibraryRepository = MediaLibraryRepository(httpClient, apiKeyStore)
}
