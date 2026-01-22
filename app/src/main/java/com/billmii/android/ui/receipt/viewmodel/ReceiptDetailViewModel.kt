package com.billmii.android.ui.receipt.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.repository.ReceiptRepository
import com.billmii.android.data.service.OcrService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Receipt Detail ViewModel
 * Manages receipt detail state and operations
 */
@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val ocrService: OcrService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Receipt ID from navigation
    private val receiptId: Long = savedStateHandle.get<Long>("receiptId") ?: 0L
    
    // UI State
    private val _receipt = MutableStateFlow<Receipt?>(null)
    val receipt: StateFlow<Receipt?> = _receipt.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()
    
    private val _editedFields = mutableMapOf<String, Any>()
    
    init {
        if (receiptId > 0) {
            loadReceipt(receiptId)
        }
    }
    
    /**
     * Load receipt by ID
     */
    fun loadReceipt(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedReceipt = receiptRepository.getReceiptById(id)
                _receipt.value = loadedReceipt
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle editing mode
     */
    fun toggleEditing() {
        _isEditing.value = !_isEditing.value
        if (!_isEditing.value) {
            _editedFields.clear()
        }
    }
    
    /**
     * Update field value
     */
    fun updateField(field: String, value: String) {
        _editedFields[field] = value
        val currentReceipt = _receipt.value ?: return
        
        // Update receipt in memory (not saved yet)
        _receipt.value = when (field) {
            "invoiceCode" -> currentReceipt.copy(invoiceCode = value)
            "invoiceNumber" -> currentReceipt.copy(invoiceNumber = value)
            "buyerName" -> currentReceipt.copy(buyerName = value)
            "buyerTaxId" -> currentReceipt.copy(buyerTaxId = value)
            "sellerName" -> currentReceipt.copy(sellerName = value)
            "sellerTaxId" -> currentReceipt.copy(sellerTaxId = value)
            "remarks" -> currentReceipt.copy(remarks = value)
            else -> currentReceipt
        }
    }
    
    /**
     * Save receipt changes
     */
    fun saveReceipt() {
        val currentReceipt = _receipt.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedReceipt = currentReceipt.copy(
                    updatedAt = java.util.Date()
                )
                receiptRepository.updateReceipt(updatedReceipt)
                _isEditing.value = false
                _editedFields.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete receipt
     */
    fun deleteReceipt() {
        val currentReceipt = _receipt.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                receiptRepository.deleteReceipt(currentReceipt)
                // Navigation will be handled by the UI
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Perform OCR on receipt
     */
    fun performOcr() {
        val currentReceipt = _receipt.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = File(currentReceipt.filePath)
                if (file.exists()) {
                    val result = ocrService.recognizeReceipt(file)
                    if (result.isSuccess) {
                        val recognizedReceipt = result.getOrNull()
                        if (recognizedReceipt != null) {
                            // Update receipt with OCR results
                            val updatedReceipt = currentReceipt.copy(
                                receiptType = recognizedReceipt.receiptType,
                                receiptCategory = recognizedReceipt.receiptCategory,
                                expenseSubCategory = recognizedReceipt.expenseSubCategory,
                                invoiceCode = recognizedReceipt.invoiceCode,
                                invoiceNumber = recognizedReceipt.invoiceNumber,
                                invoiceDate = recognizedReceipt.invoiceDate,
                                buyerName = recognizedReceipt.buyerName,
                                buyerTaxId = recognizedReceipt.buyerTaxId,
                                sellerName = recognizedReceipt.sellerName,
                                sellerTaxId = recognizedReceipt.sellerTaxId,
                                totalAmount = recognizedReceipt.totalAmount,
                                amountWithoutTax = recognizedReceipt.amountWithoutTax,
                                taxRate = recognizedReceipt.taxRate,
                                taxAmount = recognizedReceipt.taxAmount,
                                invoiceStatus = recognizedReceipt.invoiceStatus,
                                expenseDate = recognizedReceipt.expenseDate,
                                departurePlace = recognizedReceipt.departurePlace,
                                destination = recognizedReceipt.destination,
                                expenseType = recognizedReceipt.expenseType,
                                issuer = recognizedReceipt.issuer,
                                amount = recognizedReceipt.amount,
                                ocrStatus = com.billmii.android.data.model.OcrStatus.SUCCESS,
                                recognizedAt = java.util.Date(),
                                updatedAt = java.util.Date()
                            )
                            receiptRepository.updateReceipt(updatedReceipt)
                            _receipt.value = updatedReceipt
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Link to reimbursement
     */
    fun linkToReimbursement(reimbursementId: Long) {
        val currentReceipt = _receipt.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                receiptRepository.linkToReimbursement(currentReceipt.id, reimbursementId)
                loadReceipt(currentReceipt.id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Unlink from reimbursement
     */
    fun unlinkFromReimbursement() {
        val currentReceipt = _receipt.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                receiptRepository.unlinkFromReimbursement(currentReceipt.id)
                loadReceipt(currentReceipt.id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}