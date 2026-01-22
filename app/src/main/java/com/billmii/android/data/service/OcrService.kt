package com.billmii.android.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.billmii.android.data.model.*
import com.billmii.android.ocr.PaddleOCRManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR Service
 * Handles text recognition from receipt images using PaddleOCR
 */
@Singleton
class OcrService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paddleOCRManager: PaddleOCRManager
) {
    
    private var isInitialized = false
    
    // OCR settings
    var enablePreprocessing = true
    var enableCorrection = true
    var confidenceThreshold = 0.7f
    
    companion object {
        private const val TAG = "OcrService"
        private const val MODEL_DIR = "models"
    }
    
    /**
     * Initialize PaddleOCR
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        
        val result = paddleOCRManager.initialize()
        isInitialized = result
        result
    }
    
    /**
     * Recognize text from image file
     */
    suspend fun recognizeText(imageFile: File): OcrResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            initialize()
        }
        
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val ocrResult = paddleOCRManager.detectAndRecognize(bitmap)
             
            OcrResult(
                success = ocrResult.success,
                text = ocrResult.text,
                boxes = ocrResult.boxes,
                confidence = ocrResult.confidence,
                error = ocrResult.error
            )
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult(
                success = false,
                error = e.message ?: "Recognition failed"
            )
        }
    }
    
    /**
     * Recognize receipt and extract structured data
     */
    suspend fun recognizeReceipt(imageFile: File): ReceiptOcrResult = withContext(Dispatchers.IO) {
        val textResult = recognizeText(imageFile)
        
        if (!textResult.success) {
            return@withContext ReceiptOcrResult(
                success = false,
                error = textResult.error
            )
        }
        
        try {
            // Detect receipt type
            val receiptType = detectReceiptType(textResult.text)
            
            // Extract receipt data based on type
            val receiptData = when (receiptType) {
                ReceiptType.VAT_INVOICE -> extractVatInvoiceData(textResult)
                ReceiptType.TRAIN_TICKET -> extractTrainTicketData(textResult)
                ReceiptType.FLIGHT_ITINERARY -> extractFlightItineraryData(textResult)
                ReceiptType.TAXI_RECEIPT -> extractTaxiReceiptData(textResult)
                ReceiptType.HOTEL_RECEIPT -> extractHotelReceiptData(textResult)
                ReceiptType.RESTAURANT_RECEIPT -> extractRestaurantReceiptData(textResult)
                ReceiptType.GENERAL_RECEIPT -> extractGeneralReceiptData(textResult)
                else -> extractGeneralReceiptData(textResult)
            }
            
            ReceiptOcrResult(
                success = true,
                receiptType = receiptType,
                data = receiptData,
                confidence = textResult.confidence
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ReceiptOcrResult(
                success = false,
                error = "Failed to extract receipt data: ${e.message}"
            )
        }
    }
    
    /**
     * Detect receipt type from text
     */
    private fun detectReceiptType(text: String): ReceiptType {
        val lowerText = text.lowercase()
        
        return when {
            // VAT Invoice detection
            lowerText.contains("增值税") || lowerText.contains("专用发票") || 
            lowerText.contains("普通发票") || lowerText.contains("电子发票") -> {
                ReceiptType.VAT_INVOICE
            }
            // Train ticket detection
            lowerText.contains("火车票") || lowerText.contains("车次") || 
            lowerText.contains("旅客") || lowerText.contains("站到站") -> {
                ReceiptType.TRAIN_TICKET
            }
            // Flight itinerary detection
            lowerText.contains("航空") || lowerText.contains("电子客票") || 
            lowerText.contains("航班") || lowerText.contains("承运人") -> {
                ReceiptType.FLIGHT_ITINERARY
            }
            // Taxi receipt detection
            lowerText.contains("出租车") || lowerText.contains("计价器") || 
            lowerText.contains("公里") || lowerText.contains("车费") -> {
                ReceiptType.TAXI_RECEIPT
            }
            // Hotel receipt detection
            lowerText.contains("酒店") || lowerText.contains("宾馆") || 
            lowerText.contains("住宿") || lowerText.contains("客房") -> {
                ReceiptType.HOTEL_RECEIPT
            }
            // Restaurant receipt detection
            lowerText.contains("餐饮") || lowerText.contains("食品") || 
            lowerText.contains("菜") || lowerText.contains("酒") -> {
                ReceiptType.RESTAURANT_RECEIPT
            }
            else -> ReceiptType.GENERAL_RECEIPT
        }
    }
    
    /**
     * Extract VAT invoice data
     */
    private fun extractVatInvoiceData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract invoice number
        data.invoiceNumber = extractPattern(text, "发票号码[:：]?\\s*(\\d+)")
        data.invoiceCode = extractPattern(text, "发票代码[:：]?\\s*(\\d+)")
        
        // Extract dates
        data.invoiceDate = extractDate(text, "开票日期")
        data.purchaseDate = data.invoiceDate
        
        // Extract seller info
        data.sellerName = extractAfterKeyword(text, "销售方", "名称")
        data.sellerTaxId = extractAfterKeyword(text, "销售方", "纳税人识别号")
        data.sellerAddress = extractAfterKeyword(text, "销售方", "地址")
        data.sellerPhone = extractAfterKeyword(text, "销售方", "电话")
        data.sellerBank = extractAfterKeyword(text, "销售方", "开户行")
        data.sellerAccount = extractAfterKeyword(text, "销售方", "账号")
        
        // Extract buyer info
        data.buyerName = extractAfterKeyword(text, "购买方", "名称")
        data.buyerTaxId = extractAfterKeyword(text, "购买方", "纳税人识别号")
        data.buyerAddress = extractAfterKeyword(text, "购买方", "地址")
        data.buyerPhone = extractAfterKeyword(text, "购买方", "电话")
        data.buyerBank = extractAfterKeyword(text, "购买方", "开户行")
        data.buyerAccount = extractAfterKeyword(text, "购买方", "账号")
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "价税合计")
        data.amount = extractAmount(text, "合计金额")
        data.taxAmount = extractAmount(text, "合计税额")
        
        // Extract tax rate
        data.taxRate = extractTaxRate(text)
        
        return data
    }
    
    /**
     * Extract train ticket data
     */
    private fun extractTrainTicketData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract train info
        data.trainNumber = extractPattern(text, "车次[:：]?\\s*([A-Z0-9]+)")
        data.departureStation = extractPattern(text, "出发站[:：]?\\s*(.+?)(?=到达站|\\n)")
        data.arrivalStation = extractPattern(text, "到达站[:：]?\\s*(.+?)(?=时间|\\n)")
        
        // Extract dates
        data.travelDate = extractDate(text, "乘车日期")
        data.departureTime = extractPattern(text, "发车时间[:：]?\\s*(\\d{2}:\\d{2})")
        data.arrivalTime = extractPattern(text, "到达时间[:：]?\\s*(\\d{2}:\\d{2})")
        
        // Extract passenger info
        data.passengerName = extractPattern(text, "旅客[:：]?\\s*(.+?)(?=证件|\\n)")
        data.idNumber = extractPattern(text, "证件号码[:：]?\\s*([A-Za-z0-9]+)")
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "票价")
        data.amount = data.totalAmount
        
        // Extract seat info
        data.seatInfo = extractPattern(text, "席别[:：]?\\s*(.+?)(?=车厢|\\n)")
        data.carriageNumber = extractPattern(text, "车厢[:：]?\\s*(\\d+)")
        data.seatNumber = extractPattern(text, "座位号[:：]?\\s*(\\d+[A-Z]?)")
        
        return data
    }
    
    /**
     * Extract flight itinerary data
     */
    private fun extractFlightItineraryData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract flight info
        data.flightNumber = extractPattern(text, "航班号[:：]?\\s*([A-Z0-9]+)")
        data.flightDate = extractDate(text, "出发日期")
        
        // Extract route
        data.departureAirport = extractPattern(text, "出发机场[:：]?\\s*(.+?)(?=到达机场|\\n)")
        data.arrivalAirport = extractPattern(text, "到达机场[:：]?\\s*(.+?)(?=$|\\n)")
        
        // Extract times
        data.departureTime = extractPattern(text, "起飞时间[:：]?\\s*(\\d{2}:\\d{2})")
        data.arrivalTime = extractPattern(text, "到达时间[:：]?\\s*(\\d{2}:\\d{2})")
        
        // Extract passenger info
        data.passengerName = extractPattern(text, "旅客姓名[:：]?\\s*(.+?)(?=证件|\\n)")
        data.idNumber = extractPattern(text, "证件号码[:：]?\\s*([A-Za-z0-9]+)")
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "票价")
        data.amount = data.totalAmount
        
        // Extract carrier
        data.carrier = extractPattern(text, "承运人[:：]?\\s*(.+?)(?=航班|$)")
        
        return data
    }
    
    /**
     * Extract taxi receipt data
     */
    private fun extractTaxiReceiptData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract taxi info
        data.taxiCompany = extractPattern(text, "公司[:：]?\\s*(.+?)(?=车牌|\\n)")
        data.taxiLicense = extractPattern(text, "车牌号[:：]?\\s*([A-Z0-9]+)")
        data.driverName = extractPattern(text, "司机[:：]?\\s*(.+?)(?=电话|\\n)")
        data.driverPhone = extractPattern(text, "电话[:：]?\\s*(\\d+)")
        
        // Extract trip info
        data.pickupLocation = extractPattern(text, "上车[:：]?\\s*(.+?)(?=下车|\\n)")
        data.dropoffLocation = extractPattern(text, "下车[:：]?\\s*(.+?)(?=时间|\\n)")
        data.travelDate = extractDate(text, "日期")
        data.travelTime = extractPattern(text, "时间[:：]?\\s*(\\d{2}:\\d{2})")
        
        // Extract trip details
        data.distance = extractDistance(text)
        data.duration = extractDuration(text)
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "金额")
        data.amount = data.totalAmount
        data.fareAmount = extractAmount(text, "车费")
        data.surcharge = extractAmount(text, "附加费")
        
        return data
    }
    
    /**
     * Extract hotel receipt data
     */
    private fun extractHotelReceiptData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract hotel info
        data.hotelName = extractPattern(text, "酒店[:：]?\\s*(.+?)(?=地址|\\n)")
        data.hotelAddress = extractPattern(text, "地址[:：]?\\s*(.+?)(?=电话|\\n)")
        data.hotelPhone = extractPattern(text, "电话[:：]?\\s*(\\d+)")
        
        // Extract stay info
        data.checkInDate = extractDate(text, "入住日期")
        data.checkOutDate = extractDate(text, "离店日期")
        data.guestName = extractPattern(text, "客人[:：]?\\s*(.+?)(?=房间|\\n)")
        data.roomNumber = extractPattern(text, "房间号[:：]?\\s*(\\d+)")
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "合计")
        data.amount = data.totalAmount
        data.roomRate = extractAmount(text, "房费")
        
        return data
    }
    
    /**
     * Extract restaurant receipt data
     */
    private fun extractRestaurantReceiptData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract restaurant info
        data.restaurantName = extractPattern(text, "餐厅[:：]?\\s*(.+?)(?=地址|\\n)")
        data.restaurantAddress = extractPattern(text, "地址[:：]?\\s*(.+?)(?=电话|\\n)")
        data.restaurantPhone = extractPattern(text, "电话[:：]?\\s*(\\d+)")
        
        // Extract date
        data.invoiceDate = extractDate(text, "日期")
        data.purchaseDate = data.invoiceDate
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "合计")
        data.amount = data.totalAmount
        
        return data
    }
    
    /**
     * Extract general receipt data
     */
    private fun extractGeneralReceiptData(ocrResult: OcrResult): ReceiptOcrData {
        val text = ocrResult.text
        val data = ReceiptOcrData()
        
        // Extract basic info
        data.invoiceDate = extractDate(text, "日期|时间")
        data.purchaseDate = data.invoiceDate
        
        // Extract amounts
        data.totalAmount = extractAmount(text, "合计|总计|金额")
        data.amount = data.totalAmount
        
        return data
    }
    
    /**
     * Extract pattern from text
     */
    private fun extractPattern(text: String, pattern: String): String {
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(text)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }
    
    /**
     * Extract date from text
     */
    private fun extractDate(text: String, keyword: String): Date? {
        val pattern = "$keyword[:：]?\\s*(\\d{4})[-年](\\d{1,2})[-月](\\d{1,2})"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(text)
        
        return if (matcher.find()) {
            try {
                val year = matcher.group(1).toInt()
                val month = matcher.group(2).toInt()
                val day = matcher.group(3).toInt()
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, day)
                calendar.time
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * Extract amount from text
     */
    private fun extractAmount(text: String, keyword: String): Double? {
        val pattern = "$keyword[:：]?\\s*[¥￥]?(\\d+(?:\\.\\d{1,2})?)"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(text)
        
        return if (matcher.find()) {
            try {
                matcher.group(1).toDouble()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * Extract tax rate from text
     */
    private fun extractTaxRate(text: String): Double? {
        val pattern = "税率[:：]?\\s*(\\d+(?:\\.\\d{1,2})?)%"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(text)
        
        return if (matcher.find()) {
            try {
                matcher.group(1).toDouble()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * Extract distance from text
     */
    private fun extractDistance(text: String): Double? {
        val pattern = "里程[:：]?\\s*(\\d+(?:\\.\\d{1,2})?)"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(text)
        
        return if (matcher.find()) {
            try {
                matcher.group(1).toDouble()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * Extract duration from text
     */
    private fun extractDuration(text: String): String? {
        val pattern = "时间[:：]?\\s*(\\d{1,2}[:：]\\d{2})"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(text)
        
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
    
    /**
     * Extract value after keyword
     */
    private fun extractAfterKeyword(text: String, section: String, field: String): String {
        val sectionPattern = "$section[:：]?(.+?)(?=$|\\n\\n)"
        val sectionMatcher = Pattern.compile(sectionPattern, Pattern.DOTALL).matcher(text)
        
        if (sectionMatcher.find()) {
            val sectionText = sectionMatcher.group(1) ?: ""
            val fieldPattern = "$field[:：]?\\s*(.+?)(?=\\n|$)"
            val fieldMatcher = Pattern.compile(fieldPattern).matcher(sectionText)
            
            if (fieldMatcher.find()) {
                return fieldMatcher.group(1)?.trim() ?: ""
            }
        }
        
        return ""
    }
    
    /**
     * Release resources
     */
    fun release() {
        paddleOCRManager.release()
        isInitialized = false
    }
}

/**
 * OCR Result
 */
data class OcrResult(
    val success: Boolean,
    val text: String = "",
    val boxes: List<Rect> = emptyList(),
    val confidence: Float = 0f,
    val error: String? = null
)

/**
 * Receipt OCR Result
 */
data class ReceiptOcrResult(
    val success: Boolean,
    val receiptType: ReceiptType = ReceiptType.GENERAL_RECEIPT,
    val data: ReceiptOcrData = ReceiptOcrData(),
    val confidence: Float = 0f,
    val error: String? = null
)

/**
 * Receipt OCR Data
 */
data class ReceiptOcrData(
    // Basic info
    var invoiceNumber: String = "",
    var invoiceCode: String = "",
    var invoiceDate: Date? = null,
    var purchaseDate: Date? = null,
    
    // Seller info
    var sellerName: String = "",
    var sellerTaxId: String = "",
    var sellerAddress: String = "",
    var sellerPhone: String = "",
    var sellerBank: String = "",
    var sellerAccount: String = "",
    
    // Buyer info
    var buyerName: String = "",
    var buyerTaxId: String = "",
    var buyerAddress: String = "",
    var buyerPhone: String = "",
    var buyerBank: String = "",
    var buyerAccount: String = "",
    
    // Amounts
    var totalAmount: Double? = null,
    var amount: Double? = null,
    var taxAmount: Double? = null,
    var taxRate: Double? = null,
    
    // Train ticket specific
    var trainNumber: String = "",
    var departureStation: String = "",
    var arrivalStation: String = "",
    var travelDate: Date? = null,
    var departureTime: String = "",
    var arrivalTime: String = "",
    var passengerName: String = "",
    var idNumber: String = "",
    var seatInfo: String = "",
    var carriageNumber: String = "",
    var seatNumber: String = "",
    
    // Flight itinerary specific
    var flightNumber: String = "",
    var flightDate: Date? = null,
    var departureAirport: String = "",
    var arrivalAirport: String = "",
    var carrier: String = "",
    
    // Taxi receipt specific
    var taxiCompany: String = "",
    var taxiLicense: String = "",
    var driverName: String = "",
    var driverPhone: String = "",
    var pickupLocation: String = "",
    var dropoffLocation: String = "",
    var travelTime: String = "",
    var distance: Double? = null,
    var duration: String? = null,
    var fareAmount: Double? = null,
    var surcharge: Double? = null,
    
    // Hotel receipt specific
    var hotelName: String = "",
    var hotelAddress: String = "",
    var hotelPhone: String = "",
    var checkInDate: Date? = null,
    var checkOutDate: Date? = null,
    var guestName: String = "",
    var roomNumber: String = "",
    var roomRate: Double? = null,
    
    // Restaurant receipt specific
    var restaurantName: String = "",
    var restaurantAddress: String = "",
    var restaurantPhone: String = ""
)
