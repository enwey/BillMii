package com.billmii.android.data.repository

import com.billmii.android.data.database.dao.ReimbursementDao
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reimbursement Repository - 报销单仓库
 * Manages reimbursement data operations
 */
@Singleton
class ReimbursementRepository @Inject constructor(
    private val reimbursementDao: ReimbursementDao
) {
    
    /**
     * Get all reimbursements
     */
    fun getAllReimbursements(): Flow<List<Reimbursement>> {
        return reimbursementDao.getAllReimbursements()
    }
    
    /**
     * Get reimbursement by ID
     */
    suspend fun getById(id: Long): Reimbursement? {
        return reimbursementDao.getById(id)
    }
    
    /**
     * Get reimbursement with receipts
     */
    suspend fun getWithReceipts(id: Long): ReimbursementWithReceipts? {
        return reimbursementDao.getWithReceipts(id)
    }
    
    /**
     * Get reimbursements by approval status
     */
    fun getByApprovalStatus(status: ApprovalStatus): Flow<List<Reimbursement>> {
        return reimbursementDao.getByApprovalStatus(status)
    }
    
    /**
     * Get pending reimbursements
     */
    fun getPendingReimbursements(): Flow<List<Reimbursement>> {
        return reimbursementDao.getByApprovalStatus(ApprovalStatus.PENDING)
    }
    
    /**
     * Create reimbursement
     */
    suspend fun create(reimbursement: Reimbursement): Result<Reimbursement> {
        return try {
            val id = reimbursementDao.insert(reimbursement)
            Result.success(reimbursement.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update reimbursement
     */
    suspend fun update(reimbursement: Reimbursement): Result<Reimbursement> {
        return try {
            reimbursementDao.update(reimbursement)
            Result.success(reimbursement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete reimbursement
     */
    suspend fun delete(reimbursement: Reimbursement): Result<Unit> {
        return try {
            reimbursementDao.delete(reimbursement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update approval status
     */
    suspend fun updateApprovalStatus(id: Long, status: ApprovalStatus): Result<Unit> {
        return try {
            reimbursementDao.updateApprovalStatus(id, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update validation status
     */
    suspend fun updateValidationStatus(
        id: Long,
        status: ReimbursementValidationStatus
    ): Result<Unit> {
        return try {
            reimbursementDao.updateValidationStatus(id, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Archive reimbursement
     */
    suspend fun archive(id: Long): Result<Unit> {
        return try {
            reimbursementDao.updateArchivedStatus(id, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get statistics
     */
    suspend fun getStatistics(): ReimbursementStatistics {
        val totalCount = reimbursementDao.countAll()
        val pendingCount = reimbursementDao.countByApprovalStatus(ApprovalStatus.PENDING)
        val approvedCount = reimbursementDao.countByApprovalStatus(ApprovalStatus.APPROVED)
        val totalAmount = reimbursementDao.getTotalApprovedAmount() ?: 0.0
        
        return ReimbursementStatistics(
            totalCount = totalCount,
            pendingCount = pendingCount,
            approvedCount = approvedCount,
            totalApprovedAmount = totalAmount
        )
    }
}

/**
 * Reimbursement statistics data class
 */
data class ReimbursementStatistics(
    val totalCount: Int,
    val pendingCount: Int,
    val approvedCount: Int,
    val totalApprovedAmount: Double
)