package com.billmii.android.data.database.dao

import androidx.room.*
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Reimbursement Data Access Object
 * Provides database operations for reimbursements
 */
@Dao
interface ReimbursementDao {
    
    // Basic CRUD operations - 基础增删改查
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reimbursement: Reimbursement): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reimbursements: List<Reimbursement>): List<Long>
    
    @Update
    suspend fun update(reimbursement: Reimbursement)
    
    @Delete
    suspend fun delete(reimbursement: Reimbursement)
    
    @Query("DELETE FROM reimbursements WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM reimbursements WHERE id = :id")
    suspend fun getById(id: Long): Reimbursement?
    
    @Query("SELECT * FROM reimbursements ORDER BY createdAt DESC")
    fun getAllReimbursements(): Flow<List<Reimbursement>>
    
    @Transaction
    @Query("SELECT * FROM reimbursements WHERE id = :id")
    suspend fun getWithReceipts(id: Long): ReimbursementWithReceipts?
    
    // Query by status - 按状态查询
    
    @Query("SELECT * FROM reimbursements WHERE approvalStatus = :status ORDER BY createdAt DESC")
    fun getByApprovalStatus(status: ApprovalStatus): Flow<List<Reimbursement>>
    
    @Query("SELECT * FROM reimbursements WHERE validationStatus = :status ORDER BY createdAt DESC")
    fun getByValidationStatus(status: ReimbursementValidationStatus): Flow<List<Reimbursement>>
    
    @Query("SELECT * FROM reimbursements WHERE templateType = :type ORDER BY createdAt DESC")
    fun getByTemplateType(type: ReimbursementTemplate): Flow<List<Reimbursement>>
    
    // Query by applicant - 按申请人查询
    
    @Query("SELECT * FROM reimbursements WHERE applicant = :applicant ORDER BY createdAt DESC")
    fun getByApplicant(applicant: String): Flow<List<Reimbursement>>
    
    @Query("SELECT * FROM reimbursements WHERE department = :department ORDER BY createdAt DESC")
    fun getByDepartment(department: String): Flow<List<Reimbursement>>
    
    // Search operations - 搜索操作
    
    @Query("""
        SELECT * FROM reimbursements 
        WHERE reimbursementNumber LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
           OR applicant LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun search(query: String): Flow<List<Reimbursement>>
    
    @Query("SELECT * FROM reimbursements WHERE reimbursementNumber = :number LIMIT 1")
    suspend fun findByReimbursementNumber(number: String): Reimbursement?
    
    // Date range queries - 日期范围查询
    
    @Query("""
        SELECT * FROM reimbursements 
        WHERE createdAt BETWEEN :startDate AND :endDate
        ORDER BY createdAt DESC
    """)
    fun getByDateRange(startDate: Date, endDate: Date): Flow<List<Reimbursement>>
    
    @Query("""
        SELECT * FROM reimbursements 
        WHERE submittedAt BETWEEN :startDate AND :endDate
        ORDER BY submittedAt DESC
    """)
    fun getBySubmittedDateRange(startDate: Date, endDate: Date): Flow<List<Reimbursement>>
    
    // Amount range queries - 金额范围查询
    
    @Query("""
        SELECT * FROM reimbursements 
        WHERE totalAmount BETWEEN :minAmount AND :maxAmount
        ORDER BY totalAmount DESC
    """)
    fun getByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Reimbursement>>
    
    // Archive operations - 归档操作
    
    @Query("SELECT * FROM reimbursements WHERE isArchived = true ORDER BY createdAt DESC")
    fun getArchivedReimbursements(): Flow<List<Reimbursement>>
    
    @Query("SELECT * FROM reimbursements WHERE isArchived = false ORDER BY createdAt DESC")
    fun getActiveReimbursements(): Flow<List<Reimbursement>>
    
    // Export operations - 导出操作
    
    @Query("SELECT * FROM reimbursements WHERE isExported = false ORDER BY createdAt DESC")
    fun getUnexportedReimbursements(): Flow<List<Reimbursement>>
    
    // Count operations - 统计操作
    
    @Query("SELECT COUNT(*) FROM reimbursements")
    suspend fun countAll(): Int
    
    @Query("SELECT COUNT(*) FROM reimbursements WHERE approvalStatus = :status")
    suspend fun countByApprovalStatus(status: ApprovalStatus): Int
    
    @Query("SELECT COUNT(*) FROM reimbursements WHERE validationStatus = :status")
    suspend fun countByValidationStatus(status: ReimbursementValidationStatus): Int
    
    @Query("SELECT COUNT(*) FROM reimbursements WHERE templateType = :type")
    suspend fun countByTemplateType(type: ReimbursementTemplate): Int
    
    @Query("SELECT COUNT(*) FROM reimbursements WHERE isArchived = false")
    suspend fun countActive(): Int
    
    @Query("SELECT COUNT(*) FROM reimbursements WHERE isArchived = true")
    suspend fun countArchived(): Int
    
    // Statistics - 统计
    
    @Query("SELECT SUM(totalAmount) FROM reimbursements WHERE approvalStatus = 'APPROVED'")
    suspend fun getTotalApprovedAmount(): Double?
    
    @Query("""
        SELECT SUM(totalAmount) 
        FROM reimbursements 
        WHERE department = :department 
          AND approvalStatus = 'APPROVED'
    """)
    suspend fun getTotalAmountByDepartment(department: String): Double?
    
    @Query("""
        SELECT SUM(totalAmount) 
        FROM reimbursements 
        WHERE applicant = :applicant 
          AND approvalStatus = 'APPROVED'
    """)
    suspend fun getTotalAmountByApplicant(applicant: String): Double?
    
    @Query("""
        SELECT templateType, COUNT(*) as count 
        FROM reimbursements 
        GROUP BY templateType
    """)
    suspend fun getCountByTemplateType(): List<TemplateTypeCount>
    
    @Query("""
        SELECT approvalStatus, COUNT(*) as count 
        FROM reimbursements 
        GROUP BY approvalStatus
    """)
    suspend fun getCountByApprovalStatus(): List<ApprovalStatusCount>
    
    // Workflow operations - 工作流操作
    
    @Query("""
        SELECT * FROM reimbursements 
        WHERE approvalStatus = 'PENDING' 
        ORDER BY createdAt ASC
    """)
    fun getPendingReimbursements(): Flow<List<Reimbursement>>
    
    @Query("""
        SELECT * FROM reimbursements 
        WHERE approvalStatus = 'IN_REVIEW' 
        ORDER BY createdAt ASC
    """)
    fun getInReviewReimbursements(): Flow<List<Reimbursement>>
    
    // Update operations - 更新操作
    
    @Query("UPDATE reimbursements SET approvalStatus = :status WHERE id = :id")
    suspend fun updateApprovalStatus(id: Long, status: ApprovalStatus)
    
    @Query("UPDATE reimbursements SET validationStatus = :status WHERE id = :id")
    suspend fun updateValidationStatus(id: Long, status: ReimbursementValidationStatus)
    
    @Query("UPDATE reimbursements SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchivedStatus(id: Long, isArchived: Boolean)
    
    @Query("UPDATE reimbursements SET isExported = :isExported WHERE id = :id")
    suspend fun updateExportedStatus(id: Long, isExported: Boolean)
    
    @Query("UPDATE reimbursements SET currentStep = :step WHERE id = :id")
    suspend fun updateCurrentStep(id: Long, step: Int)
    
    // Batch operations - 批量操作
    
    @Query("UPDATE reimbursements SET approvalStatus = :status WHERE id IN (:ids)")
    suspend fun updateApprovalStatusBatch(ids: List<Long>, status: ApprovalStatus)
    
    @Query("UPDATE reimbursements SET isArchived = true WHERE id IN (:ids)")
    suspend fun archiveBatch(ids: List<Long>)
    
    @Query("UPDATE reimbursements SET isExported = true WHERE id IN (:ids)")
    suspend fun markExportedBatch(ids: List<Long>)
}

/**
 * Template type count result - 模板类型统计结果
 */
data class TemplateTypeCount(
    val templateType: ReimbursementTemplate,
    val count: Int
)

/**
 * Approval status count result - 审批状态统计结果
 */
data class ApprovalStatusCount(
    val approvalStatus: ApprovalStatus,
    val count: Int
)