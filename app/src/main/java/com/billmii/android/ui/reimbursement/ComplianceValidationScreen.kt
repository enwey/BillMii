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
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.service.ComplianceValidationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceValidationScreen(
    reimbursement: Reimbursement,
    receipts: List<Receipt>,
    onBack: () -> Unit,
    onFixIssue: (String) -> Unit = {},
    complianceService: ComplianceValidationService
) {
    val scope = rememberCoroutineScope()
    var validationResult by remember { mutableStateOf<ComplianceValidationService.ValidationResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSuggestions by remember { mutableStateOf(false) }
    var selectedIssue by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(reimbursement, receipts) {
        isLoading = true
        validationResult = complianceService.validateReimbursement(reimbursement, receipts)
        isLoading = false
    }
    
    val suggestions = remember(reimbursement, receipts) {
        complianceService.getComplianceSuggestions(reimbursement, receipts)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("合规性检查") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isLoading && validationResult != null) {
                        IconButton(onClick = { showSuggestions = !showSuggestions }) {
                            Icon(
                                if (showSuggestions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "建议"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                validationResult != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 合规分数卡片
                        item {
                            ComplianceScoreCard(
                                score = validationResult!!.score,
                                isCompliant = validationResult!!.isCompliant,
                                totalReceipts = receipts.size,
                                totalAmount = reimbursement.totalAmount
                            )
                        }
                        
                        // 建议列表
                        if (showSuggestions && suggestions.isNotEmpty()) {
                            item {
                                SuggestionsSection(suggestions = suggestions)
                            }
                        }
                        
                        // 问题列表
                        if (validationResult!!.issues.isNotEmpty()) {
                            item {
                                Text(
                                    text = "发现问题 (${validationResult!!.issues.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(validationResult!!.issues) { issue ->
                                IssueCard(
                                    issue = issue,
                                    onClick = {
                                        if (issue.severity == ComplianceValidationService.Severity.ERROR) {
                                            selectedIssue = issue.code
                                        }
                                        onFixIssue(issue.code)
                                    }
                                )
                            }
                        }
                        
                        // 警告列表
                        if (validationResult!!.warnings.isNotEmpty()) {
                            item {
                                Text(
                                    text = "警告提示 (${validationResult!!.warnings.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(validationResult!!.warnings) { warning ->
                                WarningCard(warning = warning)
                            }
                        }
                        
                        // 无问题时的提示
                        if (validationResult!!.issues.isEmpty() && validationResult!!.warnings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "恭喜！报销单完全符合规范",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Text(
                                            text = "可以提交审批",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplianceScoreCard(
    score: Int,
    isCompliant: Boolean,
    totalReceipts: Int,
    totalAmount: Double
) {
    val scoreColor = when {
        score >= 90 -> Color(0xFF4CAF50) // Green
        score >= 70 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "合规分数",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isCompliant) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isCompliant) "符合规范" else "存在问题",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalReceipts 张票据 • ¥${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IssueCard(
    issue: ComplianceValidationService.ValidationIssue,
    onClick: () -> Unit
) {
    val backgroundColor = when (issue.severity) {
        ComplianceValidationService.Severity.ERROR -> MaterialTheme.colorScheme.errorContainer
        ComplianceValidationService.Severity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        ComplianceValidationService.Severity.INFO -> MaterialTheme.colorScheme.secondaryContainer
    }
    
    val icon = when (issue.severity) {
        ComplianceValidationService.Severity.ERROR -> Icons.Default.Error
        ComplianceValidationService.Severity.WARNING -> Icons.Default.Warning
        ComplianceValidationService.Severity.INFO -> Icons.Default.Info
    }
    
    val iconColor = when (issue.severity) {
        ComplianceValidationService.Severity.ERROR -> MaterialTheme.colorScheme.error
        ComplianceValidationService.Severity.WARNING -> MaterialTheme.colorScheme.tertiary
        ComplianceValidationService.Severity.INFO -> MaterialTheme.colorScheme.secondary
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor
                )
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (issue.affectedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "受影响项目:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    issue.affectedItems.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            
            if (issue.severity == ComplianceValidationService.Severity.ERROR) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iconColor
                    )
                ) {
                    Text("立即修正")
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    warning: ComplianceValidationService.ValidationWarning
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = warning.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (warning.suggestion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = warning.suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionsSection(suggestions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "优化建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (index < suggestions.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}