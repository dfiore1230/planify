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
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.planify.app.LocalApiKeyStore
import com.planify.app.LocalAppSettings
import com.planify.app.LocalInstanceStore

@Composable
fun SettingsScreen(navController: NavController) {
    val instanceStore = LocalInstanceStore.current
    val appSettings = LocalAppSettings.current
    val apiKeyStore = LocalApiKeyStore.current

    val instances by instanceStore.instances.collectAsState()
    val activeId by instanceStore.activeInstanceId.collectAsState()

    val activeInstance = instanceStore.activeInstance()

    var apiKey by remember { mutableStateOf("") }
    var timeZoneId by remember { mutableStateOf(appSettings.timeZoneId) }

    LaunchedEffect(activeInstance?.id) {
        apiKey = activeInstance?.let { apiKeyStore.load(it) } ?: ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Servers", style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(instances) { instance ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { instanceStore.setActiveInstance(instance.id) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(instance.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(instance.baseUrl, style = MaterialTheme.typography.bodySmall)
                    if (activeId == instance.id) {
                        Text("Active", color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { navController.navigate("settings/edit-server/${instance.id}") }) { Text("Edit") }
                        Button(onClick = { instanceStore.removeInstance(instance.id) }) { Text("Delete") }
                    }
                }
                Divider()
            }
        }

        Button(onClick = { navController.navigate("settings/add-server") }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Server")
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text("API Key", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (activeInstance != null && apiKey.isNotBlank()) {
                    apiKeyStore.save(apiKey, activeInstance)
                }
            }) { Text("Save") }
            Button(onClick = {
                if (activeInstance != null) {
                    apiKeyStore.clear(activeInstance)
                }
            }) { Text("Remove") }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text("Localization", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = timeZoneId, onValueChange = { timeZoneId = it }, label = { Text("Time Zone") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                appSettings.timeZoneId = timeZoneId
            }) { Text("Save Time Zone") }
            Button(onClick = {
                appSettings.resetTimeZoneToCurrent()
                timeZoneId = appSettings.timeZoneId
            }) { Text("Reset to Device") }
        }
    }
}
