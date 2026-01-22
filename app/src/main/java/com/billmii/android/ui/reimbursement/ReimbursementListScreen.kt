package com.billmii.android.ui.reimbursement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billmii.android.data.model.ReimbursementStatus
import com.billmii.android.ui.reimbursement.viewmodel.ReimbursementListViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reimbursement List Screen - 报销单列表界面
 * Displays all reimbursement applications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimbursementListScreen(
    paddingValues: PaddingValues,
    onReimbursementClick: (Long) -> Unit,
    onCreateReimbursementClick: () -> Unit = {},
    onApprovalWorkflowClick: () -> Unit = {},
    viewModel: ReimbursementListViewModel = hiltViewModel()
) {
    val reimbursements by viewModel.reimbursements.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报销管理") },
                actions = {
                    IconButton(onClick = onApprovalWorkflowClick) {
                        Icon(Icons.Default.Approval, contentDescription = "审批工作流")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateReimbursementClick
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建报销")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (reimbursements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("暂无报销单")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reimbursements) { reimbursement ->
                        ReimbursementListItem(
                            reimbursement = reimbursement,
                            onClick = { onReimbursementClick(reimbursement.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReimbursementListItem(
    reimbursement: com.billmii.android.data.model.Reimbursement,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reimbursement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "申请人: ${reimbursement.applicantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = reimbursement.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "金额: ¥${String.format("%.2f", reimbursement.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(reimbursement.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: ReimbursementStatus) {
    val (text, color) = when (status) {
        ReimbursementStatus.DRAFT -> "草稿" to MaterialTheme.colorScheme.outline
        ReimbursementStatus.PENDING -> "待审批" to MaterialTheme.colorScheme.tertiary
        ReimbursementStatus.APPROVED -> "已通过" to MaterialTheme.colorScheme.primary
        ReimbursementStatus.REJECTED -> "已拒绝" to MaterialTheme.colorScheme.error
        ReimbursementStatus.PAID -> "已支付" to MaterialTheme.colorScheme.primary
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

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(date)
        }
    }
}