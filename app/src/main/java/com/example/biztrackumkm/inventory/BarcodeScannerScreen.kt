package com.example.biztrackumkm.inventory

import android.Manifest
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.biztrackumkm.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarcodeScannerScreen(onLookup: suspend (String) -> Unit, onClose: () -> Unit) {
    var captured by rememberSaveable { mutableStateOf<String?>(null) }
    var lookupFailed by remember { mutableStateOf(false) }

    LaunchedEffect(captured) {
        val code = captured ?: return@LaunchedEffect
        try {
            onLookup(code)
            onClose()
        } catch (error: SQLiteException) {
            Log.e("Inventory", "Gagal mencari barcode hasil pindai", error)
            lookupFailed = true
        }
    }
    BackHandler(onBack = onClose)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.barcode_scan_title)) },
            navigationIcon = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.inventory_back)) }
            },
        )
    }) { insets ->
        Column(
            Modifier.padding(insets).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when {
                    lookupFailed -> ScannerStatus(R.string.inventory_search_error)
                    captured != null -> ScannerStatus(R.string.barcode_scan_lookup, loading = true)
                    else -> BarcodePreview(
                        modifier = Modifier.fillMaxSize(),
                        onFrame = { code -> if (captured == null && code != null) captured = code },
                    )
                }
            }
            if (lookupFailed) {
                Button(
                    onClick = {
                        lookupFailed = false
                        captured = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.barcode_scan_retry))
                }
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.barcode_scan_manual))
            }
        }
    }
}

/** Shared live preview. Null is a frame without a decoded barcode; ambiguous frames are ignored. */
@Composable
internal fun BarcodePreview(modifier: Modifier, onFrame: (String?) -> Unit) {
    val context = LocalContext.current
    val hasRearCamera = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }
    fun cameraGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(cameraGranted()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var requesting by rememberSaveable { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        requesting = false
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = cameraGranted() }
    LaunchedEffect(hasRearCamera, hasPermission) {
        if (hasRearCamera && !hasPermission && !permissionRequested) {
            permissionRequested = true
            requesting = true
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            !hasRearCamera || cameraFailed -> ScannerStatus(R.string.barcode_scan_unavailable)
            !hasPermission && (requesting || !permissionRequested) ->
                ScannerStatus(R.string.barcode_scan_wait_permission, loading = true)
            !hasPermission -> ScannerStatus(R.string.barcode_scan_permission_denied)
            else -> {
                BarcodeCameraPreview(onFrame, onError = { cameraFailed = true })
                ScanGuide(Modifier.fillMaxSize())
                Text(
                    text = stringResource(R.string.barcode_scan_hint),
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.72f)).padding(12.dp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ScannerStatus(message: Int, loading: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) CircularProgressIndicator()
        Text(stringResource(message))
    }
}

@Composable
private fun ScanGuide(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val left = size.width * 0.1f
        val right = size.width * 0.9f
        val top = size.height * 0.28f
        val bottom = size.height * 0.67f
        val length = minOf(20.dp.toPx(), (bottom - top) / 2)
        val stroke = 3.dp.toPx()
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            val anchor = Offset(x, y)
            drawLine(color, anchor, Offset(x + dx, y), stroke)
            drawLine(color, anchor, Offset(x, y + dy), stroke)
        }
        corner(left, top, length, length)
        corner(right, top, -length, length)
        corner(left, bottom, length, -length)
        corner(right, bottom, -length, -length)
    }
}

@Composable
private fun BarcodeCameraPreview(onFrame: (String?) -> Unit, onError: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val emitFrame by rememberUpdatedState(onFrame)
    val reportError by rememberUpdatedState(onError)
    val previewView = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(previewView, owner) {
        val main = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            ).build(),
        )
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val provider = ProcessCameraProvider.getInstance(context.applicationContext)
        var boundProvider: ProcessCameraProvider? = null
        var cameraInfo: CameraInfo? = null
        // Camera state, detector results, and disposal all run on the main thread.
        var accepting = true
        fun fail() {
            if (!accepting) return
            accepting = false
            reportError()
        }
        val observer = Observer<CameraState> { state ->
            if (state.error != null) fail()
        }
        analysis.setAnalyzer(main, MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, main) { result ->
            if (!accepting || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@MlKitAnalyzer
            result.getThrowable(scanner)?.let { error ->
                Log.w("Inventory", "Pemindai tidak dapat membaca frame kamera", error)
                fail()
                return@MlKitAnalyzer
            }
            val barcodes = result.getValue(scanner) ?: return@MlKitAnalyzer
            if (barcodes.size <= 1) {
                emitFrame(barcodes.singleOrNull()?.rawValue?.takeUnless(String::isBlank))
            }
        })
        provider.addListener({
            if (accepting) {
                try {
                    val cameraProvider = provider.get()
                    val camera = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    boundProvider = cameraProvider
                    cameraInfo = camera.cameraInfo.also { it.cameraState.observe(owner, observer) }
                } catch (error: Exception) {
                    Log.w("Inventory", "Gagal membuka kamera belakang", error)
                    fail()
                }
            }
        }, main)

        onDispose {
            accepting = false
            cameraInfo?.cameraState?.removeObserver(observer)
            analysis.clearAnalyzer()
            boundProvider?.unbind(preview, analysis)
            scanner.close()
        }
    }
}
