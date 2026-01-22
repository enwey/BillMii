package com.billmii.android.ui.settings

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
 * Archive Path Management Screen
 * Allows users to manage archive paths for organizing receipts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivePathManagementScreen(
    onBack: () -> Unit,
    viewModel: ArchivePathManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("归档路径管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddPathDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "添加路径")
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
            // Archive paths list
            if (uiState.archivePaths.isEmpty()) {
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
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "暂无归档路径",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "点击右上角 + 添加归档路径",
                            style = MaterialTheme.typography.bodyMedium,
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
                    items(uiState.archivePaths) { path ->
                        ArchivePathCard(
                            path = path,
                            onEdit = { viewModel.editPath(path) },
                            onDelete = { viewModel.deletePath(path.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Add Archive Path Dialog
    if (showAddDialog) {
        ArchivePathDialog(
            title = "添加归档路径",
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, description, parentPathId ->
                viewModel.createPath(name, description, parentPathId)
            },
            availablePaths = uiState.archivePaths
        )
    }
    
    // Edit Archive Path Dialog
    if (showEditDialog) {
        uiState.editingPath?.let { path ->
            ArchivePathDialog(
                title = "编辑归档路径",
                initialName = path.name,
                initialDescription = path.description,
                initialParentId = path.parentPathId,
                onDismiss = { viewModel.hideEditDialog() },
                onConfirm = { name, description, parentPathId ->
                    viewModel.updatePath(path.id, name, description, parentPathId)
                },
                availablePaths = uiState.archivePaths.filter { it.id != path.id }
            )
        }
    }
    
    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("确认删除") },
            text = { Text("确定要删除此归档路径吗？删除后，该路径下的所有收据将被移动到默认路径。") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Error Dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("操作失败") },
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
fun ArchivePathCard(
    path: ArchivePathInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = path.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        path.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Row {
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "收据数量: ${path.receiptCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "归档号前缀: ${path.archiveNumberPrefix ?: "默认"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ArchivePathDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialParentId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long?) -> Unit,
    availablePaths: List<ArchivePathInfo>
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var parentPathId by remember { mutableStateOf(initialParentId) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = if (it.isBlank()) "路径名称不能为空" else null
                    },
                    label = { Text("路径名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } }
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = if (parentPathId != null) {
                            availablePaths.find { it.id == parentPathId }?.name ?: "根路径"
                        } else {
                            "根路径"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("父路径") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("根路径") },
                            onClick = {
                                parentPathId = null
                                expanded = false
                            }
                        )
                        availablePaths.forEach { path ->
                            DropdownMenuItem(
                                text = { Text(path.name) },
                                onClick = {
                                    parentPathId = path.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "路径名称不能为空"
                    } else {
                        onConfirm(name, description, parentPathId)
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
data class ArchivePathInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val parentPathId: Long?,
    val archiveNumberPrefix: String?,
    val receiptCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)