package com.billmii.android.data.database.dao

import androidx.room.*
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Operation Log Data Access Object
 * Provides database operations for operation logs
 */
@Dao
interface OperationLogDao {
    
    // Basic CRUD operations - 基础增删改查
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: OperationLog): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<OperationLog>): List<Long>
    
    @Query("SELECT * FROM operation_logs WHERE id = :id")
    suspend fun getById(id: Long): OperationLog?
    
    @Query("SELECT * FROM operation_logs ORDER BY operationTime DESC")
    fun getAllLogs(): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs ORDER BY operationTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getLogsPaged(limit: Int, offset: Int): List<OperationLog>>
    
    // Query by operation type - 按操作类型查询
    
    @Query("SELECT * FROM operation_logs WHERE operationType = :type ORDER BY operationTime DESC")
    fun getByOperationType(type: OperationType): Flow<List<OperationLog>>
    
    // Query by module - 按模块查询
    
    @Query("SELECT * FROM operation_logs WHERE operationModule = :module ORDER BY operationTime DESC")
    fun getByModule(module: OperationModule): Flow<List<OperationLog>>
    
    // Query by result - 按结果查询
    
    @Query("SELECT * FROM operation_logs WHERE operationResult = :result ORDER BY operationTime DESC")
    fun getByResult(result: OperationResult): Flow<List<OperationLog>>
    
    // Query by operator - 按操作人查询
    
    @Query("SELECT * FROM operation_logs WHERE operator = :operator ORDER BY operationTime DESC")
    fun getByOperator(operator: String): Flow<List<OperationLog>>
    
    // Query by related entity - 按关联实体查询
    
    @Query("SELECT * FROM operation_logs WHERE receiptId = :receiptId ORDER BY operationTime DESC")
    fun getByReceiptId(receiptId: Long): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE reimbursementId = :reimbursementId ORDER BY operationTime DESC")
    fun getByReimbursementId(reimbursementId: Long): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE ruleId = :ruleId ORDER BY operationTime DESC")
    fun getByRuleId(ruleId: Long): Flow<List<OperationLog>>
    
    // Date range queries - 日期范围查询
    
    @Query("""
        SELECT * FROM operation_logs 
        WHERE operationTime BETWEEN :startDate AND :endDate
        ORDER BY operationTime DESC
    """)
    fun getByDateRange(startDate: Date, endDate: Date): Flow<List<OperationLog>>
    
    @Query("""
        SELECT * FROM operation_logs 
        WHERE operationTime >= :startDate
        ORDER BY operationTime DESC
    """)
    fun getLogsAfterDate(startDate: Date): Flow<List<OperationLog>>
    
    // Search operations - 搜索操作
    
    @Query("""
        SELECT * FROM operation_logs 
        WHERE operationDescription LIKE '%' || :query || '%'
           OR operator LIKE '%' || :query || '%'
           OR errorMessage LIKE '%' || :query || '%'
        ORDER BY operationTime DESC
    """)
    fun search(query: String): Flow<List<OperationLog>>
    
    // Failed operations - 失败操作
    
    @Query("""
        SELECT * FROM operation_logs 
        WHERE operationResult = 'FAILED'
        ORDER BY operationTime DESC
    """)
    fun getFailedOperations(): Flow<List<OperationLog>>
    
    @Query("""
        SELECT * FROM operation_logs 
        WHERE operationResult = 'FAILED' 
          AND operationTime BETWEEN :startDate AND :endDate
        ORDER BY operationTime DESC
    """)
    fun getFailedOperationsInRange(startDate: Date, endDate: Date): Flow<List<OperationLog>>
    
    // Count operations - 统计操作
    
    @Query("SELECT COUNT(*) FROM operation_logs")
    suspend fun countAll(): Int
    
    @Query("SELECT COUNT(*) FROM operation_logs WHERE operationType = :type")
    suspend fun countByType(type: OperationType): Int
    
    @Query("SELECT COUNT(*) FROM operation_logs WHERE operationModule = :module")
    suspend fun countByModule(module: OperationModule): Int
    
    @Query("SELECT COUNT(*) FROM operation_logs WHERE operationResult = :result")
    suspend fun countByResult(result: OperationResult): Int
    
    @Query("SELECT COUNT(*) FROM operation_logs WHERE operator = :operator")
    suspend fun countByOperator(operator: String): Int
    
    @Query("SELECT COUNT(*) FROM operation_logs WHERE operationResult = 'FAILED'")
    suspend fun countFailedOperations(): Int
    
    // Statistics - 统计
    
    @Query("""
        SELECT operationType, COUNT(*) as count 
        FROM operation_logs 
        GROUP BY operationType
    """)
    suspend fun getCountByOperationType(): List<OperationTypeCount>
    
    @Query("""
        SELECT operationModule, COUNT(*) as count 
        FROM operation_logs 
        GROUP BY operationModule
    """)
    suspend fun getCountByModule(): List<ModuleCount>
    
    @Query("""
        SELECT operationResult, COUNT(*) as count 
        FROM operation_logs 
        GROUP BY operationResult
    """)
    suspend fun getCountByResult(): List<ResultCount>
    
    @Query("""
        SELECT operator, COUNT(*) as count 
        FROM operation_logs 
        GROUP BY operator
    """)
    suspend fun getCountByOperator(): List<OperatorCount>
    
    // Recent operations - 最近操作
    
    @Query("""
        SELECT * FROM operation_logs 
        ORDER BY operationTime DESC 
        LIMIT :limit
    """)
    fun getRecentOperations(limit: Int): Flow<List<OperationLog>>
    
    @Query("""
        SELECT * FROM operation_logs 
        WHERE operator = :operator 
        ORDER BY operationTime DESC 
        LIMIT :limit
    """)
    fun getRecentOperationsByOperator(operator: String, limit: Int): Flow<List<OperationLog>>
    
    // Delete operations (soft delete only) - 删除操作（仅软删除）
    
    @Query("UPDATE operation_logs SET isDeleted = true WHERE id = :id")
    suspend fun softDelete(id: Long)
    
    @Query("UPDATE operation_logs SET isDeleted = true WHERE id IN (:ids)")
    suspend fun softDeleteBatch(ids: List<Long>)
    
    @Query("UPDATE operation_logs SET isDeleted = true WHERE operationTime < :beforeDate")
    suspend fun softDeleteOldLogs(beforeDate: Date)
    
    @Query("UPDATE operation_logs SET isDeleted = true")
    suspend fun deleteAll()
    
    // Query without deleted logs - 查询未删除的日志
    
    @Query("SELECT * FROM operation_logs WHERE isDeleted = false ORDER BY operationTime DESC")
    fun getActiveLogs(): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE isDeleted = true ORDER BY operationTime DESC")
    fun getDeletedLogs(): Flow<List<OperationLog>>
}

/**
 * Operation type count result - 操作类型统计结果
 */
data class OperationTypeCount(
    val operationType: OperationType,
    val count: Int
)

/**
 * Module count result - 模块统计结果
 */
data class ModuleCount(
    val operationModule: OperationModule,
    val count: Int
)

/**
 * Result count result - 结果统计结果
 */
data class ResultCount(
    val operationResult: OperationResult,
    val count: Int
)

/**
 * Operator count result - 操作人统计结果
 */
data class OperatorCount(
    val operator: String,
    val count: Int
)