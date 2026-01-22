package com.billmii.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billmii.android.data.service.LanIntegrationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanIntegrationScreen(
    onBack: () -> Unit,
    viewModel: LanIntegrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var apiKey by remember { mutableStateOf("") }
    var useHttps by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadConfig()
        viewModel.integrationStatus.collect { status ->
            // Status updates handled by UI state
        }
    }
    
    // Load saved config when available
    LaunchedEffect(uiState.config) {
        uiState.config?.let { config ->
            host = config.host
            port = config.port.toString()
            apiKey = config.apiKey ?: ""
            useHttps = config.protocol == "https"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("局域网财务软件集成") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isConnected) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "已连接",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isConnected) 
                        MaterialTheme.colorScheme.primaryContainer
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (uiState.isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (uiState.isConnected) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isConnected) "已连接" else "未连接",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.isConnected) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        if (uiState.lastSyncTime != null) {
                            Text(
                                text = "最后同步: ${formatTimestamp(uiState.lastSyncTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Server Configuration
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "服务器配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("服务器地址") },
                        placeholder = { Text("例如: 192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Dns, contentDescription = null)
                        }
                    )
                    
                    OutlinedTextField(
                        value = port,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                port = it
                            }
                        },
                        label = { Text("端口") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Default.SettingsEthernet, contentDescription = null)
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it }
                        )
                        Text(
                            text = "使用 HTTPS",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API 密钥 (可选)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        visualTransformation = if (apiKey.isNotEmpty()) {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        } else {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val config = LanIntegrationService.ServerConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 8080,
                                    protocol = if (useHttps) "https" else "http",
                                    apiKey = apiKey.ifBlank { null }
                                )
                                viewModel.saveConfig(config)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存配置")
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.testConnection() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("测试连接")
                        }
                    }
                }
            }
            
            // Sync Operations
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "数据同步",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Divider()
                    
                    Button(
                        onClick = { viewModel.syncReceipts() },
                        enabled = uiState.isConnected && !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同步票据")
                    }
                    
                    Button(
                        onClick = { viewModel.syncReimbursements() },
                        enabled = uiState.isConnected && !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RequestQuote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同步报销单")
                    }
                    
                    Button(
                        onClick = { viewModel.syncAll() },
                        enabled = uiState.isConnected && !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("全部同步")
                    }
                    
                    if (uiState.isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Data Format Conversion
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "数据格式转换",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Divider()
                    
                    var sourceFormat by remember { mutableStateOf("JSON") }
                    var targetFormat by remember { mutableStateOf("XML") }
                    
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = {}
                    ) {
                        OutlinedTextField(
                            value = sourceFormat,
                            onValueChange = {},
                            label = { Text("源格式") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = {}
                    ) {
                        OutlinedTextField(
                            value = targetFormat,
                            onValueChange = {},
                            label = { Text("目标格式") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.convertDataFormat(sourceFormat, targetFormat) },
                        enabled = uiState.isConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("转换格式")
                    }
                }
            }
            
            // Sync Results
            if (uiState.syncResult != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.syncResult!!.success)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
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
                                if (uiState.syncResult!!.success)
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.Error,
                                contentDescription = null,
                                tint = if (uiState.syncResult!!.success)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (uiState.syncResult!!.success) "同步成功" else "同步失败",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        if (uiState.syncResult!!.syncedCount > 0) {
                            Text(
                                text = "成功: ${uiState.syncResult!!.syncedCount} 条",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        if (uiState.syncResult!!.failedCount > 0) {
                            Text(
                                text = "失败: ${uiState.syncResult!!.failedCount} 条",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (uiState.syncResult!!.errors.isNotEmpty()) {
                            uiState.syncResult!!.errors.forEach { error ->
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            
            // Clear Configuration Button
            OutlinedButton(
                onClick = { viewModel.clearConfig() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("清除配置")
            }
            
            // Help Text
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
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "1. 输入局域网内财务软件服务器的地址和端口\n" +
                               "2. 点击\"保存配置\"保存设置\n" +
                               "3. 点击\"测试连接\"验证连接\n" +
                               "4. 连接成功后可以进行数据同步",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return format.format(date)
}