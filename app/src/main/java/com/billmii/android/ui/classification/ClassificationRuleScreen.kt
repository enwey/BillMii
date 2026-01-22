package com.billmii.android.ui.classification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Classification Rule Management Screen
 * Allows users to create and manage classification rules for automatic receipt categorization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationRuleScreen(
    viewModel: ClassificationRuleViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类规则管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateRuleDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建规则")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (rules.isEmpty()) {
            EmptyRulesView(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rules) { rule ->
                    RuleCard(
                        rule = rule,
                        onEdit = { viewModel.showEditRuleDialog(rule) },
                        onDelete = { viewModel.deleteRule(rule) },
                        onToggleEnabled = { viewModel.toggleRule(rule.id) },
                        onMoveUp = { if (rule.priority > 0) viewModel.moveRule(rule.id, rule.priority - 1) },
                        onMoveDown = { viewModel.moveRule(rule.id, rule.priority + 1) },
                        canMoveUp = rule.priority > 0,
                        canMoveDown = rule.priority < rules.size - 1
                    )
                }
            }
        }
    }
    
    // Create Rule Dialog
    if (showCreateDialog) {
        CreateRuleDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, description, conditions, actions ->
                viewModel.createRule(name, description, conditions, actions)
            }
        )
    }
    
    // Edit Rule Dialog
    if (showEditDialog) {
        EditRuleDialog(
            rule = viewModel.selectedRule,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { updatedRule ->
                viewModel.updateRule(updatedRule)
            }
        )
    }
}

