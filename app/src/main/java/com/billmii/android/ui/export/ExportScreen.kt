package com.billmii.android.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Export Screen
 * Allows users to export receipts and reimbursement data in various formats
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showFormatDialog by viewModel.showFormatDialog.collectAsState()
    val showTemplateDialog by viewModel.showTemplateDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据导出") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(uiState.exportMessage)
                    if (uiState.exportProgress > 0) {
                        LinearProgressIndicator(
                            progress = { uiState.exportProgress / 100f },
                            modifier = Modifier.width(200.dp)
                        )
                        Text("${uiState.exportProgress}%")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Export Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "快速导出",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExportFormatButton(
                                    icon = Icons.Default.TableChart,
                                    label = "Excel",
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { viewModel.exportToExcel(ExportType.QUICK) }
                                )
                                ExportFormatButton(
                                    icon = Icons.Default.PictureAsPdf,
                                    label = "PDF",
                                    color = MaterialTheme.colorScheme.error,
                                    onClick = { viewModel.exportToPdf(ExportType.QUICK) }
                                )
                                ExportFormatButton(
                                    icon = Icons.Default.Code,
                                    label = "CSV",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    onClick = { viewModel.exportToCsv(ExportType.QUICK) }
                                )
                                ExportFormatButton(
                                    icon = Icons.Default.Description,
                                    label = "JSON",
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = { viewModel.exportToJson(ExportType.QUICK) }
                                )
                            }
                        }
                    }
                }
                
                // Custom Export Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "自定义导出",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Button(
                                onClick = { viewModel.showFormatOptions() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("配置导出选项")
                            }
                            
                            // Export templates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "导出模板",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = { viewModel.showTemplateOptions() }) {
                                    Text("管理模板")
                                }
                            }
                            
                            if (uiState.exportTemplates.isEmpty()) {
                                Text(
                                    text = "暂无模板",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.exportTemplates.take(3).forEach { template ->
                                        ExportTemplateItem(
                                            template = template,
                                            onUse = { viewModel.exportWithTemplate(template) },
                                            onEdit = { viewModel.editTemplate(template) },
                                            onDelete = { viewModel.deleteTemplate(template.id) }
                                        )
                                    }
                                    if (uiState.exportTemplates.size > 3) {
                                        TextButton(
                                            onClick = { viewModel.showTemplateOptions() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("查看全部 ${uiState.exportTemplates.size} 个模板")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Export History
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "导出历史",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { viewModel.clearExportHistory() }) {
                                    Text("清空")
                                }
                            }
                            
                            if (uiState.exportHistory.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无导出记录",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.exportHistory.take(5).forEach { record ->
                                        ExportHistoryItem(record = record)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Export Format Dialog
    if (showFormatDialog) {
        ExportFormatDialog(
            onDismiss = { viewModel.hideFormatDialog() },
            onExport = { format, dataRange, includeImages ->
                viewModel.exportWithFormat(format, dataRange, includeImages)
            }
        )
    }
    
    // Template Dialog
    if (showTemplateDialog) {
        ExportTemplateDialog(
            template = uiState.editingTemplate,
            onDismiss = { viewModel.hideTemplateDialog() },
            onSave = { template -> viewModel.saveTemplate(template) }
        )
    }
    
    // Success Dialog
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSuccessDialog() },
            title = { Text("导出成功") },
            text = { Text(uiState.successMessage ?: "数据导出完成") },
            confirmButton = {
                Button(onClick = { viewModel.hideSuccessDialog() }) {
                    Text("确定")
                }
            }
        )
    }
    
    // Error Dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("导出失败") },
            text = { Text(uiState.error ?: "未知错误") },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun ExportFormatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ExportTemplateItem(
    template: ExportTemplate,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "格式: ${template.format}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                IconButton(onClick = onUse) {
                    Icon(Icons.Default.Download, contentDescription = "使用")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ExportHistoryItem(record: ExportRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${record.format} - ${formatDateTime(record.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatFileSize(record.fileSize),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat, DataRange, Boolean) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.EXCEL) }
    var selectedRange by remember { mutableStateOf(DataRange.ALL) }
    var includeImages by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置导出选项") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("导出格式")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.values().forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Text("数据范围")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DataRange.values().forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeImages,
                        onCheckedChange = { includeImages = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("包含图片")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(selectedFormat, selectedRange, includeImages) }) {
                Text("开始导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ExportTemplateDialog(
    template: ExportTemplate?,
    onDismiss: () -> Unit,
    onSave: (ExportTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var format by remember { mutableStateOf(template?.format ?: ExportFormat.EXCEL) }
    var selectedFields by remember { mutableStateOf(template?.selectedFields ?: emptyList()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "新建导出模板" else "编辑导出模板") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = if (it.isBlank()) "模板名称不能为空" else null
                    },
                    label = { Text("模板名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } }
                )
                
                Text("导出格式")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.values().forEach { fmt ->
                        FilterChip(
                            selected = format == fmt,
                            onClick = { format = fmt },
                            label = { Text(fmt.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Text("导出字段")
                val availableFields = listOf(
                    "收据编号", "收据类型", "销售方", "购买方", "金额", "日期",
                    "商品明细", "归档路径", "归档号", "创建时间"
                )
                
                Column {
                    availableFields.forEach { field ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFields.contains(field),
                                onCheckedChange = { checked ->
                                    selectedFields = if (checked) {
                                        selectedFields + field
                                    } else {
                                        selectedFields - field
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(field)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "模板名称不能为空"
                    } else {
                        onSave(
                            ExportTemplate(
                                id = template?.id ?: 0,
                                name = name,
                                format = format,
                                selectedFields = selectedFields,
                                createdAt = template?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// Data models
enum class ExportFormat(val displayName: String) {
    EXCEL("Excel"),
    PDF("PDF"),
    CSV("CSV"),
    JSON("JSON")
}

enum class ExportType {
    QUICK,
    CUSTOM
}

enum class DataRange(val displayName: String) {
    ALL("全部"),
    THIS_MONTH("本月"),
    LAST_MONTH("上月"),
    CUSTOM("自定义")
}

data class ExportTemplate(
    val id: Long,
    val name: String,
    val format: ExportFormat,
    val selectedFields: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

data class ExportRecord(
    val id: Long,
    val fileName: String,
    val format: String,
    val fileSize: Long,
    val timestamp: Long,
    val filePath: String
)

fun formatDateTime(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}