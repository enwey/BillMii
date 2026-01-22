package com.billmii.android.ui.reimbursement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.model.ReimbursementStatus
import com.billmii.android.data.model.ReceiptType
import com.billmii.android.data.repository.ReceiptRepository
import com.billmii.android.data.repository.ReimbursementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CreateReimbursementViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val reimbursementRepository: ReimbursementRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateReimbursementUiState())
    val uiState: StateFlow<CreateReimbursementUiState> = _uiState.asStateFlow()
    
    private val _showReceiptSelector = MutableStateFlow(false)
    val showReceiptSelector: StateFlow<Boolean> = _showReceiptSelector.asStateFlow()
    
    private val _showValidationError = MutableStateFlow(false)
    val showValidationError: StateFlow<Boolean> = _showValidationError.asStateFlow()
    
    init {
        loadAvailableReceipts()
    }
    
    private fun loadAvailableReceipts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val receipts = receiptRepository.getAllReceipts()
                    .filter { it.reimbursementId == null }
                    .map { receipt ->
                        ReceiptSummary(
                            id = receipt.id,
                            receiptType = receipt.type,
                            sellerName = receipt.merchant,
                            amount = receipt.amount
                        )
                    }
                _uiState.value = _uiState.value.copy(
                    availableReceipts = receipts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    fun updateApplicant(applicant: String) {
        _uiState.value = _uiState.value.copy(applicant = applicant)
    }
    
    fun updateDepartment(department: String) {
        _uiState.value = _uiState.value.copy(department = department)
    }
    
    fun updateProject(project: String) {
        _uiState.value = _uiState.value.copy(project = project)
    }
    
    fun updateBudgetCode(budgetCode: String) {
        _uiState.value = _uiState.value.copy(budgetCode = budgetCode)
    }
    
    fun showReceiptSelectorDialog() {
        _showReceiptSelector.value = true
    }
    
    fun hideReceiptSelectorDialog() {
        _showReceiptSelector.value = false
    }
    
    fun updateSelectedReceipts(receipts: List<ReceiptSummary>) {
        _uiState.value = _uiState.value.copy(
            selectedReceipts = receipts,
            totalAmount = receipts.sumOf { it.amount },
            taxAmount = receipts.sumOf { it.amount * 0.06 } // Assume 6% tax rate
        )
    }
    
    fun removeReceipt(receiptId: Long) {
        val updatedReceipts = _uiState.value.selectedReceipts.filter { it.id != receiptId }
        updateSelectedReceipts(updatedReceipts)
    }
    
    fun saveAsDraft() {
        viewModelScope.launch {
            validateInput()
            if (_uiState.value.validationErrors.isEmpty()) {
                try {
                    val reimbursement = createReimbursement(ReimbursementStatus.DRAFT)
                    reimbursementRepository.insertReimbursement(reimbursement)
                    // Link receipts to reimbursement
                    linkReceiptsToReimbursement(reimbursement.id)
                    _uiState.value = _uiState.value.copy(isSaved = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            } else {
                _showValidationError.value = true
            }
        }
    }
    
    fun submitForApproval() {
        viewModelScope.launch {
            validateInput()
            if (_uiState.value.validationErrors.isEmpty()) {
                try {
                    val reimbursement = createReimbursement(ReimbursementStatus.PENDING)
                    reimbursementRepository.insertReimbursement(reimbursement)
                    // Link receipts to reimbursement
                    linkReceiptsToReimbursement(reimbursement.id)
                    _uiState.value = _uiState.value.copy(isSubmitted = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            } else {
                _showValidationError.value = true
            }
        }
    }
    
    fun prepareComplianceCheck(
        callback: (Reimbursement, List<Receipt>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Get full receipt details
                val fullReceipts = mutableListOf<Receipt>()
                _uiState.value.selectedReceipts.forEach { summary ->
                    receiptRepository.getReceiptById(summary.id)?.let { receipt ->
                        fullReceipts.add(receipt)
                    }
                }
                
                // Create temporary reimbursement for validation
                val reimbursement = createReimbursement(ReimbursementStatus.DRAFT)
                
                callback(reimbursement, fullReceipts)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    private fun validateInput() {
        val errors = mutableListOf<String>()
        
        if (_uiState.value.title.isBlank()) {
            errors.add("报销标题不能为空")
        }
        
        if (_uiState.value.applicant.isBlank()) {
            errors.add("申请人不能为空")
        }
        
        if (_uiState.value.selectedReceipts.isEmpty()) {
            errors.add("必须至少选择一张票据")
        }
        
        _uiState.value = _uiState.value.copy(validationErrors = errors)
    }
    
    private fun createReimbursement(status: ReimbursementStatus): Reimbursement {
        return Reimbursement(
            id = 0,
            title = _uiState.value.title,
            totalAmount = _uiState.value.totalAmount,
            applicant = _uiState.value.applicant,
            department = _uiState.value.department,
            description = _uiState.value.description,
            status = status,
            createdAt = LocalDateTime.now().toString(),
            updatedAt = LocalDateTime.now().toString(),
            project = _uiState.value.project,
            budgetCode = _uiState.value.budgetCode
        )
    }
    
    private fun linkReceiptsToReimbursement(reimbursementId: Long) {
        viewModelScope.launch {
            _uiState.value.selectedReceipts.forEach { receiptSummary ->
                receiptRepository.getReceiptById(receiptSummary.id)?.let { receipt ->
                    receiptRepository.updateReimbursementId(receipt.id, reimbursementId)
                }
            }
        }
    }
    
    fun hideValidationError() {
        _showValidationError.value = false
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CreateReimbursementUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val applicant: String = "",
    val department: String = "",
    val project: String = "",
    val budgetCode: String = "",
    val availableReceipts: List<ReceiptSummary> = emptyList(),
    val selectedReceipts: List<ReceiptSummary> = emptyList(),
    val totalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val validationErrors: List<String> = emptyList(),
    val isSaved: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

data class ReceiptSummary(
    val id: Long,
    val receiptType: ReceiptType,
    val sellerName: String?,
    val amount: Double
)