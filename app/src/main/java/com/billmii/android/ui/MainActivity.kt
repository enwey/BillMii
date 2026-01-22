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
import com.billmii.android.ui.receipt.*
import com.billmii.android.ui.reimbursement.*
import com.billmii.android.ui.settings.SettingsScreen
import com.billmii.android.ui.settings.viewmodel.SettingsViewModel
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
                    onFilesImported = ::handleFilesImported
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
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
    onFilesImported: (List<Uri>) -> Unit
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
                    onFilesImported = onFilesImported
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
                    paddingValues = paddingValues
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    paddingValues = paddingValues
                )
            }
        }
    }
}
