package com.billmii.android.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.ArchivePath
import com.billmii.android.data.repository.ArchivePathRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Archive Path Management functionality
 */
@HiltViewModel
class ArchivePathManagementViewModel @Inject constructor(
    private val archivePathRepository: ArchivePathRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ArchivePathManagementUiState())
    val uiState: StateFlow<ArchivePathManagementUiState> = _uiState.asStateFlow()
    
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()
    
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()
    
    init {
        loadArchivePaths()
    }
    
    private fun loadArchivePaths() {
        viewModelScope.launch {
            try {
                archivePathRepository.getAllArchivePaths().collect { paths ->
                    val pathInfos = paths.map { it.toArchivePathInfo() }
                    _uiState.value = _uiState.value.copy(archivePaths = pathInfos)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载归档路径失败: ${e.message}"
                )
            }
        }
    }
    
    fun showAddPathDialog() {
        _showAddDialog.value = true
    }
    
    fun hideAddDialog() {
        _showAddDialog.value = false
    }
    
    fun editPath(path: ArchivePathInfo) {
        _uiState.value = _uiState.value.copy(editingPath = path)
        _showEditDialog.value = true
    }
    
    fun hideEditDialog() {
        _showEditDialog.value = false
        _uiState.value = _uiState.value.copy(editingPath = null)
    }
    
    fun createPath(name: String, description: String, parentPathId: Long?) {
        viewModelScope.launch {
            try {
                val archivePath = ArchivePath(
                    name = name,
                    description = description.ifBlank { null },
                    parentPathId = parentPathId,
                    archiveNumberPrefix = null // Use default prefix
                )
                
                archivePathRepository.createArchivePath(archivePath)
                hideAddDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "创建归档路径失败: ${e.message}"
                )
            }
        }
    }
    
    fun updatePath(id: Long, name: String, description: String, parentPathId: Long?) {
        viewModelScope.launch {
            try {
                archivePathRepository.updateArchivePath(
                    id = id,
                    name = name,
                    description = description.ifBlank { null },
                    parentPathId = parentPathId
                )
                hideEditDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新归档路径失败: ${e.message}"
                )
            }
        }
    }
    
    fun deletePath(id: Long) {
        _uiState.value = _uiState.value.copy(
            deletingPathId = id,
            showDeleteConfirmDialog = true
        )
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            deletingPathId = null,
            showDeleteConfirmDialog = false
        )
    }
    
    fun confirmDelete() {
        val pathId = _uiState.value.deletingPathId ?: return
        
        viewModelScope.launch {
            try {
                archivePathRepository.deleteArchivePath(pathId)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除归档路径失败: ${e.message}"
                )
                hideDeleteDialog()
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ArchivePathManagementUiState(
    val archivePaths: List<ArchivePathInfo> = emptyList(),
    val editingPath: ArchivePathInfo? = null,
    val deletingPathId: Long? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val error: String? = null
)

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

// Extension function
fun ArchivePath.toArchivePathInfo(): ArchivePathInfo {
    return ArchivePathInfo(
        id = id,
        name = name,
        description = description,
        parentPathId = parentPathId,
        archiveNumberPrefix = archiveNumberPrefix,
        receiptCount = receiptCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}