package com.billmii.android.ui.reimbursement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.ReceiptType
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.service.ReimbursementWorkflowService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Create Reimbursement
 */
@HiltViewModel
class CreateReimbursementViewModel @Inject constructor(
    private val reimbursementWorkflowService: ReimbursementWorkflowService,
    private val receiptDao: ReceiptDao
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
            try {
                receiptDao.getUnlinkedReceipts()
                    .collect { receipts ->
                        val summaries = receipts.map { it.toSummary() }
                        _uiState.value = _uiState.value.copy(
                            availableReceipts = summaries
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
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
            taxAmount = receipts.sumOf { 0.0 } // TODO: Get actual tax amount
        )
    }
    
    fun removeReceipt(receiptId: Long) {
        val updated = _uiState.value.selectedReceipts.filter { it.id != receiptId }
        updateSelectedReceipts(updated)
    }
    
    fun saveAsDraft() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = reimbursementWorkflowService.createReimbursement(
                    title = _uiState.value.title,
                    description = _uiState.value.description.ifBlank { null },
                    applicant = _uiState.value.applicant,
                    department = _uiState.value.department.ifBlank { null },
                    project = _uiState.value.project.ifBlank { null },
                    budgetCode = _uiState.value.budgetCode.ifBlank { null },
                    receiptIds = _uiState.value.selectedReceipts.map { it.id }
                )
                
                if (result.isSuccess) {
                    // Save as draft - keep status as DRAFT
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        createSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun submitForApproval() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Validate
            val validationErrors = validateForm()
            if (validationErrors.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationErrors = validationErrors
                )
                _showValidationError.value = true
                return@launch
            }
            
            try {
                val result = reimbursementWorkflowService.createReimbursement(
                    title = _uiState.value.title,
                    description = _uiState.value.description.ifBlank { null },
                    applicant = _uiState.value.applicant,
                    department = _uiState.value.department.ifBlank { null },
                    project = _uiState.value.project.ifBlank { null },
                    budgetCode = _uiState.value.budgetCode.ifBlank { null },
                    receiptIds = _uiState.value.selectedReceipts.map { it.id }
                )
                
                if (result.isSuccess) {
                    val reimbursementId = result.getOrNull()?.id ?: return@launch
                    
                    // Submit for approval
                    val submitResult = reimbursementWorkflowService.submitForApproval(reimbursementId)
                    
                    if (submitResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            createSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = submitResult.exceptionOrNull()?.message
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun validateForm(): List<String> {
        val errors = mutableListOf<String>()
        
        if (_uiState.value.title.isBlank()) {
            errors.add("报销标题不能为空")
        }
        
        if (_uiState.value.applicant.isBlank()) {
            errors.add("申请人不能为空")
        }
        
        if (_uiState.value.selectedReceipts.isEmpty()) {
            errors.add("至少需要关联一张票据")
        }
        
        return errors
    }
    
    fun hideValidationError() {
        _showValidationError.value = false
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// UI State
data class CreateReimbursementUiState(
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
    val isLoading: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val error: String? = null,
    val createSuccess: Boolean = false
)

// Extension function
private fun Receipt.toSummary(): ReceiptSummary {
    return ReceiptSummary(
        id = id,
        receiptType = receiptType,
        sellerName = sellerName,
        amount = totalAmount ?: amount ?: 0.0
    )
}