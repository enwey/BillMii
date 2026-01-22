package com.billmii.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billmii.android.ui.settings.viewmodel.SettingsViewModel

/**
 * Settings Screen - 设置界面
 * Application settings and configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    onBackupRestoreClick: () -> Unit = {},
    onArchivePathManagementClick: () -> Unit = {},
    onOperationLogClick: () -> Unit = {},
    onOcrTemplatesClick: () -> Unit = {},
    onClassificationRulesClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ocrMode by viewModel.ocrMode.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackup.collectAsStateWithLifecycle()
    val backupInterval by viewModel.backupInterval.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            // OCR Settings
            SectionHeader("OCR设置")
            Spacer(modifier = Modifier.height(8.dp))
            
            OCRModeSelector(
                selectedMode = ocrMode,
                onModeSelected = { viewModel.updateOcrMode(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Backup Settings
            SectionHeader("备份设置")
            Spacer(modifier = Modifier.height(8.dp))
            
            SwitchSetting(
                title = "自动备份",
                description = "定期自动备份票据数据",
                checked = autoBackup,
                onCheckedChange = { viewModel.updateAutoBackup(it) }
            )
            
            if (autoBackup) {
                Spacer(modifier = Modifier.height(8.dp))
                BackupIntervalSelector(
                    selectedInterval = backupInterval,
                    onIntervalSelected = { viewModel.updateBackupInterval(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Management
            SectionHeader("数据管理")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingItem(
                title = "备份与恢复",
                description = "备份和恢复应用数据",
                onClick = onBackupRestoreClick
            )
            
            SettingItem(
                title = "归档路径管理",
                description = "管理票据归档路径",
                onClick = onArchivePathManagementClick
            )
            
            SettingItem(
                title = "操作日志",
                description = "查看应用操作记录",
                onClick = onOperationLogClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Security Settings
            SectionHeader("安全设置")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingItem(
                title = "修改密码",
                description = "更改应用访问密码",
                onClick = { /* TODO: Show password change dialog */ }
            )
            
            SettingItem(
                title = "生物识别",
                description = "使用指纹或面容解锁",
                onClick = { /* TODO: Show biometric settings */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // OCR & Classification
            SectionHeader("OCR与分类")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingItem(
                title = "OCR模板",
                description = "管理自定义OCR识别模板",
                onClick = onOcrTemplatesClick
            )
            
            SettingItem(
                title = "分类规则",
                description = "管理票据自动分类规则",
                onClick = onClassificationRulesClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // About
            SectionHeader("关于")
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingItem(
                title = "版本信息",
                description = "BillMii v1.0.0",
                onClick = { /* TODO: Show version info */ }
            )
            
            SettingItem(
                title = "使用说明",
                description = "查看应用使用帮助",
                onClick = { /* TODO: Open help documentation */ }
            )
        }
    }
}

/**
 * Section Header
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/**
 * OCR Mode Selector
 */
@Composable
fun OCRModeSelector(
    selectedMode: OCRMode,
    onModeSelected: (OCRMode) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        OCRMode.values().forEach { mode ->
            RadioButtonItem(
                text = mode.displayName,
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

/**
 * Radio Button Item
 */
@Composable
fun RadioButtonItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text)
    }
}

/**
 * Switch Setting
 */
@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Setting Item
 */
@Composable
fun SettingItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Backup Interval Selector
 */
@Composable
fun BackupIntervalSelector(
    selectedInterval: BackupInterval,
    onIntervalSelected: (BackupInterval) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        BackupInterval.values().forEach { interval ->
            RadioButtonItem(
                text = interval.displayName,
                selected = selectedInterval == interval,
                onClick = { onIntervalSelected(interval) }
            )
        }
    }
}

/**
 * OCR Mode enumeration
 */
enum class OCRMode(val displayName: String) {
    PRECISE("精准模式"),
    FAST("快速模式")
}

/**
 * Backup Interval enumeration
 */
enum class BackupInterval(val displayName: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月")
}