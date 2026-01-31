package com.planify.app.ui.components

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QrCodeAnalyzer(private val onCode: (String) -> Unit) {
    private val executor = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private var lastScanMs: Long = 0

    val analysisUseCase: ImageAnalysis = ImageAnalysis.Builder().build().apply {
        setAnalyzer(executor) { imageProxy -> analyze(imageProxy) }
    }

    private fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstOrNull()?.rawValue
                if (!code.isNullOrBlank()) {
                    val now = System.currentTimeMillis()
                    if (now - lastScanMs > 2000) {
                        lastScanMs = now
                        onCode(code)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
