package com.billmii.android.ui.reimbursement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.*

/**
 * Create Reimbursement Screen
 * Allows users to create new reimbursement applications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReimbursementScreen(
    onBack: () -> Unit,
    viewModel: CreateReimbursementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showReceiptSelector by viewModel.showReceiptSelector.collectAsState()
    val showValidationError by viewModel.showValidationError.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创建报销单") },
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
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("报销标题 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showValidationError && uiState.title.isBlank()
                    )
                    
                    // Description
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.updateDescription(it) },
                        label = { Text("描述（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    // Applicant
                    OutlinedTextField(
                        value = uiState.applicant,
                        onValueChange = { viewModel.updateApplicant(it) },
                        label = { Text("申请人 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showValidationError && uiState.applicant.isBlank()
                    )
                    
                    // Department
                    OutlinedTextField(
                        value = uiState.department,
                        onValueChange = { viewModel.updateDepartment(it) },
                        label = { Text("部门（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Project
                    OutlinedTextField(
                        value = uiState.project,
                        onValueChange = { viewModel.updateProject(it) },
                        label = { Text("项目（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Budget Code
                    OutlinedTextField(
                        value = uiState.budgetCode,
                        onValueChange = { viewModel.updateBudgetCode(it) },
                        label = { Text("预算代码（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Divider()
                    
                    // Receipts Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "关联票据 (${uiState.selectedReceipts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = { viewModel.showReceiptSelectorDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("添加票据")
                        }
                    }
                    
                    if (uiState.selectedReceipts.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无票据，点击上方按钮添加",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.selectedReceipts) { receipt ->
                                ReceiptItem(
                                    receipt = receipt,
                                    onRemove = { viewModel.removeReceipt(receipt.id) }
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Summary
                    SummaryCard(
                        receiptCount = uiState.selectedReceipts.size,
                        totalAmount = uiState.totalAmount,
                        taxAmount = uiState.taxAmount
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.saveAsDraft() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存草稿")
                        }
                        Button(
                            onClick = { 
                                viewModel.submitForApproval()
                                if (uiState.validationErrors.isEmpty()) {
                                    onBack()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("提交审批")
                        }
                    }
                }
            }
        }
    }
    
    // Receipt Selector Dialog
    if (showReceiptSelector) {
        ReceiptSelectorDialog(
            availableReceipts = uiState.availableReceipts,
            selectedReceipts = uiState.selectedReceipts,
            onDismiss = { viewModel.hideReceiptSelectorDialog() },
            onConfirm = { selected ->
                viewModel.updateSelectedReceipts(selected)
            }
        )
    }
    
    // Validation Error Dialog
    if (showValidationError && uiState.validationErrors.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.hideValidationError() },
            title = { Text("验证错误") },
            text = {
                Column {
                    Text("请修复以下错误后重试：")
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.validationErrors.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideValidationError() }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun ReceiptItem(
    receipt: ReceiptSummary,
    onRemove: () -> Unit
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
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "移除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    receiptCount: Int,
    totalAmount: Double,
    taxAmount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow(
                label = "票据数量",
                value = receiptCount.toString()
            )
            SummaryRow(
                label = "总金额",
                value = "¥${formatAmount(totalAmount)}",
                isBold = true
            )
            SummaryRow(
                label = "税额",
                value = "¥${formatAmount(taxAmount)}"
            )
            SummaryRow(
                label = "不含税金额",
                value = "¥${formatAmount(totalAmount - taxAmount)}"
            )
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )
        Text(
            text = value,
            style = if (isBold) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (isBold) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun ReceiptSelectorDialog(
    availableReceipts: List<ReceiptSummary>,
    selectedReceipts: List<ReceiptSummary>,
    onDismiss: () -> Unit,
    onConfirm: (List<ReceiptSummary>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(selectedReceipts.map { it.id }.toSet()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择票据") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (availableReceipts.isEmpty()) {
                    Text(
                        text = "暂无可选票据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableReceipts.forEach { receipt ->
                        SelectableReceiptItem(
                            receipt = receipt,
                            isSelected = selectedIds.contains(receipt.id),
                            onToggle = {
                                selectedIds = if (selectedIds.contains(receipt.id)) {
                                    selectedIds - receipt.id
                                } else {
                                    selectedIds + receipt.id
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = availableReceipts.filter { it.id in selectedIds }
                    onConfirm(selected)
                }
            ) {
                Text("确定")
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
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
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
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

fun formatAmount(amount: Double?): String {
    return NumberFormat.getCurrencyInstance(Locale.CHINA).format(amount ?: 0.0)
}

// Data models
data class ReceiptSummary(
    val id: Long,
    val receiptType: com.billmii.android.data.model.ReceiptType,
    val sellerName: String?,
    val amount: Double
)