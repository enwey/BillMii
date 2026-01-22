package com.billmii.android.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Camera Manager
 * Manages camera operations for receipt capture with image preprocessing
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // Image processing settings
    var enableAutoEnhancement = true
    var enableEdgeDetection = true
    var outputFormat = ImageFormat.JPEG
    var outputQuality = 100 // 0-100
    
    companion object {
        private const val TAG = "CameraManager"
    }
    
    /**
     * Initialize camera
     */
    suspend fun initializeCamera(): Boolean {
        return try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Start camera preview
     */
    fun startCamera(previewView: PreviewView, onPreviewReady: (() -> Unit)? = null) {
        val provider = cameraProvider ?: run {
            throw IllegalStateException("Camera not initialized")
        }
        
        // Preview use case
        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image capture use case
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        
        // Camera selector
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Unbind use cases before rebinding
            provider.unbindAll()
            
            // Bind use cases to camera
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            onPreviewReady?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Capture photo with preprocessing
     */
    suspend fun capturePhoto(): Result<Bitmap> {
        return suspendCancellableCoroutine { continuation ->
            val imageCapture = imageCapture ?: run {
                continuation.resumeWithException(IllegalStateException("ImageCapture not initialized"))
                return@suspendCancellableCoroutine
            }
            
            val outputFile = createTempImageFile()
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            // Load bitmap
                            val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                            
                            // Preprocess image
                            val processedBitmap = if (enableAutoEnhancement) {
                                preprocessImage(bitmap)
                            } else {
                                bitmap
                            }
                            
                            // Clean up temp file
                            outputFile.delete()
                            
                            continuation.resume(Result.success(processedBitmap))
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                    
                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
    
    /**
     * Capture photo with callback (for UI use)
     */
    fun capturePhoto(outputFile: File, onComplete: (Boolean) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onComplete(false)
            return
        }
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (enableAutoEnhancement) {
                        try {
                            // Load and preprocess
                            val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                            val processedBitmap = preprocessImage(bitmap)
                            
                            // Save processed image
                            java.io.FileOutputStream(outputFile).use { out ->
                                processedBitmap.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    outputQuality,
                                    out
                                )
                            }
                            
                            onComplete(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onComplete(false)
                        }
                    } else {
                        onComplete(true)
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    onComplete(false)
                }
            }
        )
    }
    
    /**
     * Capture photo and save to file (suspend version)
     */
    suspend fun capturePhotoToFile(outputFile: File): Result<File> {
        return suspendCancellableCoroutine { continuation ->
            val imageCapture = imageCapture ?: run {
                continuation.resumeWithException(IllegalStateException("ImageCapture not initialized"))
                return@suspendCancellableCoroutine
            }
            
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (enableAutoEnhancement) {
                            try {
                                // Load and preprocess
                                val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                                val processedBitmap = preprocessImage(bitmap)
                                
                                // Save processed image
                                FileOutputStream(outputFile).use { out ->
                                    processedBitmap.compress(
                                        Bitmap.CompressFormat.JPEG,
                                        outputQuality,
                                        out
                                    )
                                }
                                
                                continuation.resume(Result.success(outputFile))
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        } else {
                            continuation.resume(Result.success(outputFile))
                        }
                    }
                    
                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
    
    /**
     * Preprocess image for better OCR results
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Convert to OpenCV Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
        
        // Apply Gaussian blur to reduce noise
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(3.0, 3.0), 0.0)
        
        // Apply adaptive thresholding for better text detection
        val thresholdMat = Mat()
        Imgproc.adaptiveThreshold(
            blurredMat,
            thresholdMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11.0,
            2.0
        )
        
        // Optional: Edge detection for document boundaries
        val finalMat = if (enableEdgeDetection) {
            detectEdges(thresholdMat)
        } else {
            thresholdMat
        }
        
        // Convert back to bitmap
        val resultBitmap = Bitmap.createBitmap(
            finalMat.cols(),
            finalMat.rows(),
            Bitmap.Config.RGB_565
        )
        Utils.matToBitmap(finalMat, resultBitmap)
        
        // Clean up
        mat.release()
        grayMat.release()
        blurredMat.release()
        thresholdMat.release()
        finalMat.release()
        
        return resultBitmap
    }
    
    /**
     * Detect edges in image
     */
    private fun detectEdges(mat: Mat): Mat {
        val edges = Mat()
        Imgproc.Canny(mat, edges, 50.0, 150.0)
        
        // Dilate edges to connect broken lines
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        val dilated = Mat()
        Imgproc.dilate(edges, dilated, kernel)
        
        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            dilated,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Find largest contour (likely the document)
        var maxArea = 0.0
        var maxContour: MatOfPoint? = null
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                maxArea = area
                maxContour = contour
            }
        }
        
        // Clean up
        edges.release()
        dilated.release()
        hierarchy.release()
        
        return if (maxContour != null) {
            mat
        } else {
            mat
        }
    }
    
    /**
     * Crop image to detected document bounds
     */
    fun cropToDocument(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)
        
        val edges = Mat()
        Imgproc.Canny(grayMat, edges, 50.0, 150.0)
        
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        var maxArea = 0.0
        var maxRect: Rect? = null
        
        for (contour in contours) {
            val rect = Imgproc.boundingRect(contour)
            val area = rect.area()
            
            if (area > maxArea && area > bitmap.width * bitmap.height * 0.1) {
                maxArea = area
                maxRect = rect
            }
        }
        
        val result = if (maxRect != null) {
            val croppedMat = Mat(mat, maxRect)
            val resultBitmap = Bitmap.createBitmap(
                croppedMat.cols(),
                croppedMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(croppedMat, resultBitmap)
            croppedMat.release()
            resultBitmap
        } else {
            bitmap
        }
        
        // Clean up
        mat.release()
        grayMat.release()
        edges.release()
        hierarchy.release()
        
        return result
    }
    
    /**
     * Rotate image
     */
    fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        
        return rotatedBitmap
    }
    
    /**
     * Scale image
     */
    fun scaleImage(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)
        
        if (scale >= 1.0f) {
            return bitmap
        }
        
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            scaledWidth,
            scaledHeight,
            true
        )
        
        return scaledBitmap
    }
    
    /**
     * Create temp image file
     */
    private fun createTempImageFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "temp_$timestamp.jpg"
        return File(context.cacheDir, fileName)
    }
    
    /**
     * Stop camera
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        preview = null
    }
    
    /**
     * Shutdown camera executor
     */
    fun shutdown() {
        cameraExecutor.shutdown()
        stopCamera()
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        val provider = cameraProvider ?: return
        
        val currentSelector = camera?.cameraInfo?.lensFacing
        val newSelector = if (currentSelector == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        
        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                newSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}