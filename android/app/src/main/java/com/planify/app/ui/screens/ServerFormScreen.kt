package com.planify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.menuAnchor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.planify.app.LocalRepositories
import com.planify.app.LocalInstanceStore
import com.planify.app.data.model.AuthMethod
import com.planify.app.data.model.InstanceEnvironment
import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.themeFromBranding
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ServerFormScreen(navController: NavController, serverId: String?) {
    val instanceStore = LocalInstanceStore.current
    val repositories = LocalRepositories.current
    val existing = instanceStore.instances.value.firstOrNull { it.id == serverId }

    var displayName by remember { mutableStateOf(existing?.displayName ?: "") }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "") }
    var environment by remember { mutableStateOf(existing?.environment ?: InstanceEnvironment.prod) }
    var authMethod by remember { mutableStateOf(existing?.authMethod ?: AuthMethod.sanctum) }

    var envExpanded by remember { mutableStateOf(false) }
    var authExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            ExposedDropdownMenuBox(expanded = envExpanded, onExpandedChange = { envExpanded = !envExpanded }) {
                OutlinedTextField(
                    value = environment.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Environment") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = envExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = envExpanded, onDismissRequest = { envExpanded = false }) {
                    InstanceEnvironment.values().forEach { env ->
                        DropdownMenuItem(text = { Text(env.name) }, onClick = {
                            environment = env
                            envExpanded = false
                        })
                    }
                }
            }
        }
        item {
            ExposedDropdownMenuBox(expanded = authExpanded, onExpandedChange = { authExpanded = !authExpanded }) {
                OutlinedTextField(
                    value = authMethod.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Auth Method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = authExpanded, onDismissRequest = { authExpanded = false }) {
                    AuthMethod.values().forEach { method ->
                        DropdownMenuItem(text = { Text(method.name) }, onClick = {
                            authMethod = method
                            authExpanded = false
                        })
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.popBackStack() }) { Text("Cancel") }
                Button(onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        val trimmedBase = baseUrl.trim()
                        val name = displayName.trim().ifBlank { trimmedBase }
                        var instance = InstanceProfile(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            displayName = name,
                            baseUrl = trimmedBase,
                            environment = environment,
                            authMethod = authMethod,
                            authEndpoints = existing?.authEndpoints,
                            featureFlags = existing?.featureFlags ?: emptyMap(),
                            minAppVersion = existing?.minAppVersion,
                            rateLimits = existing?.rateLimits,
                            tokenIdentifier = existing?.tokenIdentifier,
                            theme = existing?.theme
                        )

                        try {
                            val caps = repositories.discoveryService.fetchCapabilities(trimmedBase)
                            val branding = repositories.brandingService.fetchBranding(caps)
                            instance = instance.copy(
                                baseUrl = caps.apiBaseUrl,
                                authMethod = when (caps.auth.type) {
                                    com.planify.app.data.model.CapabilitiesDocument.AuthType.sanctum -> AuthMethod.sanctum
                                    com.planify.app.data.model.CapabilitiesDocument.AuthType.passport -> AuthMethod.oauth2
                                    com.planify.app.data.model.CapabilitiesDocument.AuthType.jwt -> AuthMethod.jwt
                                },
                                authEndpoints = caps.auth.endpoints,
                                featureFlags = caps.features,
                                minAppVersion = caps.minAppVersion,
                                rateLimits = caps.rateLimits,
                                theme = themeFromBranding(branding)
                            )
                        } catch (_: Exception) {
                            // Keep manually entered values if discovery fails
                        }

                        if (existing != null) {
                            instanceStore.removeInstance(existing.id)
                        }
                        instanceStore.addInstance(instance)
                        isSaving = false
                        navController.popBackStack()
                    }
                }) { Text(if (isSaving) "Saving..." else if (existing == null) "Add" else "Save") }
            }
        }
    }
}
