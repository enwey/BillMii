package com.billmii.android.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.billmii.android.R
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.model.ReimbursementStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知服务
 * 负责应用内和系统通知
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "billmii_notifications"
        private const val CHANNEL_NAME = "BillMii Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for reimbursement and receipt updates"
        
        // Notification IDs
        private const val NOTIFICATION_ID_APPROVAL = 1001
        private const val NOTIFICATION_ID_REMINDER = 1002
        private const val NOTIFICATION_ID_EXPIRED = 1003
        private const val NOTIFICATION_ID_SYSTEM = 1004
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 发送审批通知
     */
    fun sendApprovalNotification(
        reimbursement: Reimbursement,
        message: String? = null
    ) {
        val title = when (reimbursement.status) {
            ReimbursementStatus.PENDING -> "待审批报销单"
            ReimbursementStatus.APPROVED -> "报销单已通过"
            ReimbursementStatus.REJECTED -> "报销单已拒绝"
            else -> "报销单状态更新"
        }
        
        val content = message ?: when (reimbursement.status) {
            ReimbursementStatus.PENDING -> "${reimbursement.applicant} 提交的报销单等待审批"
            ReimbursementStatus.APPROVED -> "您的报销单「${reimbursement.title}」已通过审批"
            ReimbursementStatus.REJECTED -> "您的报销单「${reimbursement.title}」已被拒绝"
            else -> "报销单「${reimbursement.title}」状态已更新"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_APPROVAL + reimbursement.id.toInt(), notification)
    }
    
    /**
     * 发送提醒通知
     */
    fun sendReminderNotification(
        reimbursementId: Long,
        title: String,
        message: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_REMINDER + reimbursementId.toInt(), notification)
    }
    
    /**
     * 发送过期通知
     */
    fun sendExpirationNotification(
        reimbursementId: Long,
        title: String,
        message: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_EXPIRED + reimbursementId.toInt(), notification)
    }
    
    /**
     * 发送系统通知
     */
    fun sendSystemNotification(
        title: String,
        message: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_SYSTEM, notification)
    }
    
    /**
     * 清除指定通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    /**
     * 清除所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * 应用内通知数据类
     */
    data class AppNotification(
        val id: String,
        val type: NotificationType,
        val title: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isRead: Boolean = false,
        val reimbursementId: Long? = null,
        val actionUrl: String? = null
    )
    
    /**
     * 通知类型
     */
    enum class NotificationType {
        APPROVAL_PENDING,      // 待审批
        APPROVAL_APPROVED,      // 已通过
        APPROVAL_REJECTED,      // 已拒绝
        REMINDER,               // 提醒
        EXPIRATION,            // 过期
        SYSTEM                  // 系统通知
    }
    
    /**
     * 创建应用内通知
     */
    fun createAppNotification(
        type: NotificationType,
        title: String,
        message: String,
        reimbursementId: Long? = null,
        actionUrl: String? = null
    ): AppNotification {
        return AppNotification(
            id = generateNotificationId(),
            type = type,
            title = title,
            message = message,
            reimbursementId = reimbursementId,
            actionUrl = actionUrl
        )
    }
    
    /**
     * 生成通知ID
     */
    private fun generateNotificationId(): String {
        return "notification_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    /**
     * 获取通知优先级
     */
    fun getNotificationPriority(type: NotificationType): Int {
        return when (type) {
            NotificationType.APPROVAL_PENDING,
            NotificationType.APPROVAL_APPROVED,
            NotificationType.APPROVAL_REJECTED -> NotificationCompat.PRIORITY_HIGH
            NotificationType.EXPIRATION -> NotificationCompat.PRIORITY_HIGH
            NotificationType.REMINDER -> NotificationCompat.PRIORITY_DEFAULT
            NotificationType.SYSTEM -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    /**
     * 格式化通知消息
     */
    fun formatApprovalMessage(
        reimbursement: Reimbursement,
        action: String,
        actor: String? = null
    ): String {
        return when (reimbursement.status) {
            ReimbursementStatus.PENDING -> {
                "${actor ?: "系统"}: ${reimbursement.applicant} 提交的报销单「${reimbursement.title}」等待审批"
            }
            ReimbursementStatus.APPROVED -> {
                "${actor ?: "审批人"}: 报销单「${reimbursement.title}」已通过审批"
            }
            ReimbursementStatus.REJECTED -> {
                "${actor ?: "审批人"}: 报销单「${reimbursement.title}」已被拒绝"
            }
            else -> {
                "报销单「${reimbursement.title}」状态已更新"
            }
        }
    }
}