package com.billmii.android.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.OcrTemplate
import com.billmii.android.data.model.OcrTemplateField
import com.billmii.android.data.service.OcrService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for OCR Template Management
 */
@HiltViewModel
class OcrTemplateViewModel @Inject constructor(
    private val ocrService: OcrService
) : ViewModel() {
    
    private val _templates = MutableStateFlow<List<OcrTemplate>>(emptyList())
    val templates: StateFlow<List<OcrTemplate>> = _templates.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()
    
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()
    
    var selectedTemplate: OcrTemplate? = null
        private set
    
    init {
        loadTemplates()
    }
    
    private fun loadTemplates() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _templates.value = ocrService.getAllTemplates()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun showCreateTemplateDialog() {
        _showCreateDialog.value = true
    }
    
    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }
    
    fun showEditTemplateDialog(template: OcrTemplate) {
        selectedTemplate = template
        _showEditDialog.value = true
    }
    
    fun hideEditDialog() {
        _showEditDialog.value = false
        selectedTemplate = null
    }
    
    fun createTemplate(name: String, description: String?, fields: List<OcrTemplateField>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val template = OcrTemplate(
                    name = name,
                    description = description,
                    fields = fields,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                ocrService.createTemplate(template)
                loadTemplates()
                hideCreateDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTemplate(template: OcrTemplate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedTemplate = template.copy(
                    updatedAt = System.currentTimeMillis()
                )
                ocrService.updateTemplate(updatedTemplate)
                loadTemplates()
                hideEditDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                ocrService.deleteTemplate(templateId)
                loadTemplates()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleTemplateEnabled(templateId: Long) {
        viewModelScope.launch {
            try {
                val template = ocrService.getTemplateById(templateId)
                if (template != null) {
                    val updatedTemplate = template.copy(
                        enabled = !template.enabled,
                        updatedAt = System.currentTimeMillis()
                    )
                    ocrService.updateTemplate(updatedTemplate)
                    loadTemplates()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}