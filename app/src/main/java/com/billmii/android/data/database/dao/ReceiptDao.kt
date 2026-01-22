package com.billmii.android.data.database.dao

import androidx.room.*
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Receipt Data Access Object
 * Provides database operations for receipts
 */
@Dao
interface ReceiptDao {
    
    // Basic CRUD operations - 基础增删改查
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: Receipt): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receipts: List<Receipt>): List<Long>
    
    @Update
    suspend fun update(receipt: Receipt)
    
    @Delete
    suspend fun delete(receipt: Receipt)
    
    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): Receipt?
    
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<Receipt>>
    
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getReceiptsPaged(limit: Int, offset: Int): List<Receipt>
    
    // Query by type - 按类型查询
    
    @Query("SELECT * FROM receipts WHERE receiptType = :type ORDER BY createdAt DESC")
    fun getByReceiptType(type: ReceiptType): Flow<List<Receipt>>
    
    @Query("SELECT * FROM receipts WHERE receiptCategory = :category ORDER BY createdAt DESC")
    fun getByCategory(category: ReceiptCategory): Flow<List<Receipt>>
    
    @Query("SELECT * FROM receipts WHERE expenseSubCategory = :subCategory ORDER BY createdAt DESC")
    fun getByExpenseSubCategory(subCategory: ExpenseSubCategory): Flow<List<Receipt>>
    
    // Query by status - 按状态查询
    
    @Query("SELECT * FROM receipts WHERE ocrStatus = :status ORDER BY createdAt DESC")
    fun getByOcrStatus(status: OcrStatus): Flow<List<Receipt>>
    
    @Query("SELECT * FROM receipts WHERE validationStatus = :status ORDER BY createdAt DESC")
    fun getByValidationStatus(status: ValidationStatus): Flow<List<Receipt>>
    
    @Query("SELECT * FROM receipts WHERE isProcessed = :isProcessed ORDER BY createdAt DESC")
    fun getByProcessedStatus(isProcessed: Boolean): Flow<List<Receipt>>
    
    // Query by reimbursement - 按报销单查询
    
    @Query("SELECT * FROM receipts WHERE reimbursementId = :reimbursementId ORDER BY createdAt DESC")
    fun getByReimbursementId(reimbursementId: Long): Flow<List<Receipt>>
    
    // Search operations - 搜索操作
    
    @Query("""
        SELECT * FROM receipts 
        WHERE invoiceNumber LIKE '%' || :query || '%'
           OR buyerName LIKE '%' || :query || '%'
           OR sellerName LIKE '%' || :query || '%'
           OR fileName LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun search(query: String): Flow<List<Receipt>>
    
    @Query("""
        SELECT * FROM receipts 
        WHERE invoiceCode = :code AND invoiceNumber = :number
        LIMIT 1
    """)
    suspend fun findByInvoiceCodeAndNumber(code: String, number: String): Receipt?
    
    // Date range queries - 日期范围查询
    
    @Query("""
        SELECT * FROM receipts 
        WHERE invoiceDate BETWEEN :startDate AND :endDate
        ORDER BY invoiceDate DESC
    """)
    fun getByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>>
    
    // Amount range queries - 金额范围查询
    
    @Query("""
        SELECT * FROM receipts 
        WHERE totalAmount BETWEEN :minAmount AND :maxAmount
        ORDER BY totalAmount DESC
    """)
    fun getByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Receipt>>
    
    // Duplicate detection - 重复检测
    
    @Query("SELECT * FROM receipts WHERE fileHash = :hash LIMIT 1")
    suspend fun findByFileHash(hash: String): Receipt?
    
    @Query("SELECT COUNT(*) FROM receipts WHERE fileHash = :hash")
    suspend fun countByFileHash(hash: String): Int
    
    // Archive operations - 归档操作
    
    @Query("SELECT * FROM receipts WHERE archiveNumber = :archiveNumber LIMIT 1")
    suspend fun getByArchiveNumber(archiveNumber: String): Receipt?
    
    @Query("SELECT * FROM receipts WHERE archiveNumber LIKE :pattern ORDER BY archiveNumber DESC")
    fun searchByArchiveNumber(pattern: String): Flow<List<Receipt>>
    
    @Query("""
        SELECT * FROM receipts
        WHERE archiveNumber IS NOT NULL
        ORDER BY archiveNumber DESC
    """)
    fun getArchivedReceipts(): Flow<List<Receipt>>
    
    // Batch operations - 批量操作
    
    @Query("UPDATE receipts SET isProcessed = :isProcessed WHERE id IN (:ids)")
    suspend fun updateProcessedStatus(ids: List<Long>, isProcessed: Boolean)
    
    @Query("UPDATE receipts SET reimbursementId = :reimbursementId WHERE id IN (:ids)")
    suspend fun linkToReimbursement(ids: List<Long>, reimbursementId: Long)
    
    @Query("UPDATE receipts SET reimbursementId = NULL WHERE id IN (:ids)")
    suspend fun unlinkFromReimbursement(ids: List<Long>)
    
    @Query("DELETE FROM receipts WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    // Count operations - 统计操作
    
    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun countAll(): Int
    
    @Query("SELECT COUNT(*) FROM receipts WHERE receiptType = :type")
    suspend fun countByType(type: ReceiptType): Int
    
    @Query("SELECT COUNT(*) FROM receipts WHERE ocrStatus = :status")
    suspend fun countByOcrStatus(status: OcrStatus): Int
    
    @Query("SELECT COUNT(*) FROM receipts WHERE validationStatus = :status")
    suspend fun countByValidationStatus(status: ValidationStatus): Int
    
    @Query("SELECT COUNT(*) FROM receipts WHERE isProcessed = false")
    suspend fun countUnprocessed(): Int
    
    @Query("SELECT COUNT(*) FROM receipts WHERE isDuplicate = true")
    suspend fun countDuplicates(): Int
    
    // Statistics - 统计
    
    @Query("SELECT SUM(totalAmount) FROM receipts WHERE totalAmount IS NOT NULL")
    suspend fun getTotalAmount(): Double?
    
    @Query("""
        SELECT SUM(totalAmount) 
        FROM receipts 
        WHERE expenseSubCategory = :subCategory 
          AND totalAmount IS NOT NULL
    """)
    suspend fun getTotalAmountBySubCategory(subCategory: ExpenseSubCategory): Double?
    
    @Query("""
        SELECT receiptType, COUNT(*) as count 
        FROM receipts 
        GROUP BY receiptType
    """)
    suspend fun getCountByReceiptType(): List<ReceiptTypeCount>
    
    // Complex queries - 复杂查询
    
    @Query("""
        SELECT * FROM receipts 
        WHERE ocrStatus = 'SUCCESS' 
          AND validationStatus = 'PENDING'
        ORDER BY createdAt DESC
    """)
    fun getPendingValidationReceipts(): Flow<List<Receipt>>
    
    @Query("""
        SELECT * FROM receipts 
        WHERE isDuplicate = true 
        ORDER BY createdAt DESC
    """)
    fun getDuplicateReceipts(): Flow<List<Receipt>>
    
    @Query("""
        SELECT * FROM receipts 
        WHERE isValid = false 
        ORDER BY createdAt DESC
    """)
    fun getInvalidReceipts(): Flow<List<Receipt>>
}

/**
 * Receipt type count result - 票据类型统计结果
 */
data class ReceiptTypeCount(
    val receiptType: ReceiptType,
    val count: Int
)