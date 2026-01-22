package com.billmii.android.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.repository.ReceiptRepository
import com.billmii.android.data.repository.ReimbursementRepository
import com.billmii.android.data.service.LanIntegrationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanIntegrationViewModel @Inject constructor(
    private val lanIntegrationService: LanIntegrationService,
    private val receiptRepository: ReceiptRepository,
    private val reimbursementRepository: ReimbursementRepository
) : ViewModel() {
    
    data class UiState(
        val isConnected: Boolean = false,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val syncResult: LanIntegrationService.SyncResult? = null,
        val config: LanIntegrationService.ServerConfig? = null,
        val errorMessage: String? = null
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    val integrationStatus = lanIntegrationService.integrationStatus
    
    init {
        loadConfig()
        observeIntegrationStatus()
    }
    
    private fun observeIntegrationStatus() {
        viewModelScope.launch {
            integrationStatus.collect { status ->
                _uiState.value = _uiState.value.copy(
                    isConnected = status.isConnected,
                    lastSyncTime = status.lastSyncTime
                )
            }
        }
    }
    
    fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                config = lanIntegrationService.getCurrentConfig()
            )
        }
    }
    
    fun saveConfig(config: LanIntegrationService.ServerConfig) {
        viewModelScope.launch {
            try {
                lanIntegrationService.configureServer(config)
                _uiState.value = _uiState.value.copy(
                    config = config,
                    errorMessage = null
                )
                // Test connection after saving
                testConnection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "保存配置失败: ${e.message}"
                )
            }
        }
    }
    
    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                val isConnected = lanIntegrationService.testConnection()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = if (!isConnected) "连接失败" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "连接测试失败: ${e.message}"
                )
            }
        }
    }
    
    fun syncReceipts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncResult = null)
            try {
                val receipts = receiptRepository.getAllReceipts()
                val result = lanIntegrationService.syncReceipts(receipts)
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = result,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = LanIntegrationService.SyncResult(
                        success = false,
                        errors = listOf("同步失败: ${e.message}")
                    ),
                    errorMessage = "同步失败: ${e.message}"
                )
            }
        }
    }
    
    fun syncReimbursements() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncResult = null)
            try {
                val reimbursements = reimbursementRepository.getAllReimbursements()
                val result = lanIntegrationService.syncReimbursements(reimbursements)
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = result,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = LanIntegrationService.SyncResult(
                        success = false,
                        errors = listOf("同步失败: ${e.message}")
                    ),
                    errorMessage = "同步失败: ${e.message}"
                )
            }
        }
    }
    
    fun syncAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncResult = null)
            try {
                // Sync receipts
                val receipts = receiptRepository.getAllReceipts()
                val receiptResult = lanIntegrationService.syncReceipts(receipts)
                
                // Sync reimbursements
                val reimbursements = reimbursementRepository.getAllReimbursements()
                val reimbursementResult = lanIntegrationService.syncReimbursements(reimbursements)
                
                val combinedResult = LanIntegrationService.SyncResult(
                    success = receiptResult.success && reimbursementResult.success,
                    syncedCount = receiptResult.syncedCount + reimbursementResult.syncedCount,
                    failedCount = receiptResult.failedCount + reimbursementResult.failedCount,
                    errors = receiptResult.errors + reimbursementResult.errors
                )
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = combinedResult,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = LanIntegrationService.SyncResult(
                        success = false,
                        errors = listOf("同步失败: ${e.message}")
                    ),
                    errorMessage = "同步失败: ${e.message}"
                )
            }
        }
    }
    
    fun convertDataFormat(sourceFormat: String, targetFormat: String) {
        viewModelScope.launch {
            try {
                // This is a placeholder - in a real implementation, you would fetch data
                // from the financial software and convert it
                val result = lanIntegrationService.convertDataFormat(
                    data = "{}",
                    sourceFormat = sourceFormat,
                    targetFormat = targetFormat
                )
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        syncResult = LanIntegrationService.SyncResult(
                            success = true,
                            syncedCount = 1
                        ),
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        syncResult = LanIntegrationService.SyncResult(
                            success = false,
                            errors = listOf("转换失败: ${result.exceptionOrNull()?.message}")
                        ),
                        errorMessage = "转换失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncResult = LanIntegrationService.SyncResult(
                        success = false,
                        errors = listOf("转换失败: ${e.message}")
                    ),
                    errorMessage = "转换失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearConfig() {
        viewModelScope.launch {
            lanIntegrationService.clearConfiguration()
            _uiState.value = UiState(
                isConnected = false,
                isSyncing = false,
                lastSyncTime = null,
                syncResult = null,
                config = null,
                errorMessage = null
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}