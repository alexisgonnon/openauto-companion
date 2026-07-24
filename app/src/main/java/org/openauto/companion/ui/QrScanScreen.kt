package org.openauto.companion.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.util.Size
import org.openauto.companion.net.PairingUriParser
import org.openauto.companion.net.PairingPayload
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "QrScanScreen"

@Composable
fun QrScanScreen(
    onScanned: (PairingPayload) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 22.dp),
    ) {
        CockpitTopBar(title = "Scan pairing code", onBack = onCancel)
        Spacer(Modifier.height(8.dp))
        Text(
            "Point your camera at the QR code on the head unit.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        when {
            hasCameraPermission -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large),
                ) {
                    CameraPreviewWithScanner(
                        onScanned = onScanned,
                        onCameraError = { cameraError = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                    ScanFrameOverlay(Modifier.fillMaxSize())
                }
                cameraError?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    CockpitCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text("Camera unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            permissionDenied -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CockpitCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Camera access needed", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Camera permission is required to scan pairing codes.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        CockpitTextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                            Text("Go back")
                        }
                    }
                }
            }
            else -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CockpitCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Requesting camera permission…", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ScanFrameOverlay(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val inset = size.minDimension * 0.08f
        val length = size.minDimension * 0.14f
        val stroke = size.minDimension * 0.012f
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset
        fun line(start: Offset, end: Offset) = drawLine(color, start, end, stroke, StrokeCap.Round)
        line(Offset(left, top + length), Offset(left, top)); line(Offset(left, top), Offset(left + length, top))
        line(Offset(right - length, top), Offset(right, top)); line(Offset(right, top), Offset(right, top + length))
        line(Offset(left, bottom - length), Offset(left, bottom)); line(Offset(left, bottom), Offset(left + length, bottom))
        line(Offset(right - length, bottom), Offset(right, bottom)); line(Offset(right, bottom), Offset(right, bottom - length))
    }
}

@Composable
private fun CameraPreviewWithScanner(
    onScanned: (PairingPayload) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanComplete = remember { AtomicBoolean(false) }
    val disposed = remember { AtomicBoolean(false) }
    val cameraProvider = remember { AtomicReference<ProcessCameraProvider?>() }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            disposed.set(true)
            cameraProvider.get()?.unbindAll()
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val provider = try {
                    cameraProviderFuture.get()
                } catch (error: Exception) {
                    Log.e(TAG, "Camera provider failed", error)
                    onCameraError("Unable to start the camera.")
                    return@addListener
                }
                cameraProvider.set(provider)
                if (disposed.get()) {
                    provider.unbindAll()
                    return@addListener
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(1280, 720))
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (scanComplete.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    processFrame(imageProxy, scanner) { payload ->
                        if (scanComplete.compareAndSet(false, true)) {
                            onScanned(payload)
                        }
                    }
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Camera bind failed", e)
                    onCameraError("Unable to start the camera.")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private var frameCount = 0L

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processFrame(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onResult: (PairingPayload) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    frameCount++
    if (frameCount % 30 == 0L) {
        Log.d(TAG, "Analyzed $frameCount frames, rotation=${imageProxy.imageInfo.rotationDegrees}, " +
                "size=${mediaImage.width}x${mediaImage.height}")
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
                Log.i(TAG, "ML Kit found ${barcodes.size} barcode(s)")
            }
            for (barcode in barcodes) {
                val rawValue = barcode.rawValue
                if (rawValue == null) continue
                val payload = PairingUriParser.parse(rawValue)
                if (payload != null) {
                    Log.i(TAG, "Valid Prodigy pairing QR scanned")
                    onResult(payload)
                    break
                }
                Log.w(TAG, "Ignoring QR code that is not a valid Prodigy pairing payload")
            }
        }
        .addOnFailureListener { e ->
            Log.w(TAG, "Barcode scan failed", e)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
