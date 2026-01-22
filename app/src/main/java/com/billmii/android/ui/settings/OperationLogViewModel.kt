package com.billmii.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.database.dao.OperationLogDao
import com.billmii.android.data.model.OperationLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Operation Log
 */
@HiltViewModel
class OperationLogViewModel @Inject constructor(
    private val operationLogDao: OperationLogDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OperationLogUiState())
    val uiState: StateFlow<OperationLogUiState> = _uiState.asStateFlow()
    
    init {
        loadLogs()
    }
    
    private fun loadLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                operationLogDao.getAllLogs()
                    .collect { logs ->
                        _uiState.value = _uiState.value.copy(
                            logs = logs.map { it.toLogEntry() },
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun exportLogs() {
        viewModelScope.launch {
            try {
                val logs = _uiState.value.logs
                // TODO: Implement actual export to file
                _uiState.value = _uiState.value.copy(exportSuccess = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun showClearConfirm() {
        _uiState.value = _uiState.value.copy(showClearConfirm = true)
    }
    
    fun hideClearConfirm() {
        _uiState.value = _uiState.value.copy(showClearConfirm = false)
    }
    
    fun confirmClearLogs() {
        viewModelScope.launch {
            try {
                operationLogDao.deleteAll()
                _uiState.value = _uiState.value.copy(
                    logs = emptyList(),
                    showClearConfirm = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    showClearConfirm = false
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }
}

// UI State
data class OperationLogUiState(
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val showClearConfirm: Boolean = false,
    val exportSuccess: Boolean = false,
    val error: String? = null
)

// Extension function
private fun OperationLog.toLogEntry(): LogEntry {
    return LogEntry(
        id = id,
        action = action,
        entity = entity,
        entityId = entityId,
        details = details,
        status = when (status) {
            com.billmii.android.data.model.LogStatus.SUCCESS -> LogStatus.SUCCESS
            com.billmii.android.data.model.LogStatus.FAILED -> LogStatus.FAILED
            com.billmii.android.data.model.LogStatus.WARNING -> LogStatus.WARNING
        },
        timestamp = timestamp
    )
}