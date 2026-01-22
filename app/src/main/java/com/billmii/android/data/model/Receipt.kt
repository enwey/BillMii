package com.billmii.android.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Receipt entity - 票据实体
 * Stores all receipt information including OCR extracted data
 */
@Entity(tableName = "receipts")
@Parcelize
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Basic information - 基本信息
    val fileName: String,                    // File name
    val filePath: String,                    // File path
    val fileHash: String,                    // File hash for duplicate detection
    val fileSize: Long,                      // File size in bytes
    val fileType: String,                    // File type (PDF, JPG, PNG, TIFF)
    
    // Receipt type - 票据类型
    val receiptType: ReceiptType,            // Receipt type enum
    val receiptCategory: ReceiptCategory,    // Receipt category
    val expenseSubCategory: ExpenseSubCategory? = null,  // Expense sub category
    
    // OCR extracted fields - OCR识别字段
    // Invoice specific fields - 发票专用字段
    val invoiceCode: String? = null,         // 发票代码
    val invoiceNumber: String? = null,       // 发票号码
    val invoiceDate: Date? = null,           // 开票日期
    val buyerName: String? = null,           // 购买方名称
    val buyerTaxId: String? = null,          // 购买方税号
    val sellerName: String? = null,         // 销售方名称
    val sellerTaxId: String? = null,        // 销售方税号
    val totalAmount: Double? = null,         // 价税合计金额
    val amountWithoutTax: Double? = null,    // 不含税金额
    val taxRate: Double? = null,             // 税率
    val taxAmount: Double? = null,           // 税额
    val invoiceStatus: InvoiceStatus? = null, // 发票状态
    
    // Expense specific fields - 费用专用字段
    val expenseDate: Date? = null,           // 费用日期
    val departurePlace: String? = null,      // 出发地
    val destination: String? = null,         // 目的地
    val expenseType: String? = null,         // 费用类型
    
    // General fields - 通用字段
    val issuer: String? = null,              // 开票方
    val amount: Double? = null,              // 金额
    val remarks: String? = null,             // 备注
    
    // Classification and archiving - 分类归档
    val archiveNumber: String? = null,      // 归档编号 (年份-月份-分类编码-流水号)
    val archivePath: String? = null,         // 归档路径
    val tags: String? = null,                // 标签 (JSON array string)
    
    // Reimbursement关联
    val reimbursementId: Long? = null,       // 关联的报销单ID
    
    // Status and validation - 状态和校验
    val ocrStatus: OcrStatus = OcrStatus.PENDING,  // OCR识别状态
    val validationStatus: ValidationStatus = ValidationStatus.PENDING,  // 校验状态
    val validationErrors: String? = null,     // 校验错误信息 (JSON array string)
    
    // Timestamps - 时间戳
    val createdAt: Date = Date(),            // 创建时间
    val recognizedAt: Date? = null,          // 识别时间
    val updatedAt: Date = Date(),            // 更新时间
    
    // User and modification info - 用户和修改信息
    val createdBy: String? = null,           // 创建人
    val modifiedBy: String? = null,          // 修改人
    val modificationHistory: String? = null,  // 修改历史 (JSON array string)
    
    // Storage location - 存储位置
    val storageLocation: StorageLocation = StorageLocation.INTERNAL,  // 存储位置
    val isBackedUp: Boolean = false,         // 是否已备份
    
    // Processing flags - 处理标志
    val isProcessed: Boolean = false,        // 是否已处理
    val isDuplicate: Boolean = false,        // 是否重复
    val isValid: Boolean = true              // 是否有效
) : Parcelable

/**
 * Invoice status enumeration - 发票状态
 */
enum class InvoiceStatus {
    VALID,           // 有效
    INVALID,         // 无效
    REVOKED,         // 已作废
    PENDING_VERIFICATION  // 待验证
}

/**
 * OCR status enumeration - OCR识别状态
 */
enum class OcrStatus {
    PENDING,         // 待识别
    PROCESSING,      // 识别中
    SUCCESS,         // 识别成功
    FAILED,          // 识别失败
    PARTIAL          // 部分识别
}

/**
 * Validation status enumeration - 校验状态
 */
enum class ValidationStatus {
    PENDING,         // 待校验
    VALID,           // 校验通过
    WARNING,         // 有警告
    INVALID          // 校验失败
}

/**
 * Storage location enumeration - 存储位置
 */
enum class StorageLocation {
    INTERNAL,        // 内部存储
    SD_CARD          // SD卡
}