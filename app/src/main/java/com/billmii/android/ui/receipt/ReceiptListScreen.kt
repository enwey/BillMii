package com.billmii.android.ui.receipt

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billmii.android.data.model.Receipt
import com.billmii.android.ui.receipt.viewmodel.ReceiptListViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Receipt List Screen - 票据列表界面
 * Displays all receipts with filtering and search capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    paddingValues: PaddingValues,
    onReceiptClick: (Long) -> Unit,
    onCameraClick: () -> Unit,
    onFilesImported: (List<Uri>) -> Unit = {},
    viewModel: ReceiptListViewModel = hiltViewModel()
) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    
    var showFileImportDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("票据管理") },
                actions = {
                    IconButton(onClick = { showFileImportDialog = true }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Import files")
                    }
                    IconButton(onClick = { /* TODO: Show filter dialog */ }) {
                        Text("筛选")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showFileImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Import files")
                }
                FloatingActionButton(
                    onClick = onCameraClick
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Take photo")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索票据...") },
                singleLine = true
            )
            
            // Filter chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )
            
            // Receipt list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (receipts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("暂无票据")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(receipts) { receipt ->
                        ReceiptListItem(
                            receipt = receipt,
                            onClick = { onReceiptClick(receipt.id) }
                        )
                    }
                }
            }
            
            // File import dialog
            FileImportDialog(
                show = showFileImportDialog,
                onDismiss = { showFileImportDialog = false },
                onFilesSelected = onFilesImported
            )
        }
    }
}

/**
 * Receipt List Item
 */
@Composable
fun ReceiptListItem(
    receipt: Receipt,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = receipt.receiptType.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(receipt.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Amount
            receipt.totalAmount?.let { amount ->
                Text(
                    text = "¥${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            receipt.amount?.let { amount ->
                Text(
                    text = "¥${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Additional info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // OCR status badge
                ReceiptStatusBadge(status = receipt.ocrStatus)
                
                // Validation status badge
                if (receipt.validationStatus != com.billmii.android.data.model.ValidationStatus.PENDING) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ValidationStatusBadge(status = receipt.validationStatus)
                }
            }
        }
    }
}

/**
 * Receipt Status Badge
 */
@Composable
fun ReceiptStatusBadge(status: com.billmii.android.data.model.OcrStatus) {
    val (text, color) = when (status) {
        com.billmii.android.data.model.OcrStatus.SUCCESS -> "已识别" to MaterialTheme.colorScheme.primary
        com.billmii.android.data.model.OcrStatus.PENDING -> "待识别" to MaterialTheme.colorScheme.outline
        com.billmii.android.data.model.OcrStatus.PROCESSING -> "识别中" to MaterialTheme.colorScheme.tertiary
        com.billmii.android.data.model.OcrStatus.FAILED -> "识别失败" to MaterialTheme.colorScheme.error
        com.billmii.android.data.model.OcrStatus.PARTIAL -> "部分识别" to MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Validation Status Badge
 */
@Composable
fun ValidationStatusBadge(status: com.billmii.android.data.model.ValidationStatus) {
    val (text, color) = when (status) {
        com.billmii.android.data.model.ValidationStatus.VALID -> "校验通过" to MaterialTheme.colorScheme.primary
        com.billmii.android.data.model.ValidationStatus.PENDING -> "待校验" to MaterialTheme.colorScheme.outline
        com.billmii.android.data.model.ValidationStatus.WARNING -> "有警告" to MaterialTheme.colorScheme.secondary
        com.billmii.android.data.model.ValidationStatus.INVALID -> "校验失败" to MaterialTheme.colorScheme.error
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Filter Chips Row
 */
@Composable
fun FilterChipsRow(
    selectedFilter: ReceiptFilter,
    onFilterSelected: (ReceiptFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReceiptFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

/**
 * Receipt Filter enumeration
 */
enum class ReceiptFilter(val displayName: String) {
    ALL("全部"),
    PENDING("待处理"),
    PROCESSED("已处理"),
    INVOICE("发票"),
    EXPENSE("费用")
}

/**
 * Format date
 */
private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(date)
}