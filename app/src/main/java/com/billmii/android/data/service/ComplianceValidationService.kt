package com.billmii.android.data.service

import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.model.ReceiptType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 报销合规校验服务
 */
@Singleton
class ComplianceValidationService @Inject constructor() {
    
    data class ValidationResult(
        val isCompliant: Boolean,
        val issues: List<ValidationIssue>,
        val warnings: List<ValidationWarning>,
        val score: Int // 0-100
    )
    
    data class ValidationIssue(
        val code: String,
        val message: String,
        val severity: Severity,
        val affectedItems: List<String>
    )
    
    data class ValidationWarning(
        val code: String,
        val message: String,
        val suggestion: String?
    )
    
    enum class Severity {
        ERROR,      // 必须修正才能提交
        WARNING,    // 建议修正但可提交
        INFO        // 信息提示
    }
    
    data class ComplianceRule(
        val category: String,
        val monthlyLimit: Double?,
        val singleLimit: Double?,
        val requiredFields: List<String>,
        val description: String
    )
    
    // 默认合规规则
    private val defaultRules = mapOf(
        ReceiptType.TRANSPORT to ComplianceRule(
            category = "交通费",
            monthlyLimit = 2000.0,
            singleLimit = 500.0,
            requiredFields = listOf("amount", "date", "merchant"),
            description = "交通费包括出差交通、市内交通等"
        ),
        ReceiptType.DINING to ComplianceRule(
            category = "餐饮费",
            monthlyLimit = 3000.0,
            singleLimit = 1000.0,
            requiredFields = listOf("amount", "date", "merchant", "attendees"),
            description = "餐饮费包括商务宴请、工作餐等"
        ),
        ReceiptType.ACCOMMODATION to ComplianceRule(
            category = "住宿费",
            monthlyLimit = 5000.0,
            singleLimit = 800.0,
            requiredFields = listOf("amount", "date", "merchant", "checkInDate", "checkOutDate"),
            description = "住宿费包括酒店、民宿等"
        ),
        ReceiptType.OFFICE to ComplianceRule(
            category = "办公费",
            monthlyLimit = 1000.0,
            singleLimit = 200.0,
            requiredFields = listOf("amount", "date", "merchant", "itemDescription"),
            description = "办公费包括办公用品、设备等"
        ),
        ReceiptType.COMMUNICATION to ComplianceRule(
            category = "通讯费",
            monthlyLimit = 500.0,
            singleLimit = 100.0,
            requiredFields = listOf("amount", "date", "merchant", "phoneNumber"),
            description = "通讯费包括电话费、网络费等"
        ),
        ReceiptType.OTHER to ComplianceRule(
            category = "其他费用",
            monthlyLimit = 1000.0,
            singleLimit = 300.0,
            requiredFields = listOf("amount", "date", "merchant", "description"),
            description = "其他费用需特别说明"
        )
    )
    
    /**
     * 验证报销单的合规性
     */
    suspend fun validateReimbursement(
        reimbursement: Reimbursement,
        receipts: List<Receipt>
    ): ValidationResult = withContext(Dispatchers.Default) {
        val issues = mutableListOf<ValidationIssue>()
        val warnings = mutableListOf<ValidationWarning>()
        
        // 1. 验证基本信息
        validateBasicInfo(reimbursement, issues)
        
        // 2. 验证票据
        validateReceipts(receipts, reimbursement, issues, warnings)
        
        // 3. 验证金额
        validateAmounts(reimbursement, receipts, issues, warnings)
        
        // 4. 验证时间
        validateTimePeriod(reimbursement, receipts, issues, warnings)
        
        // 5. 验证重复提交
        validateDuplicates(receipts, issues)
        
        // 6. 计算合规分数
        val score = calculateComplianceScore(issues, warnings)
        
        ValidationResult(
            isCompliant = issues.none { it.severity == Severity.ERROR },
            issues = issues,
            warnings = warnings,
            score = score
        )
    }
    
