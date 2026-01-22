package com.billmii.android.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Backup and Restore functionality
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupService: BackupService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()
    
    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()
    
    private val _showRestoreDialog = MutableStateFlow(false)
    val showRestoreDialog: StateFlow<Boolean> = _showRestoreDialog.asStateFlow()
    
    init {
        loadBackupHistory()
        loadLastBackupTime()
    }
    
    private fun loadBackupHistory() {
        viewModelScope.launch {
            try {
                val backups = backupService.getBackupHistory()
                _uiState.value = _uiState.value.copy(backupHistory = backups)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载备份历史失败: ${e.message}"
                )
            }
        }
    }
    
    private fun loadLastBackupTime() {
        viewModelScope.launch {
            try {
                val lastBackup = backupService.getLastBackup()
                _uiState.value = _uiState.value.copy(lastBackupTime = lastBackup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载上次备份时间失败: ${e.message}"
                )
            }
        }
    }
    
    fun showBackupOptions() {
        _showBackupDialog.value = true
    }
    
    fun hideBackupDialog() {
        _showBackupDialog.value = false
    }
    
    fun showRestoreOptions() {
        _showRestoreDialog.value = true
    }
    
    fun hideRestoreDialog() {
        _showRestoreDialog.value = false
    }
    
    fun toggleIncludeImages() {
        _uiState.value = _uiState.value.copy(
            includeImages = !_uiState.value.includeImages
        )
    }
    
    fun toggleIncrementalBackup() {
        _uiState.value = _uiState.value.copy(
            incrementalBackup = !_uiState.value.incrementalBackup
        )
    }
    
    fun createBackup(description: String) {
        hideBackupDialog()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "正在创建备份..."
            )
            
            try {
                val options = BackupService.BackupOptions(
                    includeImages = _uiState.value.includeImages,
                    incremental = _uiState.value.incrementalBackup,
                    description = description
                )
                
                val backupFile = backupService.createBackup(options)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "",
                    showSuccessDialog = true,
                    successMessage = "备份创建成功: ${backupFile.name}"
                )
                
                loadBackupHistory()
                loadLastBackupTime()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "",
                    error = "创建备份失败: ${e.message}"
                )
            }
        }
    }
    
    fun restoreFromBackup(backupId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "正在恢复数据..."
            )
            
            try {
                backupService.restoreBackup(backupId)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "",
                    showSuccessDialog = true,
                    successMessage = "数据恢复成功"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "",
                    error = "恢复数据失败: ${e.message}"
                )
            }
        }
    }
    
    fun restoreFromPath(filePath: String) {
        hideRestoreDialog()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingMessage = "正在恢复数据..."
            )
            
            try {
                backupService.restoreFromPath(filePath)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "",
                    showSuccessDialog = true,
                    successMessage = "数据恢复成功"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processingMessage = "",
                    error = "恢复数据失败: ${e.message}"
                )
            }
        }
    }
    
    fun deleteBackup(backupId: Long) {
        viewModelScope.launch {
            try {
                backupService.deleteBackup(backupId)
                loadBackupHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除备份失败: ${e.message}"
                )
            }
        }
    }
    
    fun hideSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            showSuccessDialog = false,
            successMessage = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BackupRestoreUiState(
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val lastBackupTime: java.util.Date? = null,
    val backupHistory: List<BackupInfo> = emptyList(),
    val includeImages: Boolean = true,
    val incrementalBackup: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

data class BackupInfo(
    val id: Long,
    val description: String?,
    val createdAt: java.util.Date,
    val size: Long,
    val filePath: String,
    val version: String
)