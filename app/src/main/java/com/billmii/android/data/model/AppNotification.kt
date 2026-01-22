package com.billmii.android.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 应用内通知实体
 */
@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val reimbursementId: Long? = null,
    val actionUrl: String? = null
) {
    companion object {
        fun create(
            type: NotificationType,
            title: String,
            message: String,
            reimbursementId: Long? = null,
            actionUrl: String? = null
        ): AppNotification {
            return AppNotification(
                id = generateId(),
                type = type,
                title = title,
                message = message,
                timestamp = LocalDateTime.now().toString(),
                isRead = false,
                reimbursementId = reimbursementId,
                actionUrl = actionUrl
            )
        }
        
        private fun generateId(): String {
            return "notification_${System.currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}

/**
 * 通知类型
 */
enum class NotificationType {
    APPROVAL_PENDING,      // 待审批
    APPROVAL_APPROVED,      // 已通过
    APPROVAL_REJECTED,      // 已拒绝
    REMINDER,               // 提醒
    EXPIRATION,            // 过期
    SYSTEM                 // 系统通知
}

/**
 * 通知优先级
 */
enum class NotificationPriority {
    HIGH,      // 高优先级（审批相关）
    MEDIUM,    // 中优先级（提醒）
    LOW        // 低优先级（系统通知）
}

/**
 * 通知类型扩展
 */
fun NotificationType.getPriority(): NotificationPriority {
    return when (this) {
        NotificationType.APPROVAL_PENDING,
        NotificationType.APPROVAL_APPROVED,
        NotificationType.APPROVAL_REJECTED -> NotificationPriority.HIGH
        NotificationType.EXPIRATION -> NotificationPriority.HIGH
        NotificationType.REMINDER -> NotificationPriority.MEDIUM
        NotificationType.SYSTEM -> NotificationPriority.LOW
    }
}

/**
 * 通知类型显示名称
 */
fun NotificationType.getDisplayName(): String {
    return when (this) {
        NotificationType.APPROVAL_PENDING -> "待审批"
        NotificationType.APPROVAL_APPROVED -> "已通过"
        NotificationType.APPROVAL_REJECTED -> "已拒绝"
        NotificationType.REMINDER -> "提醒"
        NotificationType.EXPIRATION -> "过期"
        NotificationType.SYSTEM -> "系统通知"
    }
}