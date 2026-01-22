package com.billmii.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.billmii.android.BillMiiApplication
import com.billmii.android.data.model.ReceiptType
import com.billmii.android.data.repository.ReceiptRepository
import com.billmii.android.ui.camera.CameraActivity
import com.billmii.android.ui.navigation.Screen
import com.billmii.android.ui.advancedsearch.AdvancedSearchScreen
import com.billmii.android.ui.batchoperations.BatchOperationsScreen
import com.billmii.android.ui.export.ExportScreen
import com.billmii.android.ui.ocrtemplate.OcrTemplateScreen
import com.billmii.android.ui.receipt.*
import com.billmii.android.ui.settings.LanIntegrationScreen
import com.billmii.android.ui.settings.BiometricSettingsScreen
import com.billmii.android.ui.reimbursement.*
import com.billmii.android.ui.settings.*
import com.billmii.android.ui.components.QrCodeScannerScreen
import com.billmii.android.data.service.ComplianceValidationService
import com.billmii.android.data.service.VoiceInputService
import com.billmii.android.ui.statistics.StatisticsScreen
import com.billmii.android.ui.theme.BillMiiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Main Activity - 主界面
 * Entry point of the application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var receiptRepository: ReceiptRepository

    @Inject
    lateinit var voiceInputService: VoiceInputService

    @Inject
    lateinit var qrCodeScannerService: QrCodeScannerService

    private val viewModel: MainViewModel by viewModels()

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "需要相机和存储权限", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera activity result
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_PATH)?.let { imagePath ->
                viewModel.importReceiptFromCamera(imagePath)
                Toast.makeText(this, "照片已保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        checkAndRequestPermissions()

        setContent {
            BillMiiTheme {
                MainScreen(
                    onCameraClick = ::startCamera,
                    onFilesImported = ::handleFilesImported,
                    voiceInputService = voiceInputService,
                    qrCodeScannerService = qrCodeScannerService
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun handleFilesImported(uris: List<Uri>) {
        viewModel.importReceiptsFromFiles(uris, contentResolver, this)
    }
}

/**
 * Main View Model
 */
class MainViewModel(
    private val receiptRepository: ReceiptRepository
) : androidx.lifecycle.ViewModel() {

    private val _importProgress = androidx.lifecycle.MutableStateFlow(0)
    val importProgress = _importProgress.asStateFlow()

    fun importReceiptFromCamera(imagePath: String) {
        viewModelScope.launch {
            try {
                val file = File(imagePath)
                receiptRepository.importReceipt(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importReceiptsFromFiles(
        uris: List<Uri>,
        contentResolver: android.content.ContentResolver,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            try {
                uris.forEachIndexed { index, uri ->
                    val inputStream = contentResolver.openInputStream(uri)
                    inputStream?.use { input ->
                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        receiptRepository.importReceipt(tempFile)
                        _importProgress.value = ((index + 1) * 100) / uris.size
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * Main Screen - 主界面布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onCameraClick: () -> Unit,
    onFilesImported: (List<Uri>) -> Unit,
    voiceInputService: VoiceInputService,
    qrCodeScannerService: QrCodeScannerService
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        Screen.ReceiptList,
        Screen.ReimbursementList,
        Screen.Statistics,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { 
                            when (screen) {
                                Screen.ReceiptList -> {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.Receipt else Icons.Outlined.Receipt,
                                        contentDescription = screen.title
                                    )
                                }
                                Screen.ReimbursementList -> {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.Description else Icons.Outlined.Description,
                                        contentDescription = screen.title
                                    )
                                }
                                Screen.Statistics -> {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                        contentDescription = screen.title
                                    )
                                }
                                Screen.Settings -> {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = screen.title
                                    )
                                }
                                else -> {}
                            }
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.ReceiptList.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Receipt List Screen
            composable(Screen.ReceiptList.route) {
                ReceiptListScreen(
                    paddingValues = paddingValues,
                    onReceiptClick = { receiptId ->
                        navController.navigate(Screen.ReceiptDetail.createRoute(receiptId))
                    },
                    onCameraClick = onCameraClick,
                    onFilesImported = onFilesImported,
                    onBatchOperationsClick = {
                        navController.navigate(Screen.BatchOperations.route)
                    },
                    onAdvancedSearchClick = {
                        navController.navigate(Screen.AdvancedSearch.route)
                    },
                    onQrScanClick = {
                        navController.navigate(Screen.QrCodeScanner.route)
                    }
                )
            }

            // Receipt Detail Screen
            composable(
                route = Screen.ReceiptDetail.route,
                arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: return@composable
                ReceiptDetailScreen(
                    receiptId = receiptId,
                    onBack = { navController.navigateUp() }
                )
            }

            // Reimbursement List Screen
            composable(Screen.ReimbursementList.route) {
                ReimbursementListScreen(
                    paddingValues = paddingValues,
                    onReimbursementClick = { reimbursementId ->
                        navController.navigate(Screen.ReimbursementDetail.createRoute(reimbursementId))
                    },
                    onCreateReimbursementClick = {
                        navController.navigate(Screen.CreateReimbursement.route)
                    },
                    onApprovalWorkflowClick = {
                        navController.navigate(Screen.ApprovalWorkflow.route)
                    }
                )
            }

            // Reimbursement Detail Screen
            composable(
                route = Screen.ReimbursementDetail.route,
                arguments = listOf(navArgument("reimbursementId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reimbursementId = backStackEntry.arguments?.getLong("reimbursementId") ?: return@composable
                ReimbursementDetailScreen(
                    reimbursementId = reimbursementId,
                    onBack = { navController.navigateUp() }
                )
            }

            // Statistics Screen
            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    paddingValues = paddingValues,
                    onExportClick = {
                        navController.navigate(Screen.Export.route)
                    }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    paddingValues = paddingValues,
                    onBackupRestoreClick = {
                        navController.navigate(Screen.BackupRestore.route)
                    },
                    onArchivePathManagementClick = {
                        navController.navigate(Screen.ArchivePathManagement.route)
                    },
                    onOperationLogClick = {
                        navController.navigate(Screen.OperationLog.route)
                    },
                    onOcrTemplatesClick = {
                        navController.navigate(Screen.OcrTemplates.route)
                    },
                    onClassificationRulesClick = {
                        navController.navigate(Screen.ClassificationRules.route)
                    },
                    onLanIntegrationClick = {
                        navController.navigate(Screen.LanIntegration.route)
                    }
                )
            }
            
            // Backup Restore Screen
            composable(Screen.BackupRestore.route) {
                BackupRestoreScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Archive Path Management Screen
            composable(Screen.ArchivePathManagement.route) {
                ArchivePathManagementScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Operation Log Screen
            composable(Screen.OperationLog.route) {
                OperationLogScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Batch Operations Screen
            composable(Screen.BatchOperations.route) {
                BatchOperationsScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Advanced Search Screen
            composable(Screen.AdvancedSearch.route) {
                AdvancedSearchScreen(
                    onBack = { navController.navigateUp() },
                    onReceiptClick = { receiptId ->
                        navController.navigate(Screen.ReceiptDetail.createRoute(receiptId))
                    },
                    voiceInputService = voiceInputService
                )
            }
            
            // Create Reimbursement Screen
            composable(Screen.CreateReimbursement.route) {
                CreateReimbursementScreen(
                    onBack = { navController.navigateUp() },
                    onSuccess = {
                        navController.navigateUp()
                    },
                    onComplianceCheck = { reimbursement, receipts ->
                        // Navigate to compliance validation screen
                        // For now, just show a toast (full implementation would need to pass data)
                        navController.navigate(Screen.ComplianceValidation.route)
                    },
                    voiceInputService = voiceInputService
                )
            }
            
            // Compliance Validation Screen
            composable(Screen.ComplianceValidation.route) {
                ComplianceValidationScreen(
                    reimbursement = com.billmii.android.data.model.Reimbursement(
                        id = 0,
                        title = "示例报销单",
                        totalAmount = 0.0,
                        applicant = "示例申请人",
                        department = "示例部门",
                        description = "",
                        status = com.billmii.android.data.model.ReimbursementStatus.DRAFT,
                        createdAt = java.time.LocalDateTime.now().toString(),
                        updatedAt = java.time.LocalDateTime.now().toString()
                    ),
                    receipts = emptyList(),
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Approval Workflow Screen
            composable(Screen.ApprovalWorkflow.route) {
                ApprovalWorkflowScreen(
                    onBack = { navController.navigateUp() },
                    onReimbursementDetail = { reimbursementId ->
                        navController.navigate(Screen.ReimbursementDetail.createRoute(reimbursementId))
                    }
                )
            }
            
            // OCR Templates Screen
            composable(Screen.OcrTemplates.route) {
                OcrTemplateScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Classification Rules Screen
            composable(Screen.ClassificationRules.route) {
                ClassificationRuleScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Export Screen
            composable(Screen.Export.route) {
                ExportScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Lan Integration Screen
            composable(Screen.LanIntegration.route) {
                LanIntegrationScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // Biometric Settings Screen
            composable(Screen.BiometricSettings.route) {
                BiometricSettingsScreen(
                    onBack = { navController.navigateUp() }
                )
            }
            
            // QR Code Scanner Screen
            composable(Screen.QrCodeScanner.route) {
                QrCodeScannerScreen(
                    onBack = { navController.navigateUp() },
                    onCodeDetected = { code, qrData ->
                        // Handle detected QR code
                        // For now, just navigate back
                        navController.navigateUp()
                    },
                    qrCodeScannerService = qrCodeScannerService
                )
            }
        }
    }
}
