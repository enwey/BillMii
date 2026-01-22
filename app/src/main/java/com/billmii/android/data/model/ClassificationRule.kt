package com.billmii.android.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Classification rule entity - 分类规则实体
 * Defines rules for automatic receipt classification
 */
@Entity(tableName = "classification_rules")
@Parcelize
data class ClassificationRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Rule information - 规则信息
    val ruleName: String,                   // 规则名称
    val ruleDescription: String? = null,     // 规则描述
    val isEnabled: Boolean = true,           // 是否启用
    val priority: Int = 0,                    // 优先级 (数字越大优先级越高)
    
    // Classification target - 分类目标
    val targetCategory: ReceiptCategory,      // 目标分类
    val targetSubCategory: ExpenseSubCategory? = null,  // 目标子分类
    val tags: String? = null,                // 标签
    
    // Rule conditions - 规则条件 (JSON array string)
    val conditions: String,                  // 条件列表
    
    // Rule actions - 规则动作
    val autoAssignArchiveNumber: Boolean = false,  // 自动分配归档编号
    val autoApplyTags: Boolean = false,      // 自动应用标签
    val moveToArchivePath: String? = null,   // 移动到归档路径
    
    // Metadata - 元数据
    val isSystemRule: Boolean = false,       // 是否系统规则
    val createdBy: String? = null,           // 创建人
    val createdAt: Date = Date(),            // 创建时间
    val updatedAt: Date = Date()             // 更新时间
) : Parcelable

/**
 * Classification condition - 分类条件
 */
data class ClassificationCondition(
    val fieldType: FieldType,                 // 字段类型
    val operator: Operator,                  // 操作符
    val value: String,                       // 值
    val logicalOperator: LogicalOperator = LogicalOperator.AND  // 逻辑运算符
)

/**
 * Field type enumeration - 字段类型
 */
enum class FieldType {
    RECEIPT_TYPE,      // 票据类型
    AMOUNT,            // 金额
    DATE,              // 日期
    BUYER_NAME,        // 购买方名称
    SELLER_NAME,       // 销售方名称
    INVOICE_CODE,      // 发票代码
    INVOICE_NUMBER,    // 发票号码
    EXPENSE_TYPE,      // 费用类型
    DEPARTURE_PLACE,   // 出发地
    DESTINATION,       // 目的地
    FILE_NAME,         // 文件名
    REMARKS            // 备注
}

/**
 * Operator enumeration - 操作符
 */
enum class Operator {
    EQUALS,            // 等于
    NOT_EQUALS,        // 不等于
    CONTAINS,          // 包含
    NOT_CONTAINS,      // 不包含
    GREATER_THAN,      // 大于
    LESS_THAN,         // 小于
    GREATER_EQUAL,     // 大于等于
    LESS_EQUAL,        // 小于等于
    STARTS_WITH,       // 以...开头
    ENDS_WITH,         // 以...结尾
    REGEX,             // 正则表达式
    IN_LIST            // 在列表中
}

/**
 * Logical operator enumeration - 逻辑运算符
 */
enum class LogicalOperator {
    AND,               // 与
    OR                 // 或
}