@Composable
fun RuleCard(
    rule: ClassificationRuleUI,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${rule.priority + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            rule.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "条件: ${rule.conditions.size} | 动作: ${rule.actions.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
                    }
                }
                
                Row {
                    TextButton(onClick = onEdit) {
                        Text("编辑")
                    }
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyRulesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "暂无分类规则",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角按钮创建自定义规则",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, List<ConditionUI>, List<ActionUI>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf(listOf<ConditionUI>()) }
    var actions by remember { mutableStateOf(listOf<ActionUI>()) }
    var showAddConditionDialog by remember { mutableStateOf(false) }
    var showAddActionDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建分类规则") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("规则名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                Divider()
                
                Text(
                    text = "条件 (${conditions.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (conditions.isEmpty()) {
                    Text(
                        text = "点击下方按钮添加条件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    conditions.forEachIndexed { index, condition ->
                        ConditionItem(
                            condition = condition,
                            onRemove = {
                                conditions = conditions.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }
                
                Button(
                    onClick = { showAddConditionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加条件")
                }
                
                Divider()
                
                Text(
                    text = "动作 (${actions.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (actions.isEmpty()) {
                    Text(
                        text = "点击下方按钮添加动作",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    actions.forEachIndexed { index, action ->
                        ActionItem(
                            action = action,
                            onRemove = {
                                actions = actions.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }
                
                Button(
                    onClick = { showAddActionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加动作")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && conditions.isNotEmpty() && actions.isNotEmpty()) {
                        onConfirm(name, description.ifBlank { null }, conditions, actions)
                    }
                },
                enabled = name.isNotBlank() && conditions.isNotEmpty() && actions.isNotEmpty()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    if (showAddConditionDialog) {
        AddConditionDialog(
            onDismiss = { showAddConditionDialog = false },
            onConfirm = { condition ->
                conditions = conditions + condition
                showAddConditionDialog = false
            }
        )
    }
    
    if (showAddActionDialog) {
        AddActionDialog(
            onDismiss = { showAddActionDialog = false },
            onConfirm = { action ->
                actions = actions + action
                showAddActionDialog = false
            }
        )
    }
}

@Composable
fun ConditionItem(
    condition: ConditionUI,
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
                    text = "${condition.field.displayName} ${condition.operator.displayName} ${condition.value}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除条件",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ActionItem(
    action: ActionUI,
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
                    text = "${action.type.displayName}: ${action.value}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除动作",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddConditionDialog(
    onDismiss: () -> Unit,
    onConfirm: (ConditionUI) -> Unit
) {
    var field by remember { mutableStateOf(ClassificationField.RECEIPT_TYPE) }
    var operator by remember { mutableStateOf(ConditionOperator.EQUALS) }
    var value by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加条件") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "字段",
                    style = MaterialTheme.typography.titleSmall
                )
                
                ClassificationField.values().forEach { f ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = field == f,
                            onClick = { field = f }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(f.displayName)
                    }
                }
                
                Text(
                    text = "操作符",
                    style = MaterialTheme.typography.titleSmall
                )
                
                ConditionOperator.values().forEach { op ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = operator == op,
                            onClick = { operator = op }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(op.displayName)
                    }
                }
                
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("值") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onConfirm(ConditionUI(field, operator, value))
                    }
                },
                enabled = value.isNotBlank()
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

@Composable
fun AddActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (ActionUI) -> Unit
) {
    var actionType by remember { mutableStateOf(ActionType.SET_CATEGORY) }
    var value by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加动作") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "动作类型",
                    style = MaterialTheme.typography.titleSmall
                )
                
                ActionType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = actionType == type,
                            onClick = { actionType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.displayName)
                    }
                }
                
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("值") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onConfirm(ActionUI(actionType, value))
                    }
                },
                enabled = value.isNotBlank()
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

@Composable
fun EditRuleDialog(
    rule: ClassificationRuleUI?,
    onDismiss: () -> Unit,
    onConfirm: (ClassificationRuleUI) -> Unit
) {
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var conditions by remember { mutableStateOf(rule?.conditions ?: listOf()) }
    var actions by remember { mutableStateOf(rule?.actions ?: listOf()) }
    var showAddConditionDialog by remember { mutableStateOf(false) }
    var showAddActionDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类规则") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("规则名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                Divider()
                
                Text(
                    text = "条件 (${conditions.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                conditions.forEachIndexed { index, condition ->
                    ConditionItem(
                        condition = condition,
                        onRemove = {
                            conditions = conditions.toMutableList().apply { removeAt(index) }
                        }
                    )
                }
                
                Button(
                    onClick = { showAddConditionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加条件")
                }
                
                Divider()
                
                Text(
                    text = "动作 (${actions.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                actions.forEachIndexed { index, action ->
                    ActionItem(
                        action = action,
                        onRemove = {
                            actions = actions.toMutableList().apply { removeAt(index) }
                        }
                    )
                }
                
                Button(
                    onClick = { showAddActionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加动作")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && rule != null) {
                        onConfirm(rule.copy(name = name, description = description, conditions = conditions, actions = actions))
                    }
                },
                enabled = name.isNotBlank() && rule != null
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
    
    if (showAddConditionDialog) {
        AddConditionDialog(
            onDismiss = { showAddConditionDialog = false },
            onConfirm = { condition ->
                conditions = conditions + condition
                showAddConditionDialog = false
            }
        )
    }
    
    if (showAddActionDialog) {
        AddActionDialog(
            onDismiss = { showAddActionDialog = false },
            onConfirm = { action ->
                actions = actions + action
                showAddActionDialog = false
            }
        )
    }
}

// UI Models
data class ClassificationRuleUI(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val conditions: List<ConditionUI> = emptyList(),
    val actions: List<ActionUI> = emptyList(),
    val enabled: Boolean = true,
    val priority: Int = 0
)

data class ConditionUI(
    val field: ClassificationField,
    val operator: ConditionOperator,
    val value: String
)

data class ActionUI(
    val type: ActionType,
    val value: String
)

// Extension properties for display names
val ClassificationField.displayName: String
    get() = when (this) {
        ClassificationField.RECEIPT_TYPE -> "票据类型"
        ClassificationField.RECEIPT_CATEGORY -> "分类"
        ClassificationField.SELLER_NAME -> "销售方"
        ClassificationField.BUYER_NAME -> "购买方"
        ClassificationField.AMOUNT -> "金额"
        ClassificationField.DATE -> "日期"
        ClassificationField.ISSUER -> "开票方"
        ClassificationField.EXPENSE_TYPE -> "费用类型"
        ClassificationField.DEPARTURE_PLACE -> "出发地"
        ClassificationField.DESTINATION -> "目的地"
        ClassificationField.FILE_TYPE -> "文件类型"
    }

val ConditionOperator.displayName: String
    get() = when (this) {
        ConditionOperator.EQUALS -> "等于"
        ConditionOperator.CONTAINS -> "包含"
        ConditionOperator.STARTS_WITH -> "开始于"
        ConditionOperator.ENDS_WITH -> "结束于"
        ConditionOperator.GREATER_THAN -> "大于"
        ConditionOperator.LESS_THAN -> "小于"
        ConditionOperator.REGEX -> "正则匹配"
        ConditionOperator.IN -> "在列表中"
        ConditionOperator.NOT_EQUALS -> "不等于"
        ConditionOperator.NOT_CONTAINS -> "不包含"
    }

val ActionType.displayName: String
    get() = when (this) {
        ActionType.SET_CATEGORY -> "设置分类"
        ActionType.SET_SUB_CATEGORY -> "设置子分类"
        ActionType.SET_EXPENSE_TYPE -> "设置费用类型"
        ActionType.SET_DEPARTMENT -> "设置部门"
        ActionType.SET_PROJECT -> "设置项目"
        ActionType.SET_TAG -> "添加标签"
        ActionType.ARCHIVE -> "归档"
        ActionType.GENERATE_ARCHIVE_NUMBER -> "生成归档号"
    }