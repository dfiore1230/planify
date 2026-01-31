package com.planify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.planify.app.data.model.TicketSale
import kotlinx.coroutines.launch

@Composable
fun TicketDetailScreen(navController: NavController, ticketId: Int) {
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val instance = instanceStore.activeInstance()

    var sale by remember { mutableStateOf<TicketSale?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var reassignName by remember { mutableStateOf("") }
    var reassignEmail by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    suspend fun reload() {
        if (instance == null) return
        isLoading = true
        try {
            sale = repositories.ticketRepository.fetch(ticketId, instance)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(instance?.id, ticketId) { reload() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }

        if (errorMessage != null) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        }

        val saleValue = sale
        if (saleValue != null) {
            Text("${saleValue.name} (${saleValue.displayStatus})", style = MaterialTheme.typography.headlineSmall)
            Text(saleValue.email, modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { repositories.ticketRepository.markAsPaid(ticketId, instance); reload() } }) { Text("Mark Paid") }
                Button(onClick = { scope.launch { repositories.ticketRepository.markAsUnpaid(ticketId, instance); reload() } }) { Text("Mark Unpaid") }
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { repositories.ticketRepository.refund(ticketId, instance); reload() } }) { Text("Refund") }
                Button(onClick = { scope.launch { repositories.ticketRepository.cancel(ticketId, instance); reload() } }) { Text("Cancel") }
            }

            Text("Reassign", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(value = reassignName, onValueChange = { reassignName = it }, label = { Text("New Holder") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = reassignEmail, onValueChange = { reassignEmail = it }, label = { Text("New Email") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    repositories.ticketRepository.reassign(ticketId, reassignName, reassignEmail, instance)
                    reload()
                }
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Reassign") }

            Text("Add Note", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                scope.launch {
                    repositories.ticketRepository.addNote(ticketId, note, instance)
                    note = ""
                    reload()
                }
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Save Note") }
        }
    }
}
