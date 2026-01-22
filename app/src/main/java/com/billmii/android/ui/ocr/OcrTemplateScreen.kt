package com.billmii.android.ui.ocr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * OCR Template Customization Screen
 * Allows users to create and manage custom OCR templates for different receipt types
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrTemplateScreen(
    viewModel: OcrTemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR模板管理") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateTemplateDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建模板")
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
        } else if (templates.isEmpty()) {
            EmptyTemplatesView(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onEdit = { viewModel.showEditTemplateDialog(template) },
                        onDelete = { viewModel.deleteTemplate(template.id) },
                        onToggleEnabled = { 
                            viewModel.toggleTemplateEnabled(template.id)
                        }
                    )
                }
            }
        }
    }
    
    // Create Template Dialog
    if (showCreateDialog) {
        CreateTemplateDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, description, fields ->
                viewModel.createTemplate(name, description, fields)
            }
        )
    }
    
    // Edit Template Dialog
    if (showEditDialog) {
        EditTemplateDialog(
            template = viewModel.selectedTemplate,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { updatedTemplate ->
                viewModel.updateTemplate(updatedTemplate)
            }
        )
    }
}

@Composable
fun TemplateCard(
    template: OcrTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
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
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = template.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            template.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "字段数量: ${template.fields.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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

@Composable
fun EmptyTemplatesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "暂无OCR模板",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角按钮创建自定义模板",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateTemplateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, List<OcrTemplateField>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fields by remember { mutableStateOf(listOf<OcrTemplateField>()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建OCR模板") },
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
                    label = { Text("模板名称") },
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
                    text = "模板字段 (${fields.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                if (fields.isEmpty()) {
                    Text(
                        text = "点击下方按钮添加字段",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    fields.forEachIndexed { index, field ->
                        FieldItem(
                            field = field,
                            onRemove = {
                                fields = fields.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }
                
                Button(
                    onClick = { showAddFieldDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加字段")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description.ifBlank { null }, fields)
                    }
                },
                enabled = name.isNotBlank()
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
    
    if (showAddFieldDialog) {
        AddFieldDialog(
            onDismiss = { showAddFieldDialog = false },
            onConfirm = { field ->
                fields = fields + field
                showAddFieldDialog = false
            }
        )
    }
}

@Composable
fun FieldItem(
    field: OcrTemplateField,
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
                    text = field.fieldName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "关键词: ${field.keyword}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除字段",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (OcrTemplateField) -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var extractionType by remember { mutableStateOf(ExtractionType.TEXT) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加字段") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = { Text("字段名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("提取关键词") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "提取类型",
                    style = MaterialTheme.typography.titleSmall
                )
                
                ExtractionType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = extractionType == type,
                            onClick = { extractionType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (type) {
                                ExtractionType.TEXT -> "文本"
                                ExtractionType.AMOUNT -> "金额"
                                ExtractionType.DATE -> "日期"
                                ExtractionType.NUMBER -> "数字"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fieldName.isNotBlank() && keyword.isNotBlank()) {
                        onConfirm(
                            OcrTemplateField(
                                fieldName = fieldName,
                                keyword = keyword,
                                extractionType = extractionType
                            )
                        )
                    }
                },
                enabled = fieldName.isNotBlank() && keyword.isNotBlank()
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
fun EditTemplateDialog(
    template: OcrTemplate?,
    onDismiss: () -> Unit,
    onConfirm: (OcrTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var fields by remember { mutableStateOf(template?.fields ?: listOf()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑OCR模板") },
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
                    label = { Text("模板名称") },
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
                    text = "模板字段 (${fields.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                fields.forEachIndexed { index, field ->
                    FieldItem(
                        field = field,
                        onRemove = {
                            fields = fields.toMutableList().apply { removeAt(index) }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && template != null) {
                        onConfirm(
                            template.copy(
                                name = name,
                                description = description.ifBlank { null },
                                fields = fields
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && template != null
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