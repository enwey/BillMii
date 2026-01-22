package com.billmii.android.data.service

import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.database.dao.ReimbursementDao
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reimbursement Workflow Service
 * Manages reimbursement creation, approval workflow, and validation
 */
@Singleton
class ReimbursementWorkflowService @Inject constructor(
    private val reimbursementDao: ReimbursementDao,
    private val receiptDao: ReceiptDao
) {
    
    companion object {
        private const val TAG = "ReimbursementWorkflowService"
    }
    
    /**
     * Create new reimbursement
     */
    suspend fun createReimbursement(
        title: String,
        description: String? = null,
        applicant: String,
        department: String? = null,
        project: String? = null,
        budgetCode: String? = null,
        receiptIds: List<Long> = emptyList()
    ): Result<Reimbursement> {
        return try {
            // Calculate total amount from receipts
            val receipts = receiptIds.mapNotNull { receiptDao.getById(it) }
            val totalAmount = receipts.sumOf { it.totalAmount ?: it.amount ?: 0.0 }
            val totalTax = receipts.sumOf { it.taxAmount ?: 0.0 }
            val amountWithoutTax = totalAmount - totalTax
            
            // Create reimbursement
            val reimbursement = Reimbursement(
                title = title,
                description = description,
                applicant = applicant,
                department = department,
                project = project,
                budgetCode = budgetCode,
                totalAmount = totalAmount,
                amountWithoutTax = amountWithoutTax,
                taxAmount = totalTax,
                receiptCount = receipts.size,
                status = ReimbursementStatus.DRAFT,
                workflowStatus = WorkflowStatus.PENDING_SUBMISSION,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val id = reimbursementDao.insert(reimbursement)
            
            // Link receipts
            if (receiptIds.isNotEmpty()) {
                receiptDao.linkToReimbursement(receiptIds, id)
            }
            
            val createdReimbursement = reimbursement.copy(id = id)
            Result.success(createdReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit reimbursement for approval
     */
    suspend fun submitForApproval(reimbursementId: Long): Result<Reimbursement> {
        return try {
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            // Validate before submission
            val validationResult = validateReimbursement(reimbursementId)
            if (!validationResult.isValid) {
                return Result.failure(Exception(validationResult.errors.joinToString("\n")))
            }
            
            // Update status
            val updatedReimbursement = reimbursement.copy(
                status = ReimbursementStatus.SUBMITTED,
                workflowStatus = WorkflowStatus.PENDING_APPROVAL,
                submittedAt = Date(),
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(updatedReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Approve reimbursement
     */
    suspend fun approveReimbursement(
        reimbursementId: Long,
        approver: String,
        comment: String? = null
    ): Result<Reimbursement> {
        return try {
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            val updatedReimbursement = reimbursement.copy(
                status = ReimbursementStatus.APPROVED,
                workflowStatus = WorkflowStatus.COMPLETED,
                currentApprover = approver,
                approvedAt = Date(),
                approvalComment = comment,
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(updatedReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reject reimbursement
     */
    suspend fun rejectReimbursement(
        reimbursementId: Long,
        rejector: String,
        reason: String
    ): Result<Reimbursement> {
        return try {
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            val updatedReimbursement = reimbursement.copy(
                status = ReimbursementStatus.REJECTED,
                workflowStatus = WorkflowStatus.REJECTED,
                currentApprover = rejector,
                rejectedAt = Date(),
                rejectionReason = reason,
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(updatedReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Return reimbursement for revision
     */
    suspend fun returnForRevision(
        reimbursementId: Long,
        reviewer: String,
        comment: String
    ): Result<Reimbursement> {
        return try {
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            val updatedReimbursement = reimbursement.copy(
                status = ReimbursementStatus.REVISION_REQUIRED,
                workflowStatus = WorkflowStatus.REVISION_REQUIRED,
                currentApprover = reviewer,
                revisionComment = comment,
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(updatedReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Resubmit reimbursement
     */
    suspend fun resubmitReimbursement(reimbursementId: Long): Result<Reimbursement> {
        return try {
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            val updatedReimbursement = reimbursement.copy(
                status = ReimbursementStatus.SUBMITTED,
                workflowStatus = WorkflowStatus.PENDING_APPROVAL,
                revisionCount = reimbursement.revisionCount + 1,
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(updatedReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel reimbursement
     */
    suspend fun cancelReimbursement(reimbursementId: Long): Result<Reimbursement> {
        return try {
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            // Can only cancel draft or revision required
            if (reimbursement.status != ReimbursementStatus.DRAFT &&
                reimbursement.status != ReimbursementStatus.REVISION_REQUIRED) {
                return Result.failure(Exception("Cannot cancel reimbursement in current status"))
            }
            
            val updatedReimbursement = reimbursement.copy(
                status = ReimbursementStatus.CANCELLED,
                workflowStatus = WorkflowStatus.CANCELLED,
                cancelledAt = Date(),
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(updatedReimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate reimbursement
     */
    suspend fun validateReimbursement(reimbursementId: Long): ValidationResult {
        val reimbursement = reimbursementDao.getById(reimbursementId)
            ?: return ValidationResult(false, listOf("Reimbursement not found"))
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check if has receipts
        val receipts = receiptDao.getByReimbursementId(reimbursementId).first()
        if (receipts.isEmpty()) {
            errors.add("Reimbursement must have at least one receipt")
        }
        
        // Check title
        if (reimbursement.title.isBlank()) {
            errors.add("Title is required")
        }
        
        // Check applicant
        if (reimbursement.applicant.isBlank()) {
            errors.add("Applicant is required")
        }
        
        // Check amount
        if (reimbursement.totalAmount == null || reimbursement.totalAmount <= 0) {
            errors.add("Total amount must be greater than 0")
        }
        
        // Check for duplicate receipts
        val duplicateReceipts = receipts.filter { it.isDuplicate }
        if (duplicateReceipts.isNotEmpty()) {
            warnings.add("Contains ${duplicateReceipts.size} duplicate receipt(s)")
        }
        
        // Check for unprocessed receipts
        val unprocessedReceipts = receipts.filter { !it.processed }
        if (unprocessedReceipts.isNotEmpty()) {
            warnings.add("Contains ${unprocessedReceipts.size} unprocessed receipt(s)")
        }
        
        // Check for OCR failed receipts
        val ocrFailedReceipts = receipts.filter { it.ocrStatus == OcrStatus.FAILED }
        if (ocrFailedReceipts.isNotEmpty()) {
            warnings.add("Contains ${ocrFailedReceipts.size} receipt(s) with failed OCR")
        }
        
        // Check budget (if budget code provided)
        if (reimbursement.budgetCode != null) {
            // TODO: Check against budget system
            warnings.add("Budget validation not implemented")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Get pending approvals
     */
    fun getPendingApprovals(): Flow<List<Reimbursement>> {
        return reimbursementDao.getByStatus(ReimbursementStatus.SUBMITTED)
    }
    
    /**
     * Get my reimbursements
     */
    fun getMyReimbursements(applicant: String): Flow<List<Reimbursement>> {
        return reimbursementDao.getByApplicant(applicant)
    }
    
    /**
     * Get reimbursement statistics
     */
    suspend fun getReimbursementStatistics(): ReimbursementStatistics {
        val totalCount = reimbursementDao.countAll()
        val pendingCount = reimbursementDao.countByStatus(ReimbursementStatus.SUBMITTED)
        val approvedCount = reimbursementDao.countByStatus(ReimbursementStatus.APPROVED)
        val rejectedCount = reimbursementDao.countByStatus(ReimbursementStatus.REJECTED)
        val totalAmount = reimbursementDao.getTotalAmount() ?: 0.0
        val pendingAmount = reimbursementDao.getTotalAmountByStatus(ReimbursementStatus.SUBMITTED) ?: 0.0
        
        return ReimbursementStatistics(
            totalCount = totalCount,
            pendingCount = pendingCount,
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            totalAmount = totalAmount,
            pendingAmount = pendingAmount
        )
    }
    
    /**
     * Add receipt to reimbursement
     */
    suspend fun addReceiptToReimbursement(
        reimbursementId: Long,
        receiptId: Long
    ): Result<Unit> {
        return try {
            // Check if reimbursement exists
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            // Check if receipt exists
            val receipt = receiptDao.getById(receiptId)
                ?: return Result.failure(Exception("Receipt not found"))
            
            // Check if receipt is already linked
            if (receipt.reimbursementId != null) {
                return Result.failure(Exception("Receipt already linked to another reimbursement"))
            }
            
            // Link receipt
            receiptDao.linkToReimbursement(listOf(receiptId), reimbursementId)
            
            // Update reimbursement totals
            val updatedReimbursement = reimbursement.copy(
                receiptCount = reimbursement.receiptCount + 1,
                totalAmount = (reimbursement.totalAmount ?: 0.0) + (receipt.totalAmount ?: receipt.amount ?: 0.0),
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove receipt from reimbursement
     */
    suspend fun removeReceiptFromReimbursement(
        reimbursementId: Long,
        receiptId: Long
    ): Result<Unit> {
        return try {
            // Check if reimbursement exists
            val reimbursement = reimbursementDao.getById(reimbursementId)
                ?: return Result.failure(Exception("Reimbursement not found"))
            
            // Check if receipt exists and is linked
            val receipt = receiptDao.getById(receiptId)
                ?: return Result.failure(Exception("Receipt not found"))
            
            if (receipt.reimbursementId != reimbursementId) {
                return Result.failure(Exception("Receipt not linked to this reimbursement"))
            }
            
            // Unlink receipt
            receiptDao.unlinkFromReimbursement(listOf(receiptId))
            
            // Update reimbursement totals
            val updatedReimbursement = reimbursement.copy(
                receiptCount = maxOf(0, reimbursement.receiptCount - 1),
                totalAmount = (reimbursement.totalAmount ?: 0.0) - (receipt.totalAmount ?: receipt.amount ?: 0.0),
                updatedAt = Date()
            )
            
            reimbursementDao.update(updatedReimbursement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Reimbursement statistics
 */
data class ReimbursementStatistics(
    val totalCount: Int,
    val pendingCount: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val totalAmount: Double,
    val pendingAmount: Double
)