package com.planify.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.planify.app.LocalInstanceStore
import com.planify.app.LocalRepositories
import com.planify.app.ui.components.QrCodeAnalyzer
import kotlinx.coroutines.launch
import com.planify.app.data.model.ScanResult
import com.planify.app.data.model.ScanStatus

@Composable
fun QRScannerScreen(navController: NavController, eventId: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repositories = LocalRepositories.current
    val instance = LocalInstanceStore.current.activeInstance()
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf<ScanResult?>(null) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission.value = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission.value) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission.value) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera permission required", modifier = Modifier.padding(24.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Grant Permission") }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analyzer = QrCodeAnalyzer { code ->
                if (instance != null) {
                    scope.launch {
                        lastResult = repositories.checkInRepository.scanTicket(code, eventId, null, null, instance)
                    }
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer.analysisUseCase)
            previewView
        }, modifier = Modifier.fillMaxSize())

        Text(
            "Align the QR code within the frame",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )

        lastResult?.let { result ->
            val color = when (result.status) {
                ScanStatus.admitted -> MaterialTheme.colorScheme.primary
                ScanStatus.invalid, ScanStatus.refunded, ScanStatus.cancelled, ScanStatus.expired -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.secondary
            }
            Text(
                result.message ?: result.status.displayName,
                color = color,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
            )
        }
    }
}
