package com.planify.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import coil.compose.AsyncImage
import com.planify.app.data.model.InstanceProfile
import com.planify.app.data.model.MediaItem
import com.planify.app.data.repository.MediaLibraryRepository
import com.planify.app.util.buildAbsoluteUrl

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaLibraryPicker(
    instance: InstanceProfile,
    repository: MediaLibraryRepository,
    onSelect: (MediaItem) -> Unit,
    onCancel: () -> Unit
) {
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(instance.id) {
        isLoading = true
        errorMessage = null
        try {
            mediaItems = repository.fetchAllMedia(instance)
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Media Library", style = MaterialTheme.typography.titleLarge)

        if (isLoading && mediaItems.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (errorMessage != null) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        } else if (mediaItems.isEmpty()) {
            Text("No images in media library")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(mediaItems) { item ->
                    val url = buildAbsoluteUrl(instance.baseUrl, item.url)
                    AsyncImage(
                        model = url,
                        contentDescription = item.displayName,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { selectedId = item.id }
                    )
                }
            }
        }

        Button(onClick = onCancel, modifier = Modifier.padding(top = 12.dp)) { Text("Cancel") }
        Button(
            onClick = {
                val selected = mediaItems.firstOrNull { it.id == selectedId }
                if (selected != null) onSelect(selected)
            },
            enabled = selectedId != null,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Select") }
    }
}
