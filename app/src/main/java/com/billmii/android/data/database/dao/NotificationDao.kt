package com.billmii.android.data.database.dao

import androidx.room.*
import com.billmii.android.data.model.AppNotification
import com.billmii.android.data.model.NotificationType
import kotlinx.coroutines.flow.Flow

/**
 * Notification Data Access Object
 * Provides database operations for notifications
 */
@Dao
interface NotificationDao {
    
    // Basic CRUD operations - 基础增删改查
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotification)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)
    
    @Update
    suspend fun update(notification: AppNotification)
    
    @Delete
    suspend fun delete(notification: AppNotification)
    
    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM app_notifications")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM app_notifications WHERE id = :id")
    suspend fun getById(id: String): AppNotification?
    
    // Query operations - 查询操作
    
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>
    
    @Query("SELECT * FROM app_notifications WHERE isRead = false ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<AppNotification>>
    
    @Query("SELECT * FROM app_notifications WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: NotificationType): Flow<List<AppNotification>>
    
    @Query("SELECT * FROM app_notifications WHERE reimbursementId = :reimbursementId ORDER BY timestamp DESC")
    fun getByReimbursementId(reimbursementId: Long): Flow<List<AppNotification>>
    
    @Query("SELECT * FROM app_notifications WHERE timestamp >= :startDate ORDER BY timestamp DESC")
    fun getNotificationsSince(startDate: String): Flow<List<AppNotification>>
    
    // Update operations - 更新操作
    
    @Query("UPDATE app_notifications SET isRead = true WHERE id = :id")
    suspend fun markAsRead(id: String)
    
    @Query("UPDATE app_notifications SET isRead = true WHERE isRead = false")
    suspend fun markAllAsRead()
    
    @Query("UPDATE app_notifications SET isRead = true WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<String>)
    
    // Count operations - 统计操作
    
    @Query("SELECT COUNT(*) FROM app_notifications")
    suspend fun countAll(): Int
    
    @Query("SELECT COUNT(*) FROM app_notifications WHERE isRead = false")
    suspend fun countUnread(): Int
    
    @Query("SELECT COUNT(*) FROM app_notifications WHERE type = :type")
    suspend fun countByType(type: NotificationType): Int
    
    @Query("SELECT COUNT(*) FROM app_notifications WHERE reimbursementId = :reimbursementId")
    suspend fun countByReimbursementId(reimbursementId: Long): Int
    
    // Cleanup operations - 清理操作
    
    @Query("DELETE FROM app_notifications WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: String)
    
    @Query("DELETE FROM app_notifications WHERE isRead = true AND timestamp < :timestamp")
    suspend fun deleteReadOlderThan(timestamp: String)
    
    // Batch operations - 批量操作
    
    @Query("DELETE FROM app_notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}