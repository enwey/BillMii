package com.billmii.android.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.paddlepaddle.paddleocr.OcrKit
import com.paddlepaddle.paddleocr.OcrResult as PaddleResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PaddleOCR Manager
 * Manages PaddleOCR OCR engine integration
 */
@Singleton
class PaddleOCRManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var ocrKit: OcrKit? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "PaddleOCRManager"
    }
    
    /**
     * Initialize PaddleOCR
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        
        try {
            // Initialize PaddleOCR OcrKit
            ocrKit = OcrKit(context)
            
            // Configure OCR settings
            ocrKit?.apply {
                // Set detection model path (default is bundled with library)
                // You can also use custom models by copying them to assets
                setDetModelFileName("ch_PP-OCRv4_det_infer.onnx")
                setRecModelFileName("ch_PP-OCRv4_rec_infer.onnx")
                setClsModelFileName("ch_ppocr_mobile_v2.0_cls_infer.onnx")
                
                // Set CPU threads for inference
                setCpuThreadNum(4)
                setCpuPowerMode("LITE_POWER_HIGH")
                
                // Initialize models
                init()
            }
            
            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Detect and recognize text from bitmap
     */
    suspend fun detectAndRecognize(bitmap: Bitmap): OcrDetectionResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            initialize()
        }
        
        try {
            val result = ocrKit?.detect(bitmap)
            
            if (result != null) {
                OcrDetectionResult(
                    success = true,
                    text = result.text,
                    boxes = result.boxes.map { rect ->
                        Rect(rect.left, rect.top, rect.right, rect.bottom)
                    },
                    confidences = result.confidences,
                    confidence = result.confidences.maxOrNull() ?: 0f
                )
            } else {
                OcrDetectionResult(
                    success = false,
                    error = "OCR detection failed"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OcrDetectionResult(
                success = false,
                error = e.message ?: "Recognition failed"
            )
        }
    }
    
    /**
     * Recognize text only (no detection)
     */
    suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            initialize()
        }
        
        try {
            ocrKit?.recognize(bitmap) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            ocrKit?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ocrKit = null
            isInitialized = false
        }
    }
    
    /**
     * Check if OCR is initialized
     */
    fun isReady(): Boolean = isInitialized && ocrKit != null
}

/**
 * OCR Detection Result
 */
data class OcrDetectionResult(
    val success: Boolean,
    val text: String = "",
    val boxes: List<Rect> = emptyList(),
    val confidences: List<Float> = emptyList(),
    val confidence: Float = 0f,
    val error: String? = null
)