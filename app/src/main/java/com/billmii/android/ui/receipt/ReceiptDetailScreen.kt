package com.billmii.android.ui.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.billmii.android.ui.receipt.viewmodel.ReceiptDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Receipt Detail Screen - 票据详情界面
 * Displays detailed receipt information with editing capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: Long,
    onBackClick: () -> Unit,
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(receiptId) {
        viewModel.loadReceipt(receiptId)
    }
    
    val receipt by viewModel.receipt.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("票据详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("返回")
                    }
                },
                actions = {
                    if (receipt != null && !isEditing) {
                        IconButton(onClick = { viewModel.toggleEditing() }) {
                            Text("编辑")
                        }
                    }
                    if (isEditing) {
                        TextButton(onClick = { viewModel.saveReceipt() }) {
                            Text("保存")
                        }
                        TextButton(onClick = { viewModel.toggleEditing() }) {
                            Text("取消")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            receipt == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("票据不存在")
                }
            }
            else -> {
                ReceiptDetailContent(
                    receipt = receipt!!,
                    isEditing = isEditing,
                    onFieldChange = { field, value -> viewModel.updateField(field, value) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Receipt Detail Content
 */
@Composable
fun ReceiptDetailContent(
    receipt: com.billmii.android.data.model.Receipt,
    isEditing: Boolean,
    onFieldChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Receipt image preview
        ReceiptImagePreview(
            filePath = receipt.filePath,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Basic information section
        SectionTitle("基本信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("文件名", receipt.fileName)
        InfoRow("文件类型", receipt.fileType)
        InfoRow("文件大小", formatFileSize(receipt.fileSize))
        InfoRow("创建时间", formatDate(receipt.createdAt))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Receipt type section
        SectionTitle("票据类型")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("类型", receipt.receiptType.displayName)
        InfoRow("分类", receipt.receiptCategory.displayName)
        receipt.expenseSubCategory?.let {
            InfoRow("子分类", it.displayName)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Invoice fields (if applicable)
        if (receipt.receiptCategory == com.billmii.android.data.model.ReceiptCategory.INVOICE) {
            SectionTitle("发票信息")
            Spacer(modifier = Modifier.height(8.dp))
            
            receipt.invoiceCode?.let { InfoRow("发票代码", it) }
            receipt.invoiceNumber?.let { InfoRow("发票号码", it) }
            receipt.invoiceDate?.let { InfoRow("开票日期", formatDate(it)) }
            receipt.buyerName?.let { InfoRow("购买方", it) }
            receipt.buyerTaxId?.let { InfoRow("购买方税号", it) }
            receipt.sellerName?.let { InfoRow("销售方", it) }
            receipt.sellerTaxId?.let { InfoRow("销售方税号", it) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Amount section
        SectionTitle("金额信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        receipt.totalAmount?.let { 
            InfoRow("价税合计", "¥${String.format("%.2f", it)}") 
        }
        receipt.amountWithoutTax?.let { 
            InfoRow("不含税金额", "¥${String.format("%.2f", it)}") 
        }
        receipt.taxAmount?.let { 
            InfoRow("税额", "¥${String.format("%.2f", it)}") 
        }
        receipt.taxRate?.let { 
            InfoRow("税率", "${String.format("%.2f", it)}%") 
        }
        receipt.amount?.let { 
            InfoRow("金额", "¥${String.format("%.2f", it)}") 
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expense fields (if applicable)
        if (receipt.departurePlace != null || receipt.destination != null) {
            SectionTitle("行程信息")
            Spacer(modifier = Modifier.height(8.dp))
            
            receipt.departurePlace?.let { InfoRow("出发地", it) }
            receipt.destination?.let { InfoRow("目的地", it) }
            receipt.expenseDate?.let { InfoRow("日期", formatDate(it)) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status section
        SectionTitle("状态信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("OCR状态", receipt.ocrStatus.name)
        InfoRow("校验状态", receipt.validationStatus.name)
        InfoRow("是否已处理", if (receipt.isProcessed) "是" else "否")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Archive information
        receipt.archiveNumber?.let {
            SectionTitle("归档信息")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("归档编号", it)
            receipt.archivePath?.let { InfoRow("归档路径", it) }
        }
    }
}

/**
 * Section Title
 */
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Info Row
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Receipt Image Preview
 */
@Composable
fun ReceiptImagePreview(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val file = File(filePath)
    
    Card(
        modifier = modifier
    ) {
        if (file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = "票据图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material.icons.Icons.Outlined.ImageBroken
                    Text(
                        text = "图片不存在",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Format file size
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Format date
 */
private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}