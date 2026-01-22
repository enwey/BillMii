package com.billmii.android.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billmii.android.data.service.CameraManager
import com.billmii.android.ui.theme.BillMiiTheme
import java.io.File

/**
 * Camera Activity - 相机界面
 * Handles camera capture for receipts
 */
class CameraActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BillMiiTheme {
                CameraScreen(
                    onBackClick = { finish() },
                    onPhotoCaptured = { file ->
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_IMAGE_PATH, file.absolutePath)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

/**
 * Camera View Model
 */
class CameraViewModel(
    private val cameraManager: CameraManager? = null
) : androidx.lifecycle.ViewModel() {
    
    private val _isCapturing = androidx.lifecycle.MutableStateFlow(false)
    val isCapturing = _isCapturing.asStateFlow()
    
    fun capturePhoto(onPhotoCaptured: (File) -> Unit) {
        // TODO: Implement photo capture
    }
}

/**
 * Camera Screen - 相机界面Compose
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBackClick: () -> Unit,
    onPhotoCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }
    var isCameraInitialized by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    
    // Camera permission launcher
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Initialize camera
        }
    }
    
    // Check and request camera permission
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            cameraManager = CameraManager(context, lifecycleOwner)
            cameraManager?.initializeCamera()
            isCameraInitialized = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍摄票据") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        cameraManager?.switchCamera()
                    }) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "切换相机")
                    }
                    IconButton(onClick = {
                        // TODO: Toggle flash
                    }) {
                        Icon(Icons.Default.FlashAuto, contentDescription = "闪光灯")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isCameraInitialized && cameraManager != null) {
                CameraPreview(
                    cameraManager = cameraManager!!,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Capture button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    CaptureButton(
                        isCapturing = isCapturing,
                        onClick = {
                            isCapturing = true
                            
                            // Capture photo
                            val photoFile = File(context.getExternalFilesDir(null), "receipt_${System.currentTimeMillis()}.jpg")
                            
                            cameraManager?.capturePhoto(photoFile) { success ->
                                isCapturing = false
                                if (success) {
                                    capturedImagePath = photoFile.absolutePath
                                    onPhotoCaptured(photoFile)
                                } else {
                                    Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在初始化相机...")
                    }
                }
            }
        }
    }
}

/**
 * Camera Preview
 */
@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { context ->
            androidx.camera.view.PreviewView(context).apply {
                this.scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                cameraManager.startCamera(this) {
                    // Preview ready
                }
            }
        },
        modifier = modifier,
        update = { previewView ->
            // Update preview view if needed
        }
    )
}

/**
 * Capture Button
 */
@Composable
fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "拍照",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}