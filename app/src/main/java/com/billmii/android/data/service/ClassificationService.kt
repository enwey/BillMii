package com.billmii.android.data.service

import com.billmii.android.data.database.dao.ClassificationRuleDao
import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classification Service
 * Handles intelligent receipt classification based on rules
 */
@Singleton
class ClassificationService @Inject constructor(
    private val classificationRuleDao: ClassificationRuleDao,
    private val receiptDao: ReceiptDao
) {
    
    companion object {
        private const val TAG = "ClassificationService"
    }
    
    /**
     * Classify a single receipt
     */
    suspend fun classifyReceipt(receiptId: Long): ClassificationResult {
        val receipt = receiptDao.getById(receiptId)
            ?: return ClassificationResult(
                success = false,
                error = "Receipt not found"
            )
        
        // Get enabled rules sorted by priority
        val rules = classificationRuleDao.getEnabledRules().first()
        
        // Apply rules in priority order
        for (rule in rules) {
            if (matchesRule(receipt, rule)) {
                val classifiedReceipt = applyRule(receipt, rule)
                receiptDao.update(classifiedReceipt)
                
                return ClassificationResult(
                    success = true,
                    receipt = classifiedReceipt,
                    ruleApplied = rule,
                    archiveNumber = classifiedReceipt.archiveNumber
                )
            }
        }
        
        // No rule matched, apply default classification
        val defaultClassified = applyDefaultClassification(receipt)
        receiptDao.update(defaultClassified)
        
        return ClassificationResult(
            success = true,
            receipt = defaultClassified,
            ruleApplied = null,
            archiveNumber = defaultClassified.archiveNumber
        )
    }
    
    /**
     * Batch classify receipts
     */
    suspend fun batchClassifyReceipts(receiptIds: List<Long>): BatchClassificationResult {
        val results = mutableListOf<ClassificationResult>()
        val errors = mutableListOf<String>()
        
        for (receiptId in receiptIds) {
            val result = classifyReceipt(receiptId)
            if (result.success) {
                results.add(result)
            } else {
                errors.add("Receipt $receiptId: ${result.error}")
            }
        }
        
        return BatchClassificationResult(
            total = receiptIds.size,
            classified = results.size,
            failed = errors.size,
            results = results,
            errors = errors
        )
    }
    
    /**
     * Auto-classify all unprocessed receipts
     */
    suspend fun autoClassifyAll(): BatchClassificationResult {
        val unprocessedReceipts = receiptDao.getByProcessedStatus(false).first()
        val receiptIds = unprocessedReceipts.map { it.id }
        return batchClassifyReceipts(receiptIds)
    }
    
    /**
     * Check if receipt matches a rule
     */
    private fun matchesRule(receipt: Receipt, rule: ClassificationRule): Boolean {
        return rule.conditions.all { condition ->
            matchesCondition(receipt, condition)
        }
    }
    
    /**
     * Check if receipt matches a single condition
     */
    private fun matchesCondition(receipt: Receipt, condition: ClassificationCondition): Boolean {
        val fieldValue = getFieldValue(receipt, condition.field)
        
        return when (condition.operator) {
            ConditionOperator.EQUALS -> fieldValue == condition.value
            ConditionOperator.CONTAINS -> fieldValue.contains(condition.value, ignoreCase = true)
            ConditionOperator.STARTS_WITH -> fieldValue.startsWith(condition.value, ignoreCase = true)
            ConditionOperator.ENDS_WITH -> fieldValue.endsWith(condition.value, ignoreCase = true)
            ConditionOperator.GREATER_THAN -> compareNumbers(fieldValue, condition.value) > 0
            ConditionOperator.LESS_THAN -> compareNumbers(fieldValue, condition.value) < 0
            ConditionOperator.REGEX -> Regex(condition.value).containsMatchIn(fieldValue)
            ConditionOperator.IN -> condition.value.split(",").any { it.trim() == fieldValue }
            ConditionOperator.NOT_EQUALS -> fieldValue != condition.value
            ConditionOperator.NOT_CONTAINS -> !fieldValue.contains(condition.value, ignoreCase = true)
        }
    }
    
    /**
     * Get field value from receipt
     */
    private fun getFieldValue(receipt: Receipt, field: ClassificationField): String {
        return when (field) {
            ClassificationField.RECEIPT_TYPE -> receipt.receiptType.name
            ClassificationField.RECEIPT_CATEGORY -> receipt.receiptCategory.name
            ClassificationField.SELLER_NAME -> receipt.sellerName ?: ""
            ClassificationField.BUYER_NAME -> receipt.buyerName ?: ""
            ClassificationField.AMOUNT -> (receipt.totalAmount ?: receipt.amount ?: 0.0).toString()
            ClassificationField.DATE -> formatDate(receipt.invoiceDate ?: receipt.createdAt)
            ClassificationField.ISSUER -> receipt.issuer ?: ""
            ClassificationField.EXPENSE_TYPE -> receipt.expenseType ?: ""
            ClassificationField.DEPARTURE_PLACE -> receipt.departurePlace ?: ""
            ClassificationField.DESTINATION -> receipt.destination ?: ""
            ClassificationField.FILE_TYPE -> receipt.fileType
        }
    }
    
    /**
     * Apply rule to receipt
     */
    private fun applyRule(receipt: Receipt, rule: ClassificationRule): Receipt {
        val updatedReceipt = receipt.copy(
            processed = true,
            processedAt = Date(),
            updatedAt = Date()
        )
        
        // Apply actions
        rule.actions.forEach { action ->
            applyAction(updatedReceipt, action)
        }
        
        // Generate archive number if not set
        val archiveNumber = if (updatedReceipt.archiveNumber.isNullOrEmpty()) {
            generateArchiveNumber(updatedReceipt)
        } else {
            updatedReceipt.archiveNumber
        }
        
        return updatedReceipt.copy(archiveNumber = archiveNumber)
    }
    
    /**
     * Apply action to receipt
     */
    private fun applyAction(receipt: Receipt, action: ClassificationAction): Receipt {
        return when (action.type) {
            ActionType.SET_CATEGORY -> receipt.copy(
                receiptCategory = try {
                    ReceiptCategory.valueOf(action.value)
                } catch (e: Exception) {
                    receipt.receiptCategory
                }
            )
            ActionType.SET_SUB_CATEGORY -> receipt.copy(
                expenseSubCategory = try {
                    ExpenseSubCategory.valueOf(action.value)
                } catch (e: Exception) {
                    receipt.expenseSubCategory
                }
            )
            ActionType.SET_EXPENSE_TYPE -> receipt.copy(expenseType = action.value)
            ActionType.SET_DEPARTMENT -> receipt.copy(department = action.value)
            ActionType.SET_PROJECT -> receipt.copy(project = action.value)
            ActionType.SET_TAG -> receipt.copy(
                tags = if (receipt.tags.isNullOrEmpty()) {
                    action.value
                } else {
                    "${receipt.tags},$action.value"
                }
            )
            ActionType.ARCHIVE -> receipt.copy(
                archived = true,
                archivedAt = Date()
            )
            ActionType.GENERATE_ARCHIVE_NUMBER -> receipt.copy(
                archiveNumber = generateArchiveNumber(receipt)
            )
        }
    }
    
    /**
     * Apply default classification
     */
    private fun applyDefaultClassification(receipt: Receipt): Receipt {
        val archiveNumber = generateArchiveNumber(receipt)
        
        return receipt.copy(
            processed = true,
            processedAt = Date(),
            archiveNumber = archiveNumber,
            updatedAt = Date()
        )
    }
    
    /**
     * Generate archive number
     * Format: YYYY-MM-CategoryCode-SerialNumber
     */
    private fun generateArchiveNumber(receipt: Receipt): String {
        val calendar = Calendar.getInstance()
        val date = receipt.invoiceDate ?: receipt.createdAt ?: Date()
        calendar.time = date
        
        val year = calendar.get(Calendar.YEAR)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val categoryCode = getCategoryCode(receipt.receiptCategory)
        
        // Get serial number for this month and category
        val serialNumber = getNextSerialNumber(year, month, categoryCode)
        val serial = String.format("%04d", serialNumber)
        
        return "$year-$month-$categoryCode-$serial"
    }
    
    /**
     * Get category code
     */
    private fun getCategoryCode(category: ReceiptCategory): String {
        return when (category) {
            ReceiptCategory.INCOME -> "INC"
            ReceiptCategory.EXPENSE -> "EXP"
            ReceiptCategory.TRANSPORTATION -> "TRA"
            ReceiptCategory.ACCOMMODATION -> "ACC"
            ReceiptCategory.FOOD -> "FOD"
            ReceiptCategory.OFFICE -> "OFF"
            ReceiptCategory.OTHER -> "OTH"
        }
    }
    
    /**
     * Get next serial number
     */
    private suspend fun getNextSerialNumber(year: Int, month: Int, categoryCode: String): Int {
        val pattern = "$year-$month-$categoryCode-%"
        val existingReceipts = receiptDao.searchByArchiveNumber(pattern).first()
        return existingReceipts.size + 1
    }
    
    /**
     * Compare two numeric values
     */
    private fun compareNumbers(value1: String, value2: String): Int {
        val num1 = value1.toDoubleOrNull() ?: 0.0
        val num2 = value2.toDoubleOrNull() ?: 0.0
        return num1.compareTo(num2)
    }
    
    /**
     * Format date
     */
    private fun formatDate(date: Date): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Get all rules
     */
    fun getAllRules(): Flow<List<ClassificationRule>> {
        return classificationRuleDao.getAllRules()
    }
    
    /**
     * Get enabled rules
     */
    fun getEnabledRules(): Flow<List<ClassificationRule>> {
        return classificationRuleDao.getEnabledRules()
    }
    
    /**
     * Get rule by ID
     */
    suspend fun getRuleById(id: Long): ClassificationRule? {
        return classificationRuleDao.getById(id)
    }
    
    /**
     * Create rule
     */
    suspend fun createRule(rule: ClassificationRule): Long {
        return classificationRuleDao.insert(rule)
    }
    
    /**
     * Update rule
     */
    suspend fun updateRule(rule: ClassificationRule) {
        classificationRuleDao.update(rule)
    }
    
    /**
     * Delete rule
     */
    suspend fun deleteRule(rule: ClassificationRule) {
        classificationRuleDao.delete(rule)
    }
    
    /**
     * Toggle rule enabled status
     */
    suspend fun toggleRule(ruleId: Long) {
        val rule = classificationRuleDao.getById(ruleId) ?: return
        classificationRuleDao.update(rule.copy(enabled = !rule.enabled))
    }
    
    /**
     * Reorder rules
     */
    suspend fun reorderRules(ruleIds: List<Long>) {
        ruleIds.forEachIndexed { index, id ->
            val rule = classificationRuleDao.getById(id) ?: return@forEachIndexed
            classificationRuleDao.update(rule.copy(priority = index))
        }
    }
}

/**
 * Classification result
 */
data class ClassificationResult(
    val success: Boolean,
    val receipt: Receipt? = null,
    val ruleApplied: ClassificationRule? = null,
    val archiveNumber: String? = null,
    val error: String? = null
)

/**
 * Batch classification result
 */
data class BatchClassificationResult(
    val total: Int,
    val classified: Int,
    val failed: Int,
    val results: List<ClassificationResult> = emptyList(),
    val errors: List<String> = emptyList()
)