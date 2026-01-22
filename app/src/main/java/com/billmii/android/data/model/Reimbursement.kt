package com.billmii.android.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Reimbursement entity - 报销单实体
 * Contains reimbursement information and related receipts
 */
@Entity(tableName = "reimbursements")
@Parcelize
data class Reimbursement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Basic information - 基本信息
    val reimbursementNumber: String,         // 报销单编号
    val title: String,                      // 报销标题
    val description: String? = null,        // 报销说明/事由
    
    // Applicant information - 申请人信息
    val applicant: String,                  // 申请人姓名
    val department: String? = null,         // 部门
    val position: String? = null,           // 职位
    val employeeId: String? = null,          // 员工工号
    
    // Financial information - 财务信息
    val totalAmount: Double,                // 报销总金额
    val currency: String = "CNY",           // 货币类型
    val advancePayment: Double = 0.0,       // 预付款金额
    val refundAmount: Double = 0.0,         // 退款金额
    
    // Template type - 模板类型
    val templateType: ReimbursementTemplate = ReimbursementTemplate.PERSONAL,  // 报销模板类型
    val customFields: String? = null,       // 自定义字段 (JSON object string)
    
    // Compliance validation - 合规校验
    val validationStatus: ReimbursementValidationStatus = ReimbursementValidationStatus.PENDING,  // 校验状态
    val validationResults: String? = null,  // 校验结果 (JSON array string)
    val complianceNotes: String? = null,   // 合规说明
    
    // Approval workflow - 审批流程
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,  // 审批状态
    val currentStep: Int = 0,                // 当前审批步骤
    val totalSteps: Int = 3,                 // 总审批步骤数
    val approvalWorkflow: String? = null,   // 审批流程配置 (JSON array string)
    val approvalHistory: String? = null,    // 审批历史 (JSON array string)
    
    // Timestamps - 时间戳
    val createdAt: Date = Date(),            // 创建时间
    val submittedAt: Date? = null,          // 提交时间
    val approvedAt: Date? = null,            // 审批通过时间
    val rejectedAt: Date? = null,            // 审批拒绝时间
    val updatedAt: Date = Date(),            // 更新时间
    
    // Additional information - 附加信息
    val remarks: String? = null,            // 备注
    val attachmentCount: Int = 0,           // 附件数量
    val isExported: Boolean = false,        // 是否已导出
    
    // Voucher generation - 凭证生成
    val voucherNumber: String? = null,      // 凭证编号
    val voucherGeneratedAt: Date? = null,   // 凭证生成时间
    val isArchived: Boolean = false         // 是否已归档
) : Parcelable

/**
 * Reimbursement with receipts - 报销单及其关联的票据
 */
data class ReimbursementWithReceipts(
    @Embedded
    val reimbursement: Reimbursement,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "reimbursementId"
    )
    val receipts: List<Receipt>
)

/**
 * Reimbursement template enumeration - 报销模板类型
 */
enum class ReimbursementTemplate {
    PERSONAL,          // 个人报销
    TEAM,              // 团队报销
    PROJECT,           // 项目报销
    CUSTOM             // 自定义模板
}

/**
 * Reimbursement validation status enumeration - 报销校验状态
 */
enum class ReimbursementValidationStatus {
    PENDING,           // 待校验
    PASSED,            // 校验通过
    WARNING,           // 有警告
    FAILED             // 校验失败
}

/**
 * Approval status enumeration - 审批状态
 */
enum class ApprovalStatus {
    DRAFT,             // 草稿
    PENDING,           // 待审批
    IN_REVIEW,         // 审批中
    APPROVED,          // 已批准
    REJECTED,          // 已拒绝
    CANCELLED          // 已取消
}

/**
 * Approval step model - 审批步骤
 */
data class ApprovalStep(
    val stepNumber: Int,
    val stepName: String,                   // 步骤名称
    val approver: String,                   // 审批人
    val approverRole: String,               // 审批人角色
    val status: ApprovalStatus,             // 状态
    val comment: String? = null,            // 审批意见
    val approvedAt: Date? = null            // 审批时间
)

/**
 * Validation result model - 校验结果
 */
data class ValidationResult(
    val validationType: String,             // 校验类型 (基础校验/标准校验/完整性校验)
    val isPassed: Boolean,                  // 是否通过
    val errorMessage: String? = null,       // 错误信息
    val warningMessage: String? = null,     // 警告信息
    val relatedReceiptId: Long? = null      // 关联的票据ID
)

/**
 * Compliance standard model - 合规标准
 */
data class ComplianceStandard(
    val expenseType: String,                // 费用类型
    val amountLimit: Double,                // 金额上限
    val requiredDocuments: List<String>,    // 必需票据
    val description: String? = null         // 说明
)