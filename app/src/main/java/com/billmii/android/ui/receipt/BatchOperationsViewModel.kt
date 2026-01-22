package com.billmii.android.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.service.ClassificationService
import com.billmii.android.data.service.ExportService
import com.billmii.android.data.service.OcrService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Batch Operations
 */
@HiltViewModel
class BatchOperationsViewModel @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val classificationService: ClassificationService,
    private val exportService: ExportService,
    private val ocrService: OcrService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BatchOperationsUiState())
    val uiState: StateFlow<BatchOperationsUiState> = _uiState.asStateFlow()
    
    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()
    
    init {
        loadReceipts()
    }
    
    private fun loadReceipts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                receiptDao.getAllReceipts()
                    .collect { receipts ->
                        _uiState.value = _uiState.value.copy(
                            availableReceipts = receipts.map { it.toSummary() },
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun toggleSelection(receiptId: Long) {
        val current = _uiState.value.selectedReceipts.toMutableList()
        if (current.contains(receiptId)) {
            current.remove(receiptId)
        } else {
            current.add(receiptId)
        }
        
        val selectedAmount = calculateSelectedAmount(current)
        _uiState.value = _uiState.value.copy(
            selectedReceipts = current,
            selectedAmount = selectedAmount,
            allSelected = current.size == _uiState.value.availableReceipts.size
        )
    }
    
    fun selectAll() {
        val allIds = _uiState.value.availableReceipts.map { it.id }
        val totalAmount = _uiState.value.availableReceipts.sumOf { it.amount }
        _uiState.value = _uiState.value.copy(
            selectedReceipts = allIds,
            selectedAmount = totalAmount,
            allSelected = true
        )
    }
    
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedReceipts = emptyList(),
            selectedAmount = 0.0,
            allSelected = false
        )
    }
    
    private fun calculateSelectedAmount(selectedIds: List<Long>): Double {
        return _uiState.value.availableReceipts
            .filter { it.id in selectedIds }
            .sumOf { it.amount }
    }
    
    fun showExportDialog() {
        _showExportDialog.value = true
    }
    
    fun hideExportDialog() {
        _showExportDialog.value = false
    }
    
    fun batchDelete() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "删除中...")
            try {
                val selectedIds = _uiState.value.selectedReceipts
                receiptDao.deleteByIds(selectedIds)
                clearSelection()
                _uiState.value = _uiState.value.copy(isProcessing = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message
                )
            }
        }
    }
    
    fun batchExport(format: ExportFormat, includeImages: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "导出中...")
            hideExportDialog()
            try {
                val selectedIds = _uiState.value.selectedReceipts
                val receipts = selectedIds.mapNotNull { receiptDao.getById(it) }
                
                val exportFormat = when (format) {
                    ExportFormat.EXCEL -> com.billmii.android.data.service.ExportFormat.EXCEL
                    ExportFormat.PDF -> com.billmii.android.data.service.ExportFormat.PDF
                    ExportFormat.JSON -> com.billmii.android.data.service.ExportFormat.JSON
                    ExportFormat.CSV -> com.billmii.android.data.service.ExportFormat.CSV
                }
                
                exportService.exportReceipts(receipts, exportFormat, includeImages)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    exportSuccess = true
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message
                )
            }
        }
    }
    
    fun batchOcr() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "OCR识别中...")
            try {
                val selectedIds = _uiState.value.selectedReceipts
                val total = selectedIds.size
                
                selectedIds.forEachIndexed { index, id ->
                    val receipt = receiptDao.getById(id) ?: return@forEachIndexed
                    val imageFile = java.io.File(receipt.filePath)
                    
                    if (imageFile.exists()) {
                        val result = ocrService.recognizeReceipt(imageFile)
                        if (result.success) {
                            val updatedReceipt = receipt.copy(
                                receiptType = result.receiptType,
                                ocrStatus = com.billmii.android.data.model.OcrStatus.SUCCESS,
                                ocrConfidence = result.confidence,
                                updatedAt = java.util.Date()
                            )
                            receiptDao.update(updatedReceipt)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        processingProgress = (index + 1).toFloat() / total
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    ocrSuccess = true
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message
                )
            }
        }
    }
    
    fun batchClassify() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingMessage = "分类中...")
            try {
                val selectedIds = _uiState.value.selectedReceipts
                
                val result = classificationService.batchClassifyReceipts(selectedIds)
                
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    classificationSuccess = true,
                    classificationResult = result
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = e.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            exportSuccess = false,
            ocrSuccess = false,
            classificationSuccess = false
        )
    }
}

// UI State
data class BatchOperationsUiState(
    val availableReceipts: List<ReceiptSummary> = emptyList(),
    val selectedReceipts: List<Long> = emptyList(),
    val selectedAmount: Double = 0.0,
    val allSelected: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val processingProgress: Float = 0f,
    val error: String? = null,
    val exportSuccess: Boolean = false,
    val ocrSuccess: Boolean = false,
    val classificationSuccess: Boolean = false,
    val classificationResult: com.billmii.android.data.service.BatchClassificationResult? = null
)

// Extension function
private fun com.billmii.android.data.model.Receipt.toSummary(): ReceiptSummary {
    return ReceiptSummary(
        id = id,
        receiptType = receiptType,
        sellerName = sellerName,
        amount = totalAmount ?: amount ?: 0.0
    )
}