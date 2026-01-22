package com.billmii.android.ui.reimbursement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.model.ReimbursementStatus
import com.billmii.android.data.repository.ReimbursementRepository
import com.billmii.android.data.service.ReimbursementWorkflowService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Reimbursement Approval Workflow functionality
 */
@HiltViewModel
class ApprovalWorkflowViewModel @Inject constructor(
    private val reimbursementRepository: ReimbursementRepository,
    private val workflowService: ReimbursementWorkflowService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ApprovalWorkflowUiState())
    val uiState: StateFlow<ApprovalWorkflowUiState> = _uiState.asStateFlow()
    
    private val _showApproveDialog = MutableStateFlow(false)
    val showApproveDialog: StateFlow<Boolean> = _showApproveDialog.asStateFlow()
    
    private val _showRejectDialog = MutableStateFlow(false)
    val showRejectDialog: StateFlow<Boolean> = _showRejectDialog.asStateFlow()
    
    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()
    
    init {
        loadPendingItems()
        loadApprovedItems()
        loadRejectedItems()
        loadAllItems()
    }
    
    private fun loadPendingItems() {
        viewModelScope.launch {
            try {
                reimbursementRepository.getReimbursementsByStatus(ReimbursementStatus.PENDING)
                    .collect { reimbursements ->
                        val items = reimbursements.map { it.toApprovalItem() }
                        _uiState.value = _uiState.value.copy(
                            pendingItems = items,
                            pendingCount = items.size
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载待审批项目失败: ${e.message}"
                )
            }
        }
    }
    
    private fun loadApprovedItems() {
        viewModelScope.launch {
            try {
                reimbursementRepository.getReimbursementsByStatus(ReimbursementStatus.APPROVED)
                    .collect { reimbursements ->
                        val items = reimbursements.map { it.toApprovalItem() }
                        _uiState.value = _uiState.value.copy(
                            approvedItems = items,
                            approvedCount = items.size
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载已通过项目失败: ${e.message}"
                )
            }
        }
    }
    
    private fun loadRejectedItems() {
        viewModelScope.launch {
            try {
                reimbursementRepository.getReimbursementsByStatus(ReimbursementStatus.REJECTED)
                    .collect { reimbursements ->
                        val items = reimbursements.map { it.toApprovalItem() }
                        _uiState.value = _uiState.value.copy(
                            rejectedItems = items,
                            rejectedCount = items.size
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载已拒绝项目失败: ${e.message}"
                )
            }
        }
    }
    
    private fun loadAllItems() {
        viewModelScope.launch {
            try {
                reimbursementRepository.getAllReimbursements()
                    .collect { reimbursements ->
                        val items = reimbursements.map { it.toApprovalItem() }
                        _uiState.value = _uiState.value.copy(allItems = items)
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载所有项目失败: ${e.message}"
                )
            }
        }
    }
    
    fun selectFilter(filter: ApprovalFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }
    
    fun showApproveConfirmation(item: ApprovalItem) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
        _showApproveDialog.value = true
    }
    
    fun hideApproveDialog() {
        _showApproveDialog.value = false
        _uiState.value = _uiState.value.copy(selectedItem = null)
    }
    
    fun showRejectConfirmation(item: ApprovalItem) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
        _showRejectDialog.value = true
    }
    
    fun hideRejectDialog() {
        _showRejectDialog.value = false
        _uiState.value = _uiState.value.copy(selectedItem = null)
    }
    
    fun showCommentDialog(item: ApprovalItem) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
        _showCommentDialog.value = true
    }
    
    fun hideCommentDialog() {
        _showCommentDialog.value = false
        _uiState.value = _uiState.value.copy(selectedItem = null)
    }
    
    fun approveReimbursement(comment: String) {
        val item = _uiState.value.selectedItem ?: return
        
        viewModelScope.launch {
            try {
                workflowService.approveReimbursement(
                    reimbursementId = item.reimbursementId,
                    approverId = getCurrentUserId(),
                    comment = comment.ifBlank { null }
                )
                
                hideApproveDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "审批通过失败: ${e.message}"
                )
            }
        }
    }
    
    fun rejectReimbursement(comment: String) {
        val item = _uiState.value.selectedItem ?: return
        
        viewModelScope.launch {
            try {
                workflowService.rejectReimbursement(
                    reimbursementId = item.reimbursementId,
                    approverId = getCurrentUserId(),
                    reason = comment
                )
                
                hideRejectDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "拒绝报销失败: ${e.message}"
                )
            }
        }
    }
    
    fun addComment(comment: String) {
        val item = _uiState.value.selectedItem ?: return
        
        viewModelScope.launch {
            try {
                workflowService.addComment(
                    reimbursementId = item.reimbursementId,
                    authorId = getCurrentUserId(),
                    authorName = getCurrentUserName(),
                    content = comment
                )
                
                hideCommentDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "添加备注失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // TODO: Implement proper user authentication
    private fun getCurrentUserId(): Long {
        return 1L // Default user ID
    }
    
    private fun getCurrentUserName(): String {
        return "当前用户" // Default user name
    }
}

data class ApprovalWorkflowUiState(
    val selectedFilter: ApprovalFilter = ApprovalFilter.PENDING,
    val pendingItems: List<ApprovalItem> = emptyList(),
    val approvedItems: List<ApprovalItem> = emptyList(),
    val rejectedItems: List<ApprovalItem> = emptyList(),
    val allItems: List<ApprovalItem> = emptyList(),
    val selectedItem: ApprovalItem? = null,
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val error: String? = null
)

enum class ApprovalFilter {
    PENDING,
    APPROVED,
    REJECTED,
    ALL
}

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class ApprovalItem(
    val reimbursementId: Long,
    val title: String,
    val applicantName: String,
    val amount: Double,
    val receiptCount: Int,
    val department: String?,
    val status: ApprovalStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val latestComment: ApprovalComment?
)

data class ApprovalComment(
    val id: Long,
    val content: String,
    val authorName: String,
    val createdAt: Long
)

// Extension function
fun Reimbursement.toApprovalItem(): ApprovalItem {
    return ApprovalItem(
        reimbursementId = id,
        title = title,
        applicantName = applicantName,
        amount = totalAmount,
        receiptCount = receiptIds.size,
        department = department,
        status = when (status) {
            ReimbursementStatus.DRAFT -> ApprovalStatus.PENDING
            ReimbursementStatus.PENDING -> ApprovalStatus.PENDING
            ReimbursementStatus.APPROVED -> ApprovalStatus.APPROVED
            ReimbursementStatus.REJECTED -> ApprovalStatus.REJECTED
            ReimbursementStatus.PAID -> ApprovalStatus.APPROVED
        },
        createdAt = createdAt,
        updatedAt = updatedAt,
        latestComment = comments.lastOrNull()?.toApprovalComment()
    )
}

fun com.billmii.android.data.model.ApprovalComment.toApprovalComment(): ApprovalComment {
    return ApprovalComment(
        id = id,
        content = content,
        authorName = authorName,
        createdAt = createdAt
    )
}