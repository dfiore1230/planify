package com.planify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InstanceOnboardingScreen(onAddServer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No server configured", style = MaterialTheme.typography.headlineSmall)
        Text("Add a Planify server to get started.", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onAddServer, modifier = Modifier.padding(top = 16.dp)) {
            Text("Add Server")
        }
    }
}
