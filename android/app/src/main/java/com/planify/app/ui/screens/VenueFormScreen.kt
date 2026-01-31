package com.planify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.planify.app.LocalInstanceStore
import com.planify.app.LocalRepositories
import com.planify.app.data.model.VenueDetail
import kotlinx.coroutines.launch

@Composable
fun VenueFormScreen(navController: NavController, venueId: Int?) {
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val instance = instanceStore.activeInstance()

    var name by remember { mutableStateOf("") }
    var address1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(instance?.id, venueId) {
        if (instance == null || venueId == null) return@LaunchedEffect
        isLoading = true
        try {
            val venue = repositories.venueRepository.fetch(venueId, instance)
            name = venue.name
            address1 = venue.address1 ?: ""
            city = venue.city ?: ""
            state = venue.state ?: ""
            postalCode = venue.postalCode ?: ""
            countryCode = venue.countryCode ?: ""
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    if (instance == null) return

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (isLoading) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        item { errorMessage?.let { Text("Error: $it") } }

        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = address1, onValueChange = { address1 = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = postalCode, onValueChange = { postalCode = it }, label = { Text("Postal Code") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = countryCode, onValueChange = { countryCode = it }, label = { Text("Country Code") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Button(onClick = {
                val venue = VenueDetail(
                    id = venueId ?: 0,
                    name = name,
                    address1 = address1.ifBlank { null },
                    city = city.ifBlank { null },
                    state = state.ifBlank { null },
                    postalCode = postalCode.ifBlank { null },
                    countryCode = countryCode.ifBlank { null }
                )
                scope.launch {
                    isLoading = true
                    try {
                        if (venueId == null) {
                            repositories.venueRepository.create(venue, instance)
                        } else {
                            repositories.venueRepository.update(venue, instance)
                        }
                        navController.popBackStack()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            }) {
                Text(if (venueId == null) "Create Venue" else "Save Changes")
            }
        }
    }
}
