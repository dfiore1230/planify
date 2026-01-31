package com.planify.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.planify.app.LocalInstanceStore
import com.planify.app.LocalRepositories
import com.planify.app.data.model.TicketSale

@Composable
fun TicketListScreen(navController: NavController) {
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val instance = instanceStore.activeInstance()

    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<TicketSale>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(instance?.id, query) {
        if (instance == null) return@LaunchedEffect
        isLoading = true
        try {
            items = repositories.ticketRepository.search(null, query.ifBlank { null }, instance)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search tickets") },
            modifier = Modifier.fillMaxWidth()
        )

        if (isLoading) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items) { sale ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("tickets/detail/${sale.id}") }
                        .padding(16.dp)
                ) {
                    Text("${sale.name} (${sale.displayStatus})", style = MaterialTheme.typography.titleMedium)
                    Text("${sale.email}")
                }
            }
        }
    }
}
