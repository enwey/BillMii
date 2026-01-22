package com.billmii.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billmii.android.data.service.BiometricAuthService
import com.billmii.android.ui.settings.viewmodel.BiometricSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricSettingsScreen(
    onBack: () -> Unit,
    viewModel: BiometricSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.checkBiometricAvailability()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生物识别设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isAvailable) 
                        MaterialTheme.colorScheme.primaryContainer
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (uiState.isAvailable) Icons.Default.Fingerprint else Icons.Default.FingerprintOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (uiState.isAvailable) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = if (uiState.isAvailable) "生物识别可用" else "生物识别不可用",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (uiState.isAvailable) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = uiState.biometricType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Settings Card
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Enable Biometric Switch
                    if (uiState.isAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "启用生物识别",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "使用${uiState.biometricType}解锁应用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = { 
                                    viewModel.toggleBiometric(it) 
                                },
                                enabled = uiState.canToggle
                            )
                        }
                    } else {
                        // Enroll Prompt
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "需要设置生物识别",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = uiState.enrollPrompt,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // Test Authentication Button
                    if (uiState.isAvailable && uiState.isEnabled) {
                        Button(
                            onClick = { viewModel.testAuthentication() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试生物识别")
                        }
                    }
                }
            }
            
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "关于生物识别",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    InfoItem(
                        icon = Icons.Default.Security,
                        title = "安全性",
                        description = "生物识别数据存储在设备的安全区域，不会上传到服务器"
                    )
                    
                    InfoItem(
                        icon = Icons.Default.Speed,
                        title = "便捷性",
                        description = "无需输入密码，快速解锁应用"
                    )
                    
                    InfoItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "隐私保护",
                        description = "只有您可以授权访问敏感数据"
                    )
                }
            }
            
            // Test Result Dialog
            if (uiState.testResult != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearTestResult() },
                    icon = {
                        Icon(
                            if (uiState.testResult!!.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (uiState.testResult!!.isSuccess) 
                                Color.Green else Color.Red
                        )
                    },
                    title = {
                        Text(
                            if (uiState.testResult!!.isSuccess) "认证成功" else "认证失败"
                        )
                    },
                    text = {
                        Text(uiState.testResult!!.message)
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearTestResult() }) {
                            Text("确定")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}