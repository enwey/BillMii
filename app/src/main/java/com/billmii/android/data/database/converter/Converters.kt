package com.billmii.android.data.database.converter

import androidx.room.TypeConverter
import com.billmii.android.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Room Type Converters for complex data types
 * Handles conversion between database types and Kotlin objects
 */
class Converters {
    
    private val gson = Gson()
    
    // Date converters
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
    
    // ReceiptType converters
    @TypeConverter
    fun fromReceiptType(type: ReceiptType): String {
        return type.name
    }
    
    @TypeConverter
    fun toReceiptType(value: String): ReceiptType {
        return try {
            ReceiptType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ReceiptType.UNKNOWN
        }
    }
    
    // ReceiptCategory converters
    @TypeConverter
    fun fromReceiptCategory(category: ReceiptCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toReceiptCategory(value: String): ReceiptCategory {
        return try {
            ReceiptCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ReceiptCategory.OTHER
        }
    }
    
    // ExpenseSubCategory converters
    @TypeConverter
    fun fromExpenseSubCategory(category: ExpenseSubCategory?): String? {
        return category?.name
    }
    
    @TypeConverter
    fun toExpenseSubCategory(value: String?): ExpenseSubCategory? {
        return value?.let {
            try {
                ExpenseSubCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ExpenseSubCategory.OTHER
            }
        }
    }
    
    // InvoiceStatus converters
    @TypeConverter
    fun fromInvoiceStatus(status: InvoiceStatus?): String? {
        return status?.name
    }
    
    @TypeConverter
    fun toInvoiceStatus(value: String?): InvoiceStatus? {
        return value?.let {
            try {
                InvoiceStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    // OcrStatus converters
    @TypeConverter
    fun fromOcrStatus(status: OcrStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toOcrStatus(value: String): OcrStatus {
        return try {
            OcrStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            OcrStatus.PENDING
        }
    }
    
    // ValidationStatus converters
    @TypeConverter
    fun fromValidationStatus(status: ValidationStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toValidationStatus(value: String): ValidationStatus {
        return try {
            ValidationStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ValidationStatus.PENDING
        }
    }
    
    // StorageLocation converters
    @TypeConverter
    fun fromStorageLocation(location: StorageLocation): String {
        return location.name
    }
    
    @TypeConverter
    fun toStorageLocation(value: String): StorageLocation {
        return try {
            StorageLocation.valueOf(value)
        } catch (e: IllegalArgumentException) {
            StorageLocation.INTERNAL
        }
    }
    
    // ReimbursementTemplate converters
    @TypeConverter
    fun fromReimbursementTemplate(template: ReimbursementTemplate): String {
        return template.name
    }
    
    @TypeConverter
    fun toReimbursementTemplate(value: String): ReimbursementTemplate {
        return try {
            ReimbursementTemplate.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ReimbursementTemplate.PERSONAL
        }
    }
    
    // ReimbursementValidationStatus converters
    @TypeConverter
    fun fromReimbursementValidationStatus(status: ReimbursementValidationStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toReimbursementValidationStatus(value: String): ReimbursementValidationStatus {
        return try {
            ReimbursementValidationStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ReimbursementValidationStatus.PENDING
        }
    }
    
    // ApprovalStatus converters
    @TypeConverter
    fun fromApprovalStatus(status: ApprovalStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toApprovalStatus(value: String): ApprovalStatus {
        return try {
            ApprovalStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ApprovalStatus.DRAFT
        }
    }
    
    // String list converters (for tags, etc.)
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        return json?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }
    
    // JSON string converters (for complex objects stored as JSON)
    @TypeConverter
    fun toJsonString(obj: Any?): String? {
        return obj?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun fromJsonString(json: String?): String? {
        return json // Return as is, parse when needed
    }
    
    // OperationType converters
    @TypeConverter
    fun fromOperationType(type: OperationType): String {
        return type.name
    }
    
    @TypeConverter
    fun toOperationType(value: String): OperationType {
        return try {
            OperationType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            OperationType.OTHER
        }
    }
    
    // OperationModule converters
    @TypeConverter
    fun fromOperationModule(module: OperationModule): String {
        return module.name
    }
    
    @TypeConverter
    fun toOperationModule(value: String): OperationModule {
        return try {
            OperationModule.valueOf(value)
        } catch (e: IllegalArgumentException) {
            OperationModule.OTHER
        }
    }
    
    // OperationResult converters
    @TypeConverter
    fun fromOperationResult(result: OperationResult): String {
        return result.name
    }
    
    @TypeConverter
    fun toOperationResult(value: String): OperationResult {
        return try {
            OperationResult.valueOf(value)
        } catch (e: IllegalArgumentException) {
            OperationResult.FAILED
        }
    }
}