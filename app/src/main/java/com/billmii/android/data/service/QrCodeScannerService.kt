package com.billmii.android.data.service

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QR Code Scanner Service using ML Kit Barcode Scanning
 * Provides QR code and barcode scanning functionality
 */
@Singleton
class QrCodeScannerService @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {
    // Scanner instance
    private val scanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    // Scanning state
    private val _scanningState = MutableStateFlow<ScanningState>(ScanningState.Idle)
    val scanningState = _scanningState.asStateFlow()

    // Last detected code
    private val _detectedCode = MutableStateFlow<Barcode?>(null)
    val detectedCode = _detectedCode.asStateFlow()

    /**
     * Scan image proxy for QR/barcode
     * @param imageProxy CameraX image proxy
     * @return Flow emitting scan results
     */
    suspend fun scanImage(imageProxy: ImageProxy): ScanResult {
        return try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                _scanningState.value = ScanningState.Scanning
                
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes.firstOrNull()
                            if (barcode != null && barcode.rawValue != null) {
                                _detectedCode.value = barcode
                                _scanningState.value = ScanningState.Success(barcode)
                            } else {
                                _scanningState.value = ScanningState.NoCodeFound
                            }
                        } else {
                            _scanningState.value = ScanningState.NoCodeFound
                        }
                    }
                    .addOnFailureListener { e ->
                        _scanningState.value = ScanningState.Error(e.message ?: "Unknown error")
                    }
                    .await()
                
                ScanResult.Success(_detectedCode.value)
            } else {
                _scanningState.value = ScanningState.Error("No image data")
                ScanResult.Error("No image data")
            }
        } catch (e: Exception) {
            _scanningState.value = ScanningState.Error(e.message ?: "Scanning failed")
            ScanResult.Error(e.message ?: "Scanning failed")
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Scan bitmap for QR/barcode
     * @param bitmap Image bitmap to scan
     * @return Scan result
     */
    suspend fun scanBitmap(bitmap: Bitmap): ScanResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            _scanningState.value = ScanningState.Scanning
            
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.firstOrNull()
                        if (barcode != null && barcode.rawValue != null) {
                            _detectedCode.value = barcode
                            _scanningState.value = ScanningState.Success(barcode)
                        } else {
                            _scanningState.value = ScanningState.NoCodeFound
                        }
                    } else {
                        _scanningState.value = ScanningState.NoCodeFound
                    }
                }
                .addOnFailureListener { e ->
                    _scanningState.value = ScanningState.Error(e.message ?: "Unknown error")
                }
                .await()
            
            ScanResult.Success(_detectedCode.value)
        } catch (e: Exception) {
            _scanningState.value = ScanningState.Error(e.message ?: "Scanning failed")
            ScanResult.Error(e.message ?: "Scanning failed")
        }
    }

    /**
     * Reset scanning state
     */
    fun reset() {
        _scanningState.value = ScanningState.Idle
        _detectedCode.value = null
    }

    /**
     * Get bounding box of detected code
     */
    fun getBoundingBox(): Rect? {
        return _detectedCode.value?.boundingBox
    }

    /**
     * Get formatted value of detected code
     */
    fun getFormattedValue(): String? {
        return _detectedCode.value?.rawValue ?: _detectedCode.value?.displayValue
    }

    /**
     * Get code type
     */
    fun getCodeType(): String {
        return when (_detectedCode.value?.format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_PDF417 -> "PDF417"
            else -> "Unknown"
        }
    }

    /**
     * Parse QR code data for receipt information
     * Attempts to extract structured data from QR code
     */
    fun parseReceiptData(qrData: String): ReceiptQrData? {
        return try {
            // Try to parse common receipt QR code formats
            when {
                // VAT invoice format (China)
                qrData.startsWith("01,04,") -> parseVatInvoice(qrData)
                // WeChat/Alipay receipt format
                qrData.contains("微信") || qrData.contains("支付宝") -> parsePaymentReceipt(qrData)
                // JSON format
                qrData.startsWith("{") -> parseJsonReceipt(qrData)
                // URL format
                qrData.startsWith("http") -> parseUrlReceipt(qrData)
                else -> ReceiptQrData(
                    raw = qrData,
                    type = ReceiptQrDataType.UNKNOWN
                )
            }
        } catch (e: Exception) {
            ReceiptQrData(
                raw = qrData,
                type = ReceiptQrDataType.UNKNOWN
            )
        }
    }

    private fun parseVatInvoice(qrData: String): ReceiptQrData? {
        // Parse VAT invoice QR code format
        // Format: 01,04,invoiceCode,invoiceNumber,date,amount,taxAmount,checkCode
        val parts = qrData.split(",")
        if (parts.size >= 8) {
            return ReceiptQrData(
                raw = qrData,
                type = ReceiptQrDataType.VAT_INVOICE,
                invoiceCode = parts.getOrNull(2),
                invoiceNumber = parts.getOrNull(3),
                date = parts.getOrNull(4),
                amount = parts.getOrNull(5)?.toDoubleOrNull(),
                taxAmount = parts.getOrNull(6)?.toDoubleOrNull(),
                checkCode = parts.getOrNull(7)
            )
        }
        return null
    }

    private fun parsePaymentReceipt(qrData: String): ReceiptQrData? {
        // Parse WeChat/Alipay payment receipt
        val amountRegex = """金额[：:]\s*([0-9.]+)""".toRegex()
        val merchantRegex = """商户[：:]\s*([^\s]+)""".toRegex()
        val timeRegex = """时间[：:]\s*([^\s]+)""".toRegex()
        
        return ReceiptQrData(
            raw = qrData,
            type = ReceiptQrDataType.PAYMENT,
            merchant = merchantRegex.find(qrData)?.groupValues?.get(1),
            amount = amountRegex.find(qrData)?.groupValues?.get(1)?.toDoubleOrNull(),
            time = timeRegex.find(qrData)?.groupValues?.get(1)
        )
    }

    private fun parseJsonReceipt(qrData: String): ReceiptQrData? {
        // Try to parse JSON format
        // Simplified parsing - would need proper JSON parsing in production
        val amountRegex = """"amount"\s*:\s*([0-9.]+)""".toRegex()
        val merchantRegex = """"merchant"\s*:\s*"([^"]+)"""".toRegex()
        val invoiceRegex = """"invoice"\s*:\s*"([^"]+)"""".toRegex()
        
        return ReceiptQrData(
            raw = qrData,
            type = ReceiptQrDataType.JSON,
            merchant = merchantRegex.find(qrData)?.groupValues?.get(1),
            invoiceNumber = invoiceRegex.find(qrData)?.groupValues?.get(1),
            amount = amountRegex.find(qrData)?.groupValues?.get(1)?.toDoubleOrNull()
        )
    }

    private fun parseUrlReceipt(qrData: String): ReceiptQrData? {
        // Parse URL format (e.g., online receipt links)
        return ReceiptQrData(
            raw = qrData,
            type = ReceiptQrDataType.URL,
            url = qrData
        )
    }

    /**
     * Scanning state sealed class
     */
    sealed class ScanningState {
        data object Idle : ScanningState()
        data object Scanning : ScanningState()
        data object NoCodeFound : ScanningState()
        data class Success(val barcode: Barcode) : ScanningState()
        data class Error(val message: String) : ScanningState()
    }

    /**
     * Scan result sealed class
     */
    sealed class ScanResult {
        data class Success(val barcode: Barcode?) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    /**
     * Receipt QR code data
     */
    data class ReceiptQrData(
        val raw: String,
        val type: ReceiptQrDataType,
        val invoiceCode: String? = null,
        val invoiceNumber: String? = null,
        val date: String? = null,
        val amount: Double? = null,
        val taxAmount: Double? = null,
        val checkCode: String? = null,
        val merchant: String? = null,
        val time: String? = null,
        val url: String? = null
    )

    /**
     * Receipt QR data type
     */
    enum class ReceiptQrDataType {
        VAT_INVOICE,
        PAYMENT,
        JSON,
        URL,
        UNKNOWN
    }
}