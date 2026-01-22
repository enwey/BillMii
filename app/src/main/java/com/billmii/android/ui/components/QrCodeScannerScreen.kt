package com.billmii.android.ui.components

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.billmii.android.data.service.QrCodeScannerService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * QR Code Scanner Screen
 * Provides camera-based QR code and barcode scanning
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScannerScreen(
    onBack: () -> Unit,
    onCodeDetected: (String, com.billmii.android.data.service.QrCodeScannerService.ReceiptQrData?) -> Unit,
    qrCodeScannerService: QrCodeScannerService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageAnalyzer by remember { mutableStateOf<ImageAnalysis?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var codeDetected by remember { mutableStateOf(false) }
    var detectedCode by remember { mutableStateOf<String?>(null) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    // Check camera permission
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    // Initialize camera
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                cameraProvider = providerFuture.get()
            }, ContextCompat.getMainExecutor(context))
        }
    }
    
    // Setup camera when provider is ready
    LaunchedEffect(cameraProvider) {
        cameraProvider?.let { provider ->
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        // Will be set in AndroidView
                    )
                }
            
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        scope.launch {
                            val result = qrCodeScannerService.scanImage(imageProxy)
                            if (result is QrCodeScannerService.ScanResult.Success && 
                                result.barcode?.rawValue != null &&
                                !codeDetected) {
                                codeDetected = true
                                detectedCode = result.barcode!!.rawValue
                                val qrData = qrCodeScannerService.parseReceiptData(result.barcode!!.rawValue)
                                onCodeDetected(result.barcode!!.rawValue, qrData)
                            }
                        }
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }
    }
    
    // Reset detection state when navigating back
    DisposableEffect(Unit) {
        onDispose {
            qrCodeScannerService.reset()
            cameraExecutor.shutdown()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("扫码") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isFlashOn = !isFlashOn
                        // Toggle flash (implementation depends on CameraX setup)
                    }) {
                        Icon(
                            if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "闪光灯"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                // Show permission request UI
                PermissionRequestView()
            } else {
                // Camera preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    update = { previewView ->
                        preview?.setSurfaceProvider(previewView.surfaceProvider)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Scanning overlay
                ScanningOverlay(
                    modifier = Modifier.fillMaxSize()
                )
                
                // Detected code display
                if (codeDetected && detectedCode != null) {
                    DetectedCodeCard(
                        code = detectedCode!!,
                        onScanAgain = {
                            codeDetected = false
                            detectedCode = null
                            qrCodeScannerService.reset()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Scanning overlay with corner brackets
 */
@Composable
fun ScanningOverlay(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Center scanning frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
        )
        
        // Corner decorations
        val cornerSize = 24.dp
        val cornerThickness = 4.dp
        
        // Top left corner
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-140).dp, y = (-140).dp)
                .size(cornerSize)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(cornerThickness)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Top right corner
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 116.dp, y = (-140).dp)
                .size(cornerSize)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(cornerThickness)
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Bottom left corner
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-140).dp, y = 116.dp)
                .size(cornerSize)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(cornerThickness)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Bottom right corner
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 116.dp, y = 116.dp)
                .size(cornerSize)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(cornerThickness)
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerThickness)
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Scanning line animation
        var scanLinePosition by remember { mutableStateOf(0f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                for (i in 0..100) {
                    scanLinePosition = i / 100f
                    kotlinx.coroutines.delay(20)
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-140 + 280 * scanLinePosition).dp)
                .fillMaxWidth(0.6f)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        )
        
        // Instruction text
        Text(
            text = "将二维码放入框内即可自动扫描",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

/**
 * Detected code card
 */
@Composable
fun DetectedCodeCard(
    code: String,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "扫描成功",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = code,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            
            Button(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("继续扫描")
            }
        }
    }
}

/**
 * Permission request view
 */
@Composable
fun PermissionRequestView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "需要相机权限",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "请授予相机权限以使用扫码功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = {
                // Request permission (would need to implement permission launcher)
            }) {
                Text("请求权限")
            }
        }
    }
}