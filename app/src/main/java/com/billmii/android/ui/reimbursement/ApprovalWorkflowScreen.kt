package com.billmii.android.ui.reimbursement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Reimbursement Approval Workflow Screen
 * Allows users to review, approve, or reject reimbursement applications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalWorkflowScreen(
    onBack: () -> Unit,
    onReimbursementDetail: (Long) -> Unit,
    viewModel: ApprovalWorkflowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showApproveDialog by viewModel.showApproveDialog.collectAsState()
    val showRejectDialog by viewModel.showRejectDialog.collectAsState()
    val showCommentDialog by viewModel.showCommentDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("审批工作流") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs
            FilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.selectFilter(it) },
                pendingCount = uiState.pendingCount,
                approvedCount = uiState.approvedCount,
                rejectedCount = uiState.rejectedCount
            )
            
            // Pending items list
            if (uiState.pendingItems.isEmpty() && uiState.selectedFilter == ApprovalFilter.PENDING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "暂无待审批项目",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (uiState.selectedFilter) {
                        ApprovalFilter.PENDING -> {
                            items(uiState.pendingItems) { item ->
                                ApprovalItemCard(
                                    item = item,
                                    onClick = { onReimbursementDetail(item.reimbursementId) },
                                    onApprove = { viewModel.showApproveConfirmation(item) },
                                    onReject = { viewModel.showRejectConfirmation(item) },
                                    onComment = { viewModel.showCommentDialog(item) }
                                )
                            }
                        }
                        ApprovalFilter.APPROVED -> {
                            items(uiState.approvedItems) { item ->
                                ApprovalItemCard(
                                    item = item,
                                    onClick = { onReimbursementDetail(item.reimbursementId) },
                                    onApprove = null,
                                    onReject = null,
                                    onComment = { viewModel.showCommentDialog(item) }
                                )
                            }
                        }
                        ApprovalFilter.REJECTED -> {
                            items(uiState.rejectedItems) { item ->
                                ApprovalItemCard(
                                    item = item,
                                    onClick = { onReimbursementDetail(item.reimbursementId) },
                                    onApprove = { viewModel.showApproveConfirmation(item) },
                                    onReject = null,
                                    onComment = { viewModel.showCommentDialog(item) }
                                )
                            }
                        }
                        ApprovalFilter.ALL -> {
                            items(uiState.allItems) { item ->
                                ApprovalItemCard(
                                    item = item,
                                    onClick = { onReimbursementDetail(item.reimbursementId) },
                                    onApprove = if (item.status == ApprovalStatus.PENDING) {
                                        { viewModel.showApproveConfirmation(item) }
                                    } else null,
                                    onReject = if (item.status == ApprovalStatus.PENDING) {
                                        { viewModel.showRejectConfirmation(item) }
                                    } else null,
                                    onComment = { viewModel.showCommentDialog(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Approve Confirmation Dialog
    if (showApproveDialog) {
        ApproveConfirmDialog(
            item = uiState.selectedItem,
            onDismiss = { viewModel.hideApproveDialog() },
            onConfirm = { comment -> viewModel.approveReimbursement(comment) }
        )
    }
    
    // Reject Confirmation Dialog
    if (showRejectDialog) {
        RejectConfirmDialog(
            item = uiState.selectedItem,
            onDismiss = { viewModel.hideRejectDialog() },
            onConfirm = { comment -> viewModel.rejectReimbursement(comment) }
        )
    }
    
    // Comment Dialog
    if (showCommentDialog) {
        CommentDialog(
            item = uiState.selectedItem,
            onDismiss = { viewModel.hideCommentDialog() },
            onConfirm = { comment -> viewModel.addComment(comment) }
        )
    }
}

@Composable
fun FilterTabs(
    selectedFilter: ApprovalFilter,
    onFilterSelected: (ApprovalFilter) -> Unit,
    pendingCount: Int,
    approvedCount: Int,
    rejectedCount: Int
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ApprovalFilter.values().forEach { filter ->
            val count = when (filter) {
                ApprovalFilter.PENDING -> pendingCount
                ApprovalFilter.APPROVED -> approvedCount
                ApprovalFilter.REJECTED -> rejectedCount
                ApprovalFilter.ALL -> pendingCount + approvedCount + rejectedCount
            }
            
            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(filter.displayName)
                        if (count > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ApprovalItemCard(
    item: ApprovalItem,
    onClick: () -> Unit,
    onApprove: (() -> Unit)?,
    onReject: (() -> Unit)?,
    onComment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "申请人: ${item.applicantName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                StatusBadge(status = item.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "金额: ¥${String.format("%.2f", item.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "收据: ${item.receiptCount} 张",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.department != null) {
                Text(
                    text = "部门: ${item.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onComment,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("备注")
                }
                
                onReject?.let {
                    Button(
                        onClick = it,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("拒绝")
                    }
                }
                
                onApprove?.let {
                    Button(
                        onClick = it,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("通过")
                    }
                }
            }
            
            // Show latest comment if exists
            item.latestComment?.let { comment ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = comment.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ApprovalStatus) {
    val (text, color) = when (status) {
        ApprovalStatus.PENDING -> "待审批" to Color(0xFFFFA000)
        ApprovalStatus.APPROVED -> "已通过" to Color(0xFF4CAF50)
        ApprovalStatus.REJECTED -> "已拒绝" to Color(0xFFF44336)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ApproveConfirmDialog(
    item: ApprovalItem?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认通过") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("确定要通过此报销申请吗？")
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("审批意见（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(comment) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("通过")
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
fun RejectConfirmDialog(
    item: ApprovalItem?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认拒绝") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("确定要拒绝此报销申请吗？请说明拒绝原因。")
                OutlinedTextField(
                    value = comment,
                    onValueChange = { 
                        comment = it
                        commentError = if (it.isBlank()) "请填写拒绝原因" else null
                    },
                    label = { Text("拒绝原因") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = commentError != null,
                    supportingText = { commentError?.let { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.isBlank()) {
                        commentError = "请填写拒绝原因"
                    } else {
                        onConfirm(comment)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("拒绝")
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
fun CommentDialog(
    item: ApprovalItem?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加备注") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { 
                        comment = it
                        commentError = if (it.isBlank()) "备注内容不能为空" else null
                    },
                    label = { Text("备注内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    isError = commentError != null,
                    supportingText = { commentError?.let { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.isBlank()) {
                        commentError = "备注内容不能为空"
                    } else {
                        onConfirm(comment)
                    }
                }
            ) {
                Text("添加")
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
enum class ApprovalFilter(val displayName: String) {
    PENDING("待审批"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    ALL("全部")
}

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class ApprovalItem(
    val reimbursementId: Long,
    val title: String,
    val applicantName: String,
    val amount: Double,
    val receiptCount: Int,
    val department: String?,
    val status: ApprovalStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val latestComment: ApprovalComment?
)

data class ApprovalComment(
    val id: Long,
    val content: String,
    val authorName: String,
    val createdAt: Long
)