package com.billmii.android.data.database.dao

import androidx.room.*
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Classification Rule Data Access Object
 * Provides database operations for classification rules
 */
@Dao
interface ClassificationRuleDao {
    
    // Basic CRUD operations - 基础增删改查
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ClassificationRule): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ClassificationRule>): List<Long>
    
    @Update
    suspend fun update(rule: ClassificationRule)
    
    @Delete
    suspend fun delete(rule: ClassificationRule)
    
    @Query("DELETE FROM classification_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM classification_rules WHERE id = :id")
    suspend fun getById(id: Long): ClassificationRule?
    
    @Query("SELECT * FROM classification_rules ORDER BY priority DESC, createdAt DESC")
    fun getAllRules(): Flow<List<ClassificationRule>>
    
    // Query by category - 按分类查询
    
    @Query("SELECT * FROM classification_rules WHERE targetCategory = :category ORDER BY priority DESC")
    fun getByCategory(category: ReceiptCategory): Flow<List<ClassificationRule>>
    
    @Query("SELECT * FROM classification_rules WHERE targetSubCategory = :subCategory ORDER BY priority DESC")
    fun getBySubCategory(subCategory: ExpenseSubCategory): Flow<List<ClassificationRule>>
    
    // Query by status - 按状态查询
    
    @Query("SELECT * FROM classification_rules WHERE isEnabled = true ORDER BY priority DESC")
    fun getEnabledRules(): Flow<List<ClassificationRule>>
    
    @Query("SELECT * FROM classification_rules WHERE isEnabled = false ORDER BY priority DESC")
    fun getDisabledRules(): Flow<List<ClassificationRule>>
    
    // Query by type - 按类型查询
    
    @Query("SELECT * FROM classification_rules WHERE isSystemRule = true ORDER BY priority DESC")
    fun getSystemRules(): Flow<List<ClassificationRule>>
    
    @Query("SELECT * FROM classification_rules WHERE isSystemRule = false ORDER BY priority DESC")
    fun getCustomRules(): Flow<List<ClassificationRule>>
    
    // Search operations - 搜索操作
    
    @Query("""
        SELECT * FROM classification_rules 
        WHERE ruleName LIKE '%' || :query || '%'
           OR ruleDescription LIKE '%' || :query || '%'
        ORDER BY priority DESC
    """)
    fun search(query: String): Flow<List<ClassificationRule>>
    
    // Priority operations - 优先级操作
    
    @Query("SELECT * FROM classification_rules ORDER BY priority DESC LIMIT :limit")
    fun getTopPriorityRules(limit: Int): Flow<List<ClassificationRule>>
    
    // Count operations - 统计操作
    
    @Query("SELECT COUNT(*) FROM classification_rules")
    suspend fun countAll(): Int
    
    @Query("SELECT COUNT(*) FROM classification_rules WHERE isEnabled = true")
    suspend fun countEnabled(): Int
    
    @Query("SELECT COUNT(*) FROM classification_rules WHERE isEnabled = false")
    suspend fun countDisabled(): Int
    
    @Query("SELECT COUNT(*) FROM classification_rules WHERE isSystemRule = true")
    suspend fun countSystemRules(): Int
    
    @Query("SELECT COUNT(*) FROM classification_rules WHERE isSystemRule = false")
    suspend fun countCustomRules(): Int
    
    // Update operations - 更新操作
    
    @Query("UPDATE classification_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateEnabledStatus(id: Long, isEnabled: Boolean)
    
    @Query("UPDATE classification_rules SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Long, priority: Int)
    
    // Batch operations - 批量操作
    
    @Query("UPDATE classification_rules SET isEnabled = :isEnabled WHERE id IN (:ids)")
    suspend fun updateEnabledStatusBatch(ids: List<Long>, isEnabled: Boolean)
    
    @Query("DELETE FROM classification_rules WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    // Statistics - 统计
    
    @Query("""
        SELECT targetCategory, COUNT(*) as count 
        FROM classification_rules 
        GROUP BY targetCategory
    """)
    suspend fun getCountByCategory(): List<CategoryCount>
    
    @Query("""
        SELECT targetSubCategory, COUNT(*) as count 
        FROM classification_rules 
        WHERE targetSubCategory IS NOT NULL
        GROUP BY targetSubCategory
    """)
    suspend fun getCountBySubCategory(): List<SubCategoryCount>
}

/**
 * Category count result - 分类统计结果
 */
data class CategoryCount(
    val targetCategory: ReceiptCategory,
    val count: Int
)

/**
 * Sub-category count result - 子分类统计结果
 */
data class SubCategoryCount(
    val targetSubCategory: ExpenseSubCategory,
    val count: Int
)