    private fun validateBasicInfo(
        reimbursement: Reimbursement,
        issues: MutableList<ValidationIssue>
    ) {
        // 验证标题
        if (reimbursement.title.isBlank()) {
            issues.add(ValidationIssue(
                code = "EMPTY_TITLE",
                message = "报销单标题不能为空",
                severity = Severity.ERROR,
                affectedItems = emptyList()
            ))
        }
        
        // 验证申请金额
        if (reimbursement.totalAmount <= 0) {
            issues.add(ValidationIssue(
                code = "INVALID_AMOUNT",
                message = "申请金额必须大于0",
                severity = Severity.ERROR,
                affectedItems = emptyList()
            ))
        }
        
        // 验证申请人
        if (reimbursement.applicant.isBlank()) {
            issues.add(ValidationIssue(
                code = "EMPTY_APPLICANT",
                message = "申请人信息不能为空",
                severity = Severity.ERROR,
                affectedItems = emptyList()
            ))
        }
    }
    
    private fun validateReceipts(
        receipts: List<Receipt>,
        reimbursement: Reimbursement,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<ValidationWarning>
    ) {
        if (receipts.isEmpty()) {
            issues.add(ValidationIssue(
                code = "NO_RECEIPTS",
                message = "报销单必须包含至少一张票据",
                severity = Severity.ERROR,
                affectedItems = emptyList()
            ))
            return
        }
        
        receipts.forEach { receipt ->
            // 验证必需字段
            val rule = defaultRules[receipt.type]
            if (rule != null) {
                rule.requiredFields.forEach { field ->
                    when (field) {
                        "amount" -> {
                            if (receipt.amount <= 0) {
                                issues.add(ValidationIssue(
                                    code = "MISSING_AMOUNT",
                                    message = "票据金额无效",
                                    severity = Severity.ERROR,
                                    affectedItems = listOf(receipt.title)
                                ))
                            }
                        }
                        "date" -> {
                            if (receipt.date == null) {
                                issues.add(ValidationIssue(
                                    code = "MISSING_DATE",
                                    message = "票据日期缺失",
                                    severity = Severity.ERROR,
                                    affectedItems = listOf(receipt.title)
                                ))
                            }
                        }
                        "merchant" -> {
                            if (receipt.merchant.isBlank()) {
                                warnings.add(ValidationWarning(
                                    code = "MISSING_MERCHANT",
                                    message = "票据商户信息缺失",
                                    suggestion = "建议补充商户名称"
                                ))
                            }
                        }
                        "attendees" -> {
                            if (receipt.type == ReceiptType.DINING && receipt.attendees.isEmpty()) {
                                warnings.add(ValidationWarning(
                                    code = "MISSING_ATTENDEES",
                                    message = "餐饮费建议记录参与人员",
                                    suggestion = "请补充参与人员信息"
                                ))
                            }
                        }
                        "itemDescription" -> {
                            if (receipt.type == ReceiptType.OFFICE && receipt.description.isBlank()) {
                                warnings.add(ValidationWarning(
                                    code = "MISSING_DESCRIPTION",
                                    message = "办公费建议补充物品说明",
                                    suggestion = "请补充购买的物品信息"
                                ))
                            }
                        }
                    }
                }
            }
            
            // 验证票据图片
            if (receipt.imagePath.isBlank()) {
                warnings.add(ValidationWarning(
                    code = "MISSING_IMAGE",
                    message = "建议上传票据图片",
                    suggestion = "上传图片有助于审核"
                ))
            }
            
            // 验证OCR识别结果
            if (receipt.ocrResult.isBlank()) {
                warnings.add(ValidationWarning(
                    code = "NO_OCR",
                    message = "票据未进行OCR识别",
                    suggestion = "建议进行OCR识别以提高审核效率"
                ))
            }
        }
    }
    
    private fun validateAmounts(
        reimbursement: Reimbursement,
        receipts: List<Receipt>,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<ValidationWarning>
    ) {
        // 计算票据总金额
        val receiptTotal = receipts.sumOf { it.amount }
        
        // 验证金额一致性
        if (kotlin.math.abs(reimbursement.totalAmount - receiptTotal) > 0.01) {
            warnings.add(ValidationWarning(
                code = "AMOUNT_MISMATCH",
                message = "申请金额与票据总金额不一致",
                suggestion = "申请金额: ¥${reimbursement.totalAmount}, 票据总额: ¥$receiptTotal"
            ))
        }
        
        // 按类型验证单张票据金额限制
        receipts.groupBy { it.type }.forEach { (type, typeReceipts) ->
            val rule = defaultRules[type]
            if (rule?.singleLimit != null) {
                typeReceipts.forEach { receipt ->
                    if (receipt.amount > rule.singleLimit) {
                        issues.add(ValidationIssue(
                            code = "EXCEED_SINGLE_LIMIT",
                            message = "${rule.category}单张票据超过限额 ¥${rule.singleLimit}",
                            severity = Severity.WARNING,
                            affectedItems = listOf(receipt.title)
                        ))
                    }
                }
            }
        }
    }
    
    private fun validateTimePeriod(
        reimbursement: Reimbursement,
        receipts: List<Receipt>,
        issues: MutableList<ValidationIssue>,
        warnings: MutableList<ValidationWarning>
    ) {
        if (receipts.isEmpty()) return
        
        // 验证票据日期范围
        val dates = receipts.mapNotNull { it.date }.filterNotNull()
        if (dates.isNotEmpty()) {
            val minDate = dates.minOrNull()
            val maxDate = dates.maxOrNull()
            
            if (minDate != null && maxDate != null) {
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate)
                
                if (daysBetween > 30) {
                    warnings.add(ValidationWarning(
                        code = "WIDE_DATE_RANGE",
                        message = "票据日期跨度较大（${daysBetween}天）",
                        suggestion = "建议按月分批报销"
                    ))
                }
                
                // 验证票据日期是否在报销月份内
                val reimbursementMonth = reimbursement.createdAt.substring(0, 7) // yyyy-MM
                val outOfMonthReceipts = dates.count { 
                    it.format(DateTimeFormatter.ofPattern("yyyy-MM")) != reimbursementMonth 
                }
                
                if (outOfMonthReceipts > 0) {
                    warnings.add(ValidationWarning(
                        code = "OUT_OF_MONTH",
                        message = "有$outOfMonthReceipts 张票据不在报销月份内",
                        suggestion = "请确认是否需要跨月报销"
                    ))
                }
            }
        }
    }
    
    private fun validateDuplicates(
        receipts: List<Receipt>,
        issues: MutableList<ValidationIssue>
    ) {
        // 检查重复票据（基于金额+商户+日期）
        val receiptKeys = receipts.map { 
            "${it.amount}_${it.merchant}_${it.date}" 
        }
        val duplicates = receiptKeys.groupingBy { it }.eachCount().filter { it.value > 1 }
        
        if (duplicates.isNotEmpty()) {
            issues.add(ValidationIssue(
                code = "DUPLICATE_RECEIPTS",
                message = "检测到${duplicates.size}组疑似重复票据",
                severity = Severity.WARNING,
                affectedItems = duplicates.keys.toList()
            ))
        }
    }
    
    private fun calculateComplianceScore(
        issues: List<ValidationIssue>,
        warnings: List<ValidationWarning>
    ): Int {
        var score = 100
        
        // 错误扣分（每个错误扣20分）
        val errorCount = issues.count { it.severity == Severity.ERROR }
        score -= errorCount * 20
        
        // 警告扣分（每个警告扣5分）
        val warningCount = warnings.size
        score -= warningCount * 5
        
        // 确保分数在0-100之间
        return score.coerceIn(0, 100)
    }
    
    /**
     * 获取合规规则列表
     */
    fun getComplianceRules(): Map<ReceiptType, ComplianceRule> = defaultRules
    
    /**
     * 更新合规规则
     */
    fun updateComplianceRule(type: ReceiptType, rule: ComplianceRule) {
        // TODO: 实现规则持久化
    }
    
    /**
     * 获取合规建议
     */
    fun getComplianceSuggestions(
        reimbursement: Reimbursement,
        receipts: List<Receipt>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 建议补充信息
        receipts.forEach { receipt ->
            if (receipt.merchant.isBlank()) {
                suggestions.add("建议补充「${receipt.title}」的商户信息")
            }
            if (receipt.description.isBlank() && receipt.type == ReceiptType.OTHER) {
                suggestions.add("建议补充「${receipt.title}」的详细说明")
            }
        }
        
        // 建议分类
        val typeCounts = receipts.groupingBy { it.type }.eachCount()
        if (typeCounts.size > 3) {
            suggestions.add("建议将多种费用类型拆分为多个报销单")
        }
        
        return suggestions
    }
}