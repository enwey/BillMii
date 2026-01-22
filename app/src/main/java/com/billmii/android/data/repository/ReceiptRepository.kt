package com.billmii.android.data.repository

import android.content.Context
import android.net.Uri
import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.model.*
import com.billmii.android.data.service.FileStorageService
import com.billmii.android.data.service.OcrService
import com.billmii.android.data.service.ReceiptOcrResult
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Receipt Repository - 票据仓库
 * Manages receipt data operations and business logic
 */
@Singleton
class ReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val fileStorageService: FileStorageService,
    private val ocrService: OcrService,
    private val context: Context
) {
    
    /**
     * Get all receipts
     */
    fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts()
    }
    
    /**
     * Get receipt by ID
     */
    suspend fun getReceiptById(id: Long): Receipt? {
        return receiptDao.getById(id)
    }
    
    /**
     * Get receipts by type
     */
    fun getReceiptsByType(type: ReceiptType): Flow<List<Receipt>> {
        return receiptDao.getByReceiptType(type)
    }
    
    /**
     * Get receipts by category
     */
    fun getReceiptsByCategory(category: ReceiptCategory): Flow<List<Receipt>> {
        return receiptDao.getByCategory(category)
    }
    
    /**
     * Get receipts by OCR status
     */
    fun getReceiptsByOcrStatus(status: OcrStatus): Flow<List<Receipt>> {
        return receiptDao.getByOcrStatus(status)
    }
    
    /**
     * Get receipts by validation status
     */
    fun getReceiptsByValidationStatus(status: ValidationStatus): Flow<List<Receipt>> {
        return receiptDao.getByValidationStatus(status)
    }
    
    /**
     * Get unprocessed receipts
     */
    fun getUnprocessedReceipts(): Flow<List<Receipt>> {
        return receiptDao.getByProcessedStatus(false)
    }
    
    /**
     * Search receipts
     */
    fun searchReceipts(query: String): Flow<List<Receipt>> {
        return receiptDao.search(query)
    }
    
    /**
     * Import receipt from file
     */
    suspend fun importReceipt(
        uri: Uri,
        fileName: String? = null,
        createdBy: String? = null
    ): Result<Receipt> {
        return try {
            // Get file info
            val file = fileStorageService.copyFromUri(uri)
            val fileSize = file.length()
            val fileType = getFileExtension(file.name)
            val fileHash = fileStorageService.calculateFileHash(file)
            
            // Check for duplicates
            val existing = receiptDao.findByFileHash(fileHash)
            if (existing != null) {
                return Result.failure(Exception("Duplicate file detected"))
            }
            
            // Create receipt
            val receipt = Receipt(
                fileName = fileName ?: file.name,
                filePath = file.absolutePath,
                fileHash = fileHash,
                fileSize = fileSize,
                fileType = fileType,
                receiptType = ReceiptType.UNKNOWN,
                receiptCategory = ReceiptCategory.OTHER,
                ocrStatus = OcrStatus.PENDING,
                createdBy = createdBy,
                createdAt = Date()
            )
            
            val id = receiptDao.insert(receipt)
            Result.success(receipt.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import receipt from file (File object)
     */
    suspend fun importReceipt(file: File): Result<Receipt> {
        return try {
            val fileSize = file.length()
            val fileType = getFileExtension(file.name)
            val fileHash = fileStorageService.calculateFileHash(file)
            
            // Check for duplicates
            val existing = receiptDao.findByFileHash(fileHash)
            if (existing != null) {
                return Result.failure(Exception("Duplicate file detected"))
            }
            
            val receipt = Receipt(
                fileName = file.name,
                filePath = file.absolutePath,
                fileHash = fileHash,
                fileSize = fileSize,
                fileType = fileType,
                receiptType = ReceiptType.UNKNOWN,
                receiptCategory = ReceiptCategory.OTHER,
                ocrStatus = OcrStatus.PENDING,
                createdAt = Date()
            )
            
            val id = receiptDao.insert(receipt)
            Result.success(receipt.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import multiple receipts
     */
    suspend fun importReceipts(
        uris: List<Uri>,
        createdBy: String? = null
    ): Result<List<Receipt>> {
        return try {
            val receipts = mutableListOf<Receipt>()
            val errors = mutableListOf<String>()
            
            for (uri in uris) {
                val result = importReceipt(uri, createdBy = createdBy)
                if (result.isSuccess) {
                    receipts.add(result.getOrNull()!!)
                } else {
                    errors.add("Failed to import ${uri.lastPathSegment}: ${result.exceptionOrNull()?.message}")
                }
            }
            
            if (errors.isNotEmpty()) {
                Result.failure(Exception(errors.joinToString("\n")))
            } else {
                Result.success(receipts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create receipt from camera capture
     */
    suspend fun createReceiptFromCamera(
        imageFile: File,
        fileName: String? = null,
        createdBy: String? = null
    ): Result<Receipt> {
        return try {
            val fileSize = imageFile.length()
            val fileType = getFileExtension(imageFile.name)
            val fileHash = fileStorageService.calculateFileHash(imageFile)
            
            // Check for duplicates
            val existing = receiptDao.findByFileHash(fileHash)
            if (existing != null) {
                return Result.failure(Exception("Duplicate file detected"))
            }
            
            val receipt = Receipt(
                fileName = fileName ?: imageFile.name,
                filePath = imageFile.absolutePath,
                fileHash = fileHash,
                fileSize = fileSize,
                fileType = fileType,
                receiptType = ReceiptType.UNKNOWN,
                receiptCategory = ReceiptCategory.OTHER,
                ocrStatus = OcrStatus.PENDING,
                createdBy = createdBy,
                createdAt = Date()
            )
            
            val id = receiptDao.insert(receipt)
            Result.success(receipt.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update receipt
     */
    suspend fun updateReceipt(receipt: Receipt): Result<Receipt> {
        return try {
            receiptDao.update(receipt)
            Result.success(receipt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete receipt
     */
    suspend fun deleteReceipt(receipt: Receipt): Result<Unit> {
        return try {
            // Delete file from storage
            val file = File(receipt.filePath)
            if (file.exists()) {
                file.delete()
            }
            
            // Delete from database
            receiptDao.delete(receipt)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete multiple receipts
     */
    suspend fun deleteReceipts(receiptIds: List<Long>): Result<Unit> {
        return try {
            // Get receipts
            val receipts = receiptIds.mapNotNull { receiptDao.getById(it) }
            
            // Delete files
            receipts.forEach { receipt ->
                val file = File(receipt.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            
            // Delete from database
            receiptDao.deleteByIds(receiptIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update OCR status
     */
    suspend fun updateOcrStatus(
        receiptId: Long,
        status: OcrStatus,
        recognizedData: Receipt? = null
    ): Result<Receipt> {
        return try {
            val receipt = receiptDao.getById(receiptId)
                ?: return Result.failure(Exception("Receipt not found"))
            
            val updatedReceipt = if (recognizedData != null) {
                receipt.copy(
                    ocrStatus = status,
                    recognizedAt = Date(),
                    updatedAt = Date(),
                    // Update OCR fields
                    receiptType = recognizedData.receiptType,
                    receiptCategory = recognizedData.receiptCategory,
                    expenseSubCategory = recognizedData.expenseSubCategory,
                    invoiceCode = recognizedData.invoiceCode,
                    invoiceNumber = recognizedData.invoiceNumber,
                    invoiceDate = recognizedData.invoiceDate,
                    buyerName = recognizedData.buyerName,
                    buyerTaxId = recognizedData.buyerTaxId,
                    sellerName = recognizedData.sellerName,
                    sellerTaxId = recognizedData.sellerTaxId,
                    totalAmount = recognizedData.totalAmount,
                    amountWithoutTax = recognizedData.amountWithoutTax,
                    taxRate = recognizedData.taxRate,
                    taxAmount = recognizedData.taxAmount,
                    invoiceStatus = recognizedData.invoiceStatus,
                    expenseDate = recognizedData.expenseDate,
                    departurePlace = recognizedData.departurePlace,
                    destination = recognizedData.destination,
                    expenseType = recognizedData.expenseType,
                    issuer = recognizedData.issuer,
                    amount = recognizedData.amount
                )
            } else {
                receipt.copy(
                    ocrStatus = status,
                    recognizedAt = Date(),
                    updatedAt = Date()
                )
            }
            
            receiptDao.update(updatedReceipt)
            Result.success(updatedReceipt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform OCR on a receipt
     */
    suspend fun performOcr(receiptId: Long): Result<Receipt> {
        return try {
            val receipt = receiptDao.getById(receiptId)
                ?: return Result.failure(Exception("Receipt not found"))
            
            // Update status to processing
            updateOcrStatus(receiptId, OcrStatus.PROCESSING)
            
            // Perform OCR recognition
            val imageFile = File(receipt.filePath)
            val ocrResult = ocrService.recognizeReceipt(imageFile)
            
            if (!ocrResult.success) {
                updateOcrStatus(receiptId, OcrStatus.FAILED)
                return Result.failure(Exception(ocrResult.error ?: "OCR failed"))
            }
            
            // Extract OCR data and update receipt
            val updatedReceipt = extractOcrDataToReceipt(receipt, ocrResult)
            
            // Update status based on confidence
            val ocrStatus = if (ocrResult.confidence >= ocrService.confidenceThreshold) {
                OcrStatus.SUCCESS
            } else {
                OcrStatus.PARTIAL
            }
            
            updateOcrStatus(
                receiptId,
                ocrStatus,
                updatedReceipt
            )
            
            // Get updated receipt
            val finalReceipt = receiptDao.getById(receiptId)!!
            Result.success(finalReceipt)
        } catch (e: Exception) {
            updateOcrStatus(receiptId, OcrStatus.FAILED)
            Result.failure(e)
        }
    }
    
    /**
     * Perform batch OCR on multiple receipts
     */
    suspend fun performBatchOcr(receiptIds: List<Long>): Result<Map<Long, Receipt>> {
        return try {
            val results = mutableMapOf<Long, Receipt>()
            val errors = mutableListOf<String>()
            
            for (receiptId in receiptIds) {
                val result = performOcr(receiptId)
                if (result.isSuccess) {
                    results[receiptId] = result.getOrNull()!!
                } else {
                    errors.add("Receipt $receiptId: ${result.exceptionOrNull()?.message}")
                }
            }
            
            if (errors.isEmpty()) {
                Result.success(results)
            } else {
                Result.failure(Exception(errors.joinToString("\n")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract OCR data to receipt
     */
    private fun extractOcrDataToReceipt(receipt: Receipt, ocrResult: ReceiptOcrResult): Receipt {
        val data = ocrResult.data
        
        return receipt.copy(
            receiptType = ocrResult.receiptType,
            receiptCategory = determineCategory(ocrResult.receiptType),
            
            // Invoice fields
            invoiceCode = data.invoiceCode,
            invoiceNumber = data.invoiceNumber,
            invoiceDate = data.invoiceDate,
            buyerName = data.buyerName,
            buyerTaxId = data.buyerTaxId,
            sellerName = data.sellerName,
            sellerTaxId = data.sellerTaxId,
            
            // Amount fields
            totalAmount = data.totalAmount,
            amountWithoutTax = data.amount,
            taxRate = data.taxRate,
            taxAmount = data.taxAmount,
            
            // Travel fields
            departurePlace = data.departureStation,
            destination = data.arrivalStation,
            expenseDate = data.travelDate,
            
            // Expense fields
            expenseType = determineExpenseType(ocrResult.receiptType),
            issuer = data.sellerName ?: data.hotelName ?: data.restaurantName ?: data.taxiCompany,
            
            // Amount fallback
            amount = data.totalAmount ?: data.amount
        )
    }
    
    /**
     * Determine receipt category from type
     */
    private fun determineCategory(type: ReceiptType): ReceiptCategory {
        return when (type) {
            ReceiptType.VAT_INVOICE -> ReceiptCategory.INCOME
            ReceiptType.TRAIN_TICKET,
            ReceiptType.FLIGHT_ITINERARY,
            ReceiptType.TAXI_RECEIPT -> ReceiptCategory.TRANSPORTATION
            ReceiptType.HOTEL_RECEIPT -> ReceiptCategory.ACCOMMODATION
            ReceiptType.RESTAURANT_RECEIPT -> ReceiptCategory.FOOD
            else -> ReceiptCategory.OTHER
        }
    }
    
    /**
     * Determine expense type from receipt type
     */
    private fun determineExpenseType(type: ReceiptType): ExpenseSubCategory {
        return when (type) {
            ReceiptType.TRAIN_TICKET -> ExpenseSubCategory.TRAIN_TICKET
            ReceiptType.FLIGHT_ITINERARY -> ExpenseSubCategory.AIR_TICKET
            ReceiptType.TAXI_RECEIPT -> ExpenseSubCategory.TAXI
            ReceiptType.HOTEL_RECEIPT -> ExpenseSubCategory.HOTEL
            ReceiptType.RESTAURANT_RECEIPT -> ExpenseSubCategory.MEAL
            ReceiptType.VAT_INVOICE -> ExpenseSubCategory.OFFICE_SUPPLIES
            else -> ExpenseSubCategory.OTHER
        }
    }
    
    /**
     * Update validation status
     */
    suspend fun updateValidationStatus(
        receiptId: Long,
        status: ValidationStatus,
        errors: List<String>? = null
    ): Result<Receipt> {
        return try {
            val receipt = receiptDao.getById(receiptId)
                ?: return Result.failure(Exception("Receipt not found"))
            
            val updatedReceipt = receipt.copy(
                validationStatus = status,
                validationErrors = errors?.let { 
                    com.google.gson.Gson().toJson(it) 
                },
                updatedAt = Date()
            )
            
            receiptDao.update(updatedReceipt)
            Result.success(updatedReceipt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Link receipt to reimbursement
     */
    suspend fun linkToReimbursement(
        receiptId: Long,
        reimbursementId: Long
    ): Result<Unit> {
        return try {
            receiptDao.linkToReimbursement(listOf(receiptId), reimbursementId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unlink receipt from reimbursement
     */
    suspend fun unlinkFromReimbursement(receiptId: Long): Result<Unit> {
        return try {
            receiptDao.unlinkFromReimbursement(listOf(receiptId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get receipt statistics
     */
    suspend fun getReceiptStatistics(): ReceiptStatistics {
        val totalCount = receiptDao.countAll()
        val unprocessedCount = receiptDao.countUnprocessed()
        val duplicateCount = receiptDao.countDuplicates()
        val totalAmount = receiptDao.getTotalAmount() ?: 0.0
        
        return ReceiptStatistics(
            totalCount = totalCount,
            unprocessedCount = unprocessedCount,
            duplicateCount = duplicateCount,
            totalAmount = totalAmount
        )
    }
    
    /**
     * Get file extension
     */
    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").uppercase()
    }
}

/**
 * Receipt statistics data class
 */
data class ReceiptStatistics(
    val totalCount: Int,
    val unprocessedCount: Int,
    val duplicateCount: Int,
    val totalAmount: Double
)