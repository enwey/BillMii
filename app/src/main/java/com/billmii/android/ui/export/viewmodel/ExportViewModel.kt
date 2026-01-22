package com.billmii.android.ui.export.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.service.ExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Export functionality
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportService: ExportService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    private val _showFormatDialog = MutableStateFlow(false)
    val showFormatDialog: StateFlow<Boolean> = _showFormatDialog.asStateFlow()
    
    private val _showTemplateDialog = MutableStateFlow(false)
    val showTemplateDialog: StateFlow<Boolean> = _showTemplateDialog.asStateFlow()
    
    init {
        loadExportTemplates()
        loadExportHistory()
    }
    
    private fun loadExportTemplates() {
        viewModelScope.launch {
            try {
                val templates = exportService.getAllExportTemplates()
                _uiState.value = _uiState.value.copy(exportTemplates = templates)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载导出模板失败: ${e.message}"
                )
            }
        }
    }
    
    private fun loadExportHistory() {
        viewModelScope.launch {
            try {
                val history = exportService.getExportHistory()
                _uiState.value = _uiState.value.copy(exportHistory = history)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载导出历史失败: ${e.message}"
                )
            }
        }
    }
    
    fun showFormatOptions() {
        _showFormatDialog.value = true
    }
    
    fun hideFormatDialog() {
        _showFormatDialog.value = false
    }
    
    fun showTemplateOptions() {
        _showTemplateDialog.value = true
    }
    
    fun hideTemplateDialog() {
        _showTemplateDialog.value = false
        _uiState.value = _uiState.value.copy(editingTemplate = null)
    }
    
    fun editTemplate(template: ExportTemplate) {
        _uiState.value = _uiState.value.copy(editingTemplate = template)
        _showTemplateDialog.value = true
    }
    
    fun exportToExcel(type: ExportType) {
        performExport(ExportFormat.EXCEL, type)
    }
    
    fun exportToPdf(type: ExportType) {
        performExport(ExportFormat.PDF, type)
    }
    
    fun exportToCsv(type: ExportType) {
        performExport(ExportFormat.CSV, type)
    }
    
    fun exportToJson(type: ExportType) {
        performExport(ExportFormat.JSON, type)
    }
    
    fun exportWithFormat(format: ExportFormat, dataRange: DataRange, includeImages: Boolean) {
        hideFormatDialog()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportMessage = "正在导出数据...",
                exportProgress = 0
            )
            
            try {
                val options = ExportService.ExportOptions(
                    format = format,
                    dataRange = dataRange,
                    includeImages = includeImages
                )
                
                exportService.exportData(options) { progress ->
                    _uiState.value = _uiState.value.copy(exportProgress = progress)
                }
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "",
                    exportProgress = 0,
                    showSuccessDialog = true,
                    successMessage = "数据导出成功"
                )
                
                loadExportHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "",
                    exportProgress = 0,
                    error = "导出失败: ${e.message}"
                )
            }
        }
    }
    
    fun exportWithTemplate(template: ExportTemplate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportMessage = "正在使用模板导出数据...",
                exportProgress = 0
            )
            
            try {
                exportService.exportWithTemplate(template.id) { progress ->
                    _uiState.value = _uiState.value.copy(exportProgress = progress)
                }
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "",
                    exportProgress = 0,
                    showSuccessDialog = true,
                    successMessage = "使用模板导出成功"
                )
                
                loadExportHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "",
                    exportProgress = 0,
                    error = "模板导出失败: ${e.message}"
                )
            }
        }
    }
    
    fun saveTemplate(template: ExportTemplate) {
        viewModelScope.launch {
            try {
                if (template.id == 0L) {
                    exportService.createExportTemplate(template)
                } else {
                    exportService.updateExportTemplate(template)
                }
                
                hideTemplateDialog()
                loadExportTemplates()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "保存模板失败: ${e.message}"
                )
            }
        }
    }
    
    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                exportService.deleteExportTemplate(templateId)
                loadExportTemplates()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除模板失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearExportHistory() {
        viewModelScope.launch {
            try {
                exportService.clearExportHistory()
                loadExportHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "清空导出历史失败: ${e.message}"
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
    
    private fun performExport(format: ExportFormat, type: ExportType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportMessage = "正在导出数据...",
                exportProgress = 0
            )
            
            try {
                val options = ExportService.ExportOptions(
                    format = format,
                    dataRange = DataRange.ALL,
                    includeImages = false
                )
                
                exportService.exportData(options) { progress ->
                    _uiState.value = _uiState.value.copy(exportProgress = progress)
                }
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "",
                    exportProgress = 0,
                    showSuccessDialog = true,
                    successMessage = "数据导出成功"
                )
                
                loadExportHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "",
                    exportProgress = 0,
                    error = "导出失败: ${e.message}"
                )
            }
        }
    }
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportMessage: String = "",
    val exportProgress: Int = 0,
    val exportTemplates: List<ExportTemplate> = emptyList(),
    val exportHistory: List<ExportRecord> = emptyList(),
    val editingTemplate: ExportTemplate? = null,
    val showSuccessDialog: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

enum class ExportFormat {
    EXCEL,
    PDF,
    CSV,
    JSON
}

enum class ExportType {
    QUICK,
    CUSTOM
}

enum class DataRange {
    ALL,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

data class ExportTemplate(
    val id: Long,
    val name: String,
    val format: ExportFormat,
    val selectedFields: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

data class ExportRecord(
    val id: Long,
    val fileName: String,
    val format: String,
    val fileSize: Long,
    val timestamp: Long,
    val filePath: String
)