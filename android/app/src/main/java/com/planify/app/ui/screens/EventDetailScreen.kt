package com.planify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.planify.app.LocalAppSettings
import com.planify.app.LocalInstanceStore
import com.planify.app.LocalRepositories
import com.planify.app.data.model.Event
import com.planify.app.util.DateFormatters

@Composable
fun EventDetailScreen(navController: NavController, eventId: String) {
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val appSettings = LocalAppSettings.current
    val instance = instanceStore.activeInstance()

    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(instance?.id, eventId) {
        if (instance == null) return@LaunchedEffect
        isLoading = true
        try {
            event = repositories.eventRepository.getEvent(eventId, instance)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }

        if (errorMessage != null) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        }

        val eventValue = event
        if (eventValue != null) {
            Text(eventValue.name, style = MaterialTheme.typography.headlineSmall)
            Text(DateFormatters.formatDisplay(eventValue.startAt, appSettings.timeZoneId))
            Text("Venue: ${eventValue.venueDisplayDescription}")
            eventValue.description?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { navController.navigate("events/edit/${eventValue.id}") }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    Text("Edit", modifier = Modifier.padding(start = 8.dp))
                }
                Button(onClick = { navController.navigate("scanner/${eventValue.id}") }) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan")
                    Text("Scan", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
