package com.planify.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.planify.app.LocalAppSettings
import com.planify.app.LocalInstanceStore
import com.planify.app.LocalRepositories
import com.planify.app.data.model.Event
import com.planify.app.data.model.EventGroup
import com.planify.app.data.model.MediaItem
import com.planify.app.data.model.PublishState
import com.planify.app.data.model.Venue
import com.planify.app.data.repository.EventRepository
import com.planify.app.ui.components.MediaLibraryPicker
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinInstant
import java.time.ZoneId
import java.time.ZonedDateTime

private data class TicketDraft(
    val type: String,
    val price: String,
    val quantity: String,
    val description: String
)

private data class MemberDraft(
    val name: String,
    val email: String,
    val youtubeUrl: String
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(navController: NavController, eventId: String?) {
    val context = LocalContext.current
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val appSettings = LocalAppSettings.current
    val instance = instanceStore.activeInstance()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startAt by remember { mutableStateOf(ZonedDateTime.now()) }
    var endAt by remember { mutableStateOf(ZonedDateTime.now().plusHours(1)) }
    var venueId by remember { mutableStateOf<String?>(null) }
    var isInPerson by remember { mutableStateOf(true) }
    var isOnline by remember { mutableStateOf(false) }
    var onlineUrl by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var publishState by remember { mutableStateOf(PublishState.draft) }
    var attendeesVisible by remember { mutableStateOf(true) }

    var selectedCategory by remember { mutableStateOf("") }
    var selectedGroupSlug by remember { mutableStateOf("") }

    var ticketsEnabled by remember { mutableStateOf(false) }
    var ticketCurrencyCode by remember { mutableStateOf("") }
    var totalTicketsMode by remember { mutableStateOf("individual") }
    var ticketNotes by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var paymentInstructions by remember { mutableStateOf("") }
    var expireUnpaidTickets by remember { mutableStateOf(false) }
    var remindUnpaidTicketsEvery by remember { mutableStateOf("") }
    var registrationUrl by remember { mutableStateOf("") }
    var eventPassword by remember { mutableStateOf("") }
    var guestListVisibility by remember { mutableStateOf("attendees_only") }
    var schedule by remember { mutableStateOf("") }
    var paymentUrl by remember { mutableStateOf("") }

    var tickets by remember { mutableStateOf(listOf<TicketDraft>()) }
    var members by remember { mutableStateOf(listOf<MemberDraft>()) }

    var venues by remember { mutableStateOf<List<Venue>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var groups by remember { mutableStateOf<List<EventGroup>>(emptyList()) }

    var venueExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var publishExpanded by remember { mutableStateOf(false) }

    var showMediaPicker by remember { mutableStateOf(false) }
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var clearFlyerImage by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(instance?.id, eventId) {
        if (instance == null) return@LaunchedEffect
        isLoading = true
        try {
            val resources = repositories.eventRepository.listEventResources(instance)
            venues = resources.venues.map { Venue(it.id, it.name) }
            categories = resources.categories
            groups = resources.groups

            if (eventId != null) {
                val event = repositories.eventRepository.getEvent(eventId, instance)
                name = event.name
                description = event.description ?: ""
                val tz = ZoneId.of(event.timezone ?: appSettings.timeZoneId)
                startAt = ZonedDateTime.ofInstant(java.time.Instant.parse(event.startAt.toString()), tz)
                endAt = ZonedDateTime.ofInstant(java.time.Instant.parse(event.endAt.toString()), tz)
                venueId = event.venueId
                isInPerson = !event.venueId.isNullOrBlank()
                onlineUrl = event.onlineUrl ?: ""
                isOnline = !event.onlineUrl.isNullOrBlank()
                capacity = event.capacity?.toString() ?: ""
                publishState = event.publishState
                attendeesVisible = event.attendeesVisible ?: true
                selectedCategory = event.category ?: ""
                selectedGroupSlug = event.groupSlug ?: ""
                selectedMediaItem = event.flyerImageId?.toIntOrNull()?.let { id ->
                    MediaItem(id = id, uuid = "", url = event.flyerImageUrl ?: "", originalFilename = "")
                }
                clearFlyerImage = false
            }
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    if (instance == null) return

    if (showMediaPicker) {
        Dialog(onDismissRequest = { showMediaPicker = false }) {
            MediaLibraryPicker(
                instance = instance,
                repository = repositories.mediaLibraryRepository,
                onSelect = {
                    selectedMediaItem = it
                    clearFlyerImage = false
                    showMediaPicker = false
                },
                onCancel = { showMediaPicker = false }
            )
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        item { if (errorMessage != null) Text("Error: $errorMessage") }

        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Button(onClick = {
                val date = startAt
                DatePickerDialog(context, { _, y, m, d ->
                    startAt = startAt.withYear(y).withMonth(m + 1).withDayOfMonth(d)
                }, date.year, date.monthValue - 1, date.dayOfMonth).show()
            }) { Text("Start Date: ${startAt.toLocalDate()}") }
        }

        item {
            Button(onClick = {
                val time = startAt
                TimePickerDialog(context, { _, h, min ->
                    startAt = startAt.withHour(h).withMinute(min)
                }, time.hour, time.minute, false).show()
            }) { Text("Start Time: ${startAt.toLocalTime()}") }
        }

        item {
            Button(onClick = {
                val date = endAt
                DatePickerDialog(context, { _, y, m, d ->
                    endAt = endAt.withYear(y).withMonth(m + 1).withDayOfMonth(d)
                }, date.year, date.monthValue - 1, date.dayOfMonth).show()
            }) { Text("End Date: ${endAt.toLocalDate()}") }
        }

        item {
            Button(onClick = {
                val time = endAt
                TimePickerDialog(context, { _, h, min ->
                    endAt = endAt.withHour(h).withMinute(min)
                }, time.hour, time.minute, false).show()
            }) { Text("End Time: ${endAt.toLocalTime()}") }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("In-Person")
                Switch(checked = isInPerson, onCheckedChange = { isInPerson = it })
                Text("Online")
                Switch(checked = isOnline, onCheckedChange = { isOnline = it })
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = venueExpanded, onExpandedChange = { venueExpanded = !venueExpanded }) {
                val selected = venues.firstOrNull { it.id == venueId }?.name ?: "Select Venue"
                OutlinedTextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Venue") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = venueExpanded) }
                )
                ExposedDropdownMenu(expanded = venueExpanded, onDismissRequest = { venueExpanded = false }) {
                    venues.forEach { venue ->
                        DropdownMenuItem(text = { Text(venue.name) }, onClick = {
                            venueId = venue.id
                            venueExpanded = false
                        })
                    }
                    DropdownMenuItem(text = { Text("Online Only") }, onClick = {
                        venueId = null
                        venueExpanded = false
                    })
                }
            }
        }

        item {
            OutlinedTextField(value = onlineUrl, onValueChange = { onlineUrl = it }, label = { Text("Online URL") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Capacity") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            ExposedDropdownMenuBox(expanded = publishExpanded, onExpandedChange = { publishExpanded = !publishExpanded }) {
                OutlinedTextField(
                    value = publishState.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Publish State") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = publishExpanded) }
                )
                ExposedDropdownMenu(expanded = publishExpanded, onDismissRequest = { publishExpanded = false }) {
                    PublishState.values().forEach { state ->
                        DropdownMenuItem(text = { Text(state.name) }, onClick = {
                            publishState = state
                            publishExpanded = false
                        })
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Attendees Visible")
                Switch(checked = attendeesVisible, onCheckedChange = { attendeesVisible = it })
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                val label = if (selectedCategory.isBlank()) "Select Category" else selectedCategory
                OutlinedTextField(
                    value = label,
                    onValueChange = { selectedCategory = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category) }, onClick = {
                            selectedCategory = category
                            categoryExpanded = false
                        })
                    }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { groupExpanded = !groupExpanded }) {
                val label = if (selectedGroupSlug.isBlank()) "Select Group" else selectedGroupSlug
                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Group") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) }
                )
                ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                    groups.forEach { group ->
                        DropdownMenuItem(text = { Text(group.name) }, onClick = {
                            selectedGroupSlug = group.slug
                            groupExpanded = false
                        })
                    }
                }
            }
        }

        item {
            Text("Flyer Image")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showMediaPicker = true }) { Text("Choose from Media Library") }
                Button(onClick = {
                    selectedMediaItem = null
                    clearFlyerImage = true
                }) { Text("Clear") }
            }
            selectedMediaItem?.let { Text("Selected: ${it.displayName}") }
        }

        item {
            Text("Tickets")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enabled")
                Switch(checked = ticketsEnabled, onCheckedChange = { ticketsEnabled = it })
            }
            OutlinedTextField(value = ticketCurrencyCode, onValueChange = { ticketCurrencyCode = it }, label = { Text("Currency Code") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = totalTicketsMode, onValueChange = { totalTicketsMode = it }, label = { Text("Total Tickets Mode") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ticketNotes, onValueChange = { ticketNotes = it }, label = { Text("Ticket Notes") }, modifier = Modifier.fillMaxWidth())
        }

        items(tickets.size) { index ->
            val draft = tickets[index]
            OutlinedTextField(value = draft.type, onValueChange = {
                val value = it
                tickets = tickets.toMutableList().also { list -> list[index] = list[index].copy(type = value) }
            }, label = { Text("Ticket Type") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = draft.price, onValueChange = {
                val value = it
                tickets = tickets.toMutableList().also { list -> list[index] = list[index].copy(price = value) }
            }, label = { Text("Price") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = draft.quantity, onValueChange = {
                val value = it
                tickets = tickets.toMutableList().also { list -> list[index] = list[index].copy(quantity = value) }
            }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = draft.description, onValueChange = {
                val value = it
                tickets = tickets.toMutableList().also { list -> list[index] = list[index].copy(description = value) }
            }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Button(onClick = { tickets = tickets + TicketDraft("", "", "", "") }) { Text("Add Ticket") }
        }

        item {
            Text("Payment")
            OutlinedTextField(value = paymentMethod, onValueChange = { paymentMethod = it }, label = { Text("Payment Method") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = paymentInstructions, onValueChange = { paymentInstructions = it }, label = { Text("Payment Instructions") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Expire Unpaid")
                Switch(checked = expireUnpaidTickets, onCheckedChange = { expireUnpaidTickets = it })
            }
            OutlinedTextField(value = remindUnpaidTicketsEvery, onValueChange = { remindUnpaidTicketsEvery = it }, label = { Text("Remind Unpaid (minutes)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = paymentUrl, onValueChange = { paymentUrl = it }, label = { Text("Payment URL") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Text("Registration")
            OutlinedTextField(value = registrationUrl, onValueChange = { registrationUrl = it }, label = { Text("Registration URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = eventPassword, onValueChange = { eventPassword = it }, label = { Text("Event Password") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = guestListVisibility, onValueChange = { guestListVisibility = it }, label = { Text("Guest List Visibility") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Schedule") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Text("Members")
            members.forEachIndexed { index, member ->
                OutlinedTextField(value = member.name, onValueChange = {
                    val value = it
                    members = members.toMutableList().also { list -> list[index] = list[index].copy(name = value) }
                }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = member.email, onValueChange = {
                    val value = it
                    members = members.toMutableList().also { list -> list[index] = list[index].copy(email = value) }
                }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = member.youtubeUrl, onValueChange = {
                    val value = it
                    members = members.toMutableList().also { list -> list[index] = list[index].copy(youtubeUrl = value) }
                }, label = { Text("YouTube URL") }, modifier = Modifier.fillMaxWidth())
            }
            Button(onClick = { members = members + MemberDraft("", "", "") }) { Text("Add Member") }
        }

        item {
            Button(onClick = {
                val event = Event(
                    id = eventId ?: java.util.UUID.randomUUID().toString(),
                    name = name,
                    description = description.ifBlank { null },
                    startAt = startAt.toInstant().toKotlinInstant(),
                    endAt = endAt.toInstant().toKotlinInstant(),
                    venueId = if (isInPerson) venueId else null,
                    onlineUrl = if (isOnline) onlineUrl.ifBlank { null } else null,
                    capacity = capacity.toIntOrNull(),
                    publishState = publishState,
                    timezone = appSettings.timeZoneId,
                    attendeesVisible = attendeesVisible,
                    category = selectedCategory.ifBlank { null },
                    groupSlug = selectedGroupSlug.ifBlank { null },
                    flyerImageUrl = selectedMediaItem?.url,
                    flyerImageId = selectedMediaItem?.id?.toString()
                )

                val options = EventRepository.ExtendedEventOptions(
                    categoryName = if (selectedCategory.any { it.isLetter() }) selectedCategory else null,
                    ticketsEnabled = ticketsEnabled,
                    ticketCurrencyCode = ticketCurrencyCode.ifBlank { null },
                    totalTicketsMode = totalTicketsMode.ifBlank { null },
                    ticketNotes = ticketNotes.ifBlank { null },
                    paymentMethod = paymentMethod.ifBlank { null },
                    paymentInstructions = paymentInstructions.ifBlank { null },
                    expireUnpaidTickets = expireUnpaidTickets,
                    remindUnpaidTicketsEvery = remindUnpaidTicketsEvery.toIntOrNull(),
                    registrationUrl = registrationUrl.ifBlank { null },
                    eventPassword = eventPassword.ifBlank { null },
                    flyerImageId = selectedMediaItem?.id?.toString(),
                    clearFlyerImage = clearFlyerImage,
                    guestListVisibility = guestListVisibility.ifBlank { null },
                    schedule = schedule.ifBlank { null },
                    tickets = tickets.mapNotNull { draft ->
                        val price = draft.price.toIntOrNull()
                        val qty = draft.quantity.toIntOrNull()
                        if (price == null || qty == null) null else EventRepository.TicketDTO(
                            type = draft.type.ifBlank { null },
                            price = price,
                            quantity = qty,
                            description = draft.description.ifBlank { null }
                        )
                    },
                    members = members.mapNotNull { member ->
                        if (member.name.isBlank()) null else EventRepository.MemberDTO(
                            name = member.name,
                            email = member.email.ifBlank { null },
                            youtubeUrl = member.youtubeUrl.ifBlank { null }
                        )
                    },
                    paymentUrl = paymentUrl.ifBlank { null }
                )

                isLoading = true
                scope.launch {
                    try {
                        if (eventId == null) {
                            repositories.eventRepository.createEvent(event, instance, null, options)
                        } else {
                            repositories.eventRepository.updateEvent(event, instance, null, options)
                        }
                        navController.popBackStack()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            }) {
                Text(if (eventId == null) "Create Event" else "Save Changes")
            }
        }
    }
}
