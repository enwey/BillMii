package com.billmii.android.ui.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Batch Operations Screen
 * Allows users to perform batch operations on multiple receipts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchOperationsScreen(
    onBack: () -> Unit,
    viewModel: BatchOperationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量操作") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.selectAll() },
                        enabled = !uiState.allSelected
                    ) {
                        Text("全选")
                    }
                    TextButton(
                        onClick = { viewModel.clearSelection() },
                        enabled = uiState.selectedReceipts.isNotEmpty()
                    ) {
                        Text("清空")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Selection Summary
                SelectionSummaryCard(
                    totalCount = uiState.availableReceipts.size,
                    selectedCount = uiState.selectedReceipts.size,
                    selectedAmount = uiState.selectedAmount
                )
                
                // Receipt List
                if (uiState.availableReceipts.isEmpty()) {
                    EmptyReceiptsView()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availableReceipts) { receipt ->
                            SelectableReceiptItem(
                                receipt = receipt,
                                isSelected = uiState.selectedReceipts.contains(receipt.id),
                                onToggle = { viewModel.toggleSelection(receipt.id) }
                            )
                        }
                    }
                }
                
                // Action Bar
                ActionActionBar(
                    selectedCount = uiState.selectedReceipts.size,
                    onBatchDelete = { viewModel.batchDelete() },
                    onBatchExport = { viewModel.showExportDialog() },
                    onBatchOcr = { viewModel.batchOcr() },
                    onBatchClassify = { viewModel.batchClassify() }
                )
            }
        }
    }
    
    // Export Dialog
    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { viewModel.hideExportDialog() },
            onConfirm = { format, includeImages ->
                viewModel.batchExport(format, includeImages)
            }
        )
    }
    
    // Processing Dialog
    if (uiState.isProcessing) {
        ProcessingDialog(
            message = uiState.processingMessage,
            progress = uiState.processingProgress
        )
    }
}

@Composable
fun SelectionSummaryCard(
    totalCount: Int,
    selectedCount: Int,
    selectedAmount: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "已选择 $selectedCount / $totalCount 项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "总金额: ¥${formatAmount(selectedAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (selectedCount > 0) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SelectableReceiptItem(
    receipt: ReceiptSummary,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.receiptType.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = receipt.sellerName ?: "未知销售方",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¥${formatAmount(receipt.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ActionActionBar(
    selectedCount: Int,
    onBatchDelete: () -> Unit,
    onBatchExport: () -> Unit,
    onBatchOcr: () -> Unit,
    onBatchClassify: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = onBatchDelete,
                label = { Text("删除") },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                enabled = selectedCount > 0
            )
            
            AssistChip(
                onClick = onBatchExport,
                label = { Text("导出") },
                leadingIcon = {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                enabled = selectedCount > 0
            )
            
            AssistChip(
                onClick = onBatchOcr,
                label = { Text("OCR识别") },
                leadingIcon = {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                enabled = selectedCount > 0
            )
            
            AssistChip(
                onClick = onBatchClassify,
                label = { Text("分类") },
                leadingIcon = {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                enabled = selectedCount > 0
            )
        }
    }
}

@Composable
fun EmptyReceiptsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "暂无票据",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "请先添加票据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportFormat, Boolean) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.EXCEL) }
    var includeImages by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出选项") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "导出格式",
                    style = MaterialTheme.typography.titleSmall
                )
                
                ExportFormat.values().forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(format.displayName)
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
            Button(onClick = { onConfirm(selectedFormat, includeImages) }) {
                Text("导出")
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
fun ProcessingDialog(
    message: String,
    progress: Float
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("处理中") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(progress = { progress })
                Text(message)
            }
        },
        confirmButton = {}
    )
}

// Export format enum
enum class ExportFormat(val displayName: String) {
    EXCEL("Excel"),
    PDF("PDF"),
    JSON("JSON"),
    CSV("CSV")
}

fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}