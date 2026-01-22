package com.billmii.android.data.model

/**
 * OCR Template for custom receipt recognition
 */
data class OcrTemplate(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val fields: List<OcrTemplateField> = emptyList(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * OCR Template Field
 * Defines how to extract specific information from receipts
 */
data class OcrTemplateField(
    val id: Long = 0,
    val fieldName: String,
    val keyword: String,
    val extractionType: ExtractionType = ExtractionType.TEXT,
    val regexPattern: String? = null,
    val required: Boolean = false
)

/**
 * Extraction type for OCR template fields
 */
enum class ExtractionType {
    TEXT,       // Plain text extraction
    AMOUNT,     // Currency amount extraction
    DATE,       // Date extraction
    NUMBER      // Numeric value extraction
}