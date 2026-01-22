package com.billmii.android.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Operation log entity - 操作日志实体
 * Records all operations for audit purposes
 */
@Entity(tableName = "operation_logs")
@Parcelize
data class OperationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Operation information - 操作信息
    val operationType: OperationType,       // 操作类型
    val operationModule: OperationModule,   // 操作模块
    val operationDescription: String,       // 操作描述
    
    // Related entities - 关联实体
    val receiptId: Long? = null,             // 关联的票据ID
    val reimbursementId: Long? = null,      // 关联的报销单ID
    val ruleId: Long? = null,                // 关联的规则ID
    
    // Operator information - 操作人信息
    val operator: String,                    // 操作人
    val operatorRole: String? = null,        // 操作人角色
    
    // Operation details - 操作详情
    val oldValue: String? = null,            // 旧值 (JSON string)
    val newValue: String? = null,            // 新值 (JSON string)
    val operationResult: OperationResult,    // 操作结果
    val errorMessage: String? = null,        // 错误信息
    
    // Device and location - 设备和位置
    val deviceId: String? = null,            // 设备ID
    val deviceModel: String? = null,         // 设备型号
    val ipAddress: String? = null,           // IP地址
    
    // Timestamp - 时间戳
    val operationTime: Date = Date(),        // 操作时间
    
    // Additional metadata - 附加元数据
    val additionalInfo: String? = null,      // 附加信息 (JSON string)
    val isDeleted: Boolean = false          // 是否已删除 (日志不可删除，此字段标记为软删除)
) : Parcelable

/**
 * Operation type enumeration - 操作类型
 */
enum class OperationType {
    // Receipt operations - 票据操作
    RECEIPT_CREATE,          // 创建票据
    RECEIPT_IMPORT,          // 导入票据
    RECEIPT_CAPTURE,         // 拍摄票据
    RECEIPT_UPDATE,          // 更新票据
    RECEIPT_DELETE,          // 删除票据
    RECEIPT_RECOGNIZE,       // OCR识别
    RECEIPT_VALIDATE,        // 校验票据
    RECEIPT_CLASSIFY,        // 分类票据
    RECEIPT_ARCHIVE,         // 归档票据
    RECEIPT_EXPORT,          // 导出票据
    RECEIPT_BATCH_DELETE,    // 批量删除票据
    RECEIPT_BATCH_UPDATE,    // 批量更新票据
    
    // Reimbursement operations - 报销操作
    REIMBURSEMENT_CREATE,    // 创建报销单
    REIMBURSEMENT_UPDATE,    // 更新报销单
    REIMBURSEMENT_DELETE,    // 删除报销单
    REIMBURSEMENT_SUBMIT,    // 提交报销单
    REIMBURSEMENT_APPROVE,   // 审批报销单
    REIMBURSEMENT_REJECT,    // 拒绝报销单
    REIMBURSEMENT_VALIDATE,  // 校验报销单
    REIMBURSEMENT_EXPORT,    // 导出报销单
    
    // Classification rule operations - 分类规则操作
    RULE_CREATE,             // 创建规则
    RULE_UPDATE,             // 更新规则
    RULE_DELETE,             // 删除规则
    RULE_ENABLE,             // 启用规则
    RULE_DISABLE,            // 禁用规则
    
    // Backup and restore operations - 备份恢复操作
    BACKUP_CREATE,           // 创建备份
    BACKUP_RESTORE,          // 恢复备份
    BACKUP_AUTO,             // 自动备份
    
    // Data export operations - 数据导出操作
    DATA_EXPORT_EXCEL,       // 导出Excel
    DATA_EXPORT_PDF,         // 导出PDF
    DATA_EXPORT_CUSTOM,      // 自定义导出
    
    // System operations - 系统操作
    USER_LOGIN,              // 用户登录
    USER_LOGOUT,             // 用户登出
    SETTINGS_UPDATE,         // 更新设置
    SYSTEM_SYNC,             // 系统同步
    OTHER                    // 其他操作
}

/**
 * Operation module enumeration - 操作模块
 */
enum class OperationModule {
    COLLECTION,              // 采集模块
    OCR,                     // OCR模块
    CLASSIFICATION,          // 分类模块
    REIMBURSEMENT,           // 报销模块
    EXPORT,                  // 导出模块
    BACKUP,                  // 备份模块
    SETTINGS,                // 设置模块
    SECURITY,                // 安全模块
    OTHER                    // 其他模块
}

/**
 * Operation result enumeration - 操作结果
 */
enum class OperationResult {
    SUCCESS,                 // 成功
    FAILED,                  // 失败
    WARNING,                 // 警告
    PARTIAL                  // 部分成功
}