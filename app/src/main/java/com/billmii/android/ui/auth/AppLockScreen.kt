package com.billmii.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billmii.android.security.BiometricAuthManager
import com.billmii.android.ui.auth.viewmodel.AppLockViewModel

/**
 * App Lock Screen
 * Authentication screen displayed when app is locked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    onBiometricNotAvailable: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val biometricResult by viewModel.biometricResult.collectAsState()
    val authInProgress by viewModel.authInProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    LaunchedEffect(biometricResult) {
        if (biometricResult is BiometricAuthManager.BiometricAuthResult.Success) {
            onUnlockSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App name
            Text(
                text = "BillMii",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "票据管理助手",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Authentication status
            when {
                authInProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在验证...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                errorMessage != null -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.authenticate() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("重试")
                    }
                }
                biometricResult is BiometricAuthManager.BiometricAuthResult.Success -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Green
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "验证成功",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "请验证身份以继续",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.authenticate() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始验证")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Skip button for biometric not available
            if (biometricResult is BiometricAuthManager.BiometricAuthResult.Failed) {
                OutlinedButton(
                    onClick = onBiometricNotAvailable,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("跳过验证")
                }
            }
        }
    }
}

/**
 * Biometric Setup Prompt Screen
 * Shown when biometric is available but not set up
 */
@Composable
fun BiometricSetupPrompt(
    onEnableBiometric: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        icon = {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
        },
        title = {
            Text("启用生物识别")
        },
        text = {
            Text(
                "为了更好的安全性，建议启用指纹或面容验证来保护您的数据。" +
                "\n\n您也可以稍后在设置中启用此功能。"
            )
        },
        confirmButton = {
            Button(onClick = onEnableBiometric) {
                Text("启用")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("稍后")
            }
        }
    )
}

/**
 * Biometric Not Available Screen
 * Shown when biometric hardware is not available
 */
@Composable
fun BiometricNotAvailableScreen(
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        icon = {
            Icon(Icons.Default.Info, contentDescription = null)
        },
        title = {
            Text("生物识别不可用")
        },
        text = {
            Text(
                "您的设备不支持生物识别或未设置指纹/面容。" +
                "\n\n您可以使用应用密码保护来保护您的数据。"
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("继续")
            }
        }
    )
}