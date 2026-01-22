package com.billmii.android.ui.reimbursement

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billmii.android.ui.reimbursement.viewmodel.ReimbursementDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reimbursement Detail Screen - 报销单详情界面
 * Displays detailed reimbursement information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimbursementDetailScreen(
    reimbursementId: Long,
    onBackClick: () -> Unit,
    viewModel: ReimbursementDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(reimbursementId) {
        viewModel.loadReimbursement(reimbursementId)
    }
    
    val reimbursement by viewModel.reimbursement.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报销单详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("返回")
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
            reimbursement == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("报销单不存在")
                }
            }
            else -> {
                ReimbursementDetailContent(
                    reimbursement = reimbursement!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Reimbursement Detail Content
 */
@Composable
fun ReimbursementDetailContent(
    reimbursement: com.billmii.android.data.model.Reimbursement,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp)
    ) {
        // Basic information
        SectionTitle("基本信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("报销单号", reimbursement.reimbursementNumber)
        InfoRow("标题", reimbursement.title)
        reimbursement.description?.let { InfoRow("说明", it) }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Applicant information
        SectionTitle("申请人信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("申请人", reimbursement.applicant)
        reimbursement.department?.let { InfoRow("部门", it) }
        reimbursement.position?.let { InfoRow("职位", it) }
        reimbursement.employeeId?.let { InfoRow("工号", it) }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Amount information
        SectionTitle("金额信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("报销总金额", "¥${String.format("%.2f", reimbursement.totalAmount)}")
        InfoRow("预付款", "¥${String.format("%.2f", reimbursement.advancePayment)}")
        InfoRow("退款金额", "¥${String.format("%.2f", reimbursement.refundAmount)}")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status information
        SectionTitle("状态信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("审批状态", reimbursement.approvalStatus.name)
        InfoRow("校验状态", reimbursement.validationStatus.name)
        InfoRow("当前步骤", "${reimbursement.currentStep}/${reimbursement.totalSteps}")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timestamps
        SectionTitle("时间信息")
        Spacer(modifier = Modifier.height(8.dp))
        
        InfoRow("创建时间", formatDate(reimbursement.createdAt))
        reimbursement.submittedAt?.let { InfoRow("提交时间", formatDate(it)) }
        reimbursement.approvedAt?.let { InfoRow("审批时间", formatDate(it)) }
        reimbursement.rejectedAt?.let { InfoRow("拒绝时间", formatDate(it)) }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Remarks
        reimbursement.remarks?.let {
            SectionTitle("备注")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
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
 * Format date
 */
private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}