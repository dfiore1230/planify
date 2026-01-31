package com.planify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.planify.app.data.model.Talent
import kotlinx.coroutines.launch

@Composable
fun TalentFormScreen(navController: NavController, talentId: Int?) {
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val instance = instanceStore.activeInstance()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(instance?.id, talentId) {
        if (instance == null || talentId == null) return@LaunchedEffect
        isLoading = true
        try {
            val talent = repositories.talentRepository.fetch(talentId, instance)
            name = talent.name
            description = talent.description ?: ""
            email = talent.email ?: ""
            phone = talent.phone ?: ""
            website = talent.website ?: ""
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
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            Button(onClick = {
                val talent = Talent(
                    id = talentId ?: 0,
                    name = name,
                    description = description.ifBlank { null },
                    email = email.ifBlank { null },
                    phone = phone.ifBlank { null },
                    website = website.ifBlank { null }
                )
                scope.launch {
                    isLoading = true
                    try {
                        if (talentId == null) {
                            repositories.talentRepository.create(talent, instance)
                        } else {
                            repositories.talentRepository.update(talent, instance)
                        }
                        navController.popBackStack()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            }) {
                Text(if (talentId == null) "Create Talent" else "Save Changes")
            }
        }
    }
}
