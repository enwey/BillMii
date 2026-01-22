# PaddleOCR Integration Guide

## Overview

BillMii uses PaddleOCR for local OCR recognition of receipts and invoices. This document describes the integration and configuration of PaddleOCR in the project.

## Dependencies

The project uses the PaddleOCR Android SDK from JitPack:

```kotlin
// app/build.gradle.kts
implementation("com.github.PaddlePaddle.PaddleOCR-android:ocrkit:2.3.0")
```

This library includes the PaddleOCR PP-OCRv4 models bundled with the SDK.

## Architecture

### Components

1. **PaddleOCRManager** ([`PaddleOCRManager.kt`](app/src/main/java/com/billmii/android/ocr/PaddleOCRManager.kt))
   - Manages PaddleOCR lifecycle
   - Initializes and configures OCR models
   - Provides detection and recognition methods

2. **OcrService** ([`OcrService.kt`](app/src/main/java/com/billmii/android/data/service/OcrService.kt))
   - High-level OCR service
   - Integrates with PaddleOCRManager
   - Handles receipt-specific data extraction

## PaddleOCRManager

The PaddleOCRManager wraps the PaddleOCR OcrKit and provides a clean interface for OCR operations.

### Initialization

```kotlin
val paddleOCRManager = PaddleOCRManager(context)

// Initialize OCR models
val success = paddleOCRManager.initialize()
```

### Configuration

The manager is configured with default PaddleOCR PP-OCRv4 models:

```kotlin
ocrKit?.apply {
    // Detection model
    setDetModelFileName("ch_PP-OCRv4_det_infer.onnx")
    
    // Recognition model
    setRecModelFileName("ch_PP-OCRv4_rec_infer.onnx")
    
    // Classification model
    setClsModelFileName("ch_ppocr_mobile_v2.0_cls_infer.onnx")
    
    // CPU threads for inference
    setCpuThreadNum(4)
    
    // Power mode
    setCpuPowerMode("LITE_POWER_HIGH")
    
    // Initialize models
    init()
}
```

### Usage

```kotlin
// Detect and recognize text
val result = paddleOCRManager.detectAndRecognize(bitmap)

if (result.success) {
    println("Detected text: ${result.text}")
    println("Confidence: ${result.confidence}")
    
    // Access text boxes
    result.boxes.forEach { box ->
        println("Box: $box")
    }
}

// Recognize text only (no detection)
val text = paddleOCRManager.recognize(bitmap)
```

### Result Structure

```kotlin
data class OcrDetectionResult(
    val success: Boolean,
    val text: String = "",
    val boxes: List<Rect> = emptyList(),
    val confidences: List<Float> = emptyList(),
    val confidence: Float = 0f,
    val error: String? = null
)
```

## OcrService Integration

The OcrService uses PaddleOCRManager for text recognition and adds receipt-specific data extraction.

### Receipt Recognition Flow

```kotlin
val ocrService = OcrService(context, paddleOCRManager)

// Recognize receipt and extract structured data
val result = ocrService.recognizeReceipt(imageFile)

if (result.success) {
    val receiptType = result.receiptType
    val receiptData = result.data
    
    // Access extracted data
    receiptData.invoiceNumber
    receiptData.totalAmount
    receiptData.sellerName
    // ... more fields
}
```

### Supported Receipt Types

1. **VAT Invoice** (增值税发票)
   - Invoice number and code
   - Seller and buyer information
   - Tax ID, address, phone
   - Amount and tax details

2. **Train Ticket** (火车票)
   - Train number
   - Departure and arrival stations
   - Travel date and times
   - Passenger information
   - Seat details

3. **Flight Itinerary** (航空行程单)
   - Flight number
   - Airport information
   - Departure and arrival times
   - Passenger details
   - Carrier information

4. **Taxi Receipt** (出租车发票)
   - Taxi company and license
   - Driver information
   - Pickup and dropoff locations
   - Distance and duration
   - Fare details

5. **Hotel Receipt** (酒店发票)
   - Hotel name and address
   - Check-in/out dates
   - Guest and room information
   - Room rate

6. **Restaurant Receipt** (餐饮发票)
   - Restaurant name and address
   - Date
   - Total amount

7. **General Receipt** (通用票据)
   - Basic date and amount extraction

## Performance Optimization

### Model Configuration

The default configuration is optimized for mobile devices:

- **CPU Threads**: 4 (balanced for most devices)
- **Power Mode**: `LITE_POWER_HIGH` (max performance)
- **Models**: PP-OCRv4 Mobile (optimized for accuracy vs speed)

### Memory Management

```kotlin
// Release resources when no longer needed
paddleOCRManager.release()

// Check if OCR is ready
if (paddleOCRManager.isReady()) {
    // Perform OCR
}
```

### Batch Processing

For multiple images, initialize once and reuse:

```kotlin
paddleOCRManager.initialize()

images.forEach { imageFile ->
    val result = ocrService.recognizeReceipt(imageFile)
    // Process result
}

paddleOCRManager.release()
```

## Troubleshooting

### Initialization Failures

**Issue**: OCR initialization fails

**Solutions**:
1. Check if PaddleOCR library is properly included in build.gradle.kts
2. Ensure JitPack repository is added to settings.gradle.kts
3. Verify Android NDK is installed
4. Check app has sufficient memory

```kotlin
// settings.gradle.kts
repositories {
    maven { url = uri("https://jitpack.io") }
}
```

### Recognition Accuracy

**Issue**: Low recognition accuracy

**Solutions**:
1. Ensure images are preprocessed (grayscale, noise reduction)
2. Check image resolution (recommended: 300-600 DPI)
3. Verify image is not too dark or overexposed
4. Use OpenCV preprocessing for better results

### Performance Issues

**Issue**: Slow recognition speed

**Solutions**:
1. Reduce CPU threads: `setCpuThreadNum(2)`
2. Use lower power mode: `setCpuPowerMode("LITE_POWER_LOW")`
3. Resize images to reasonable size (max 2000x2000)
4. Process images in background threads

### Out of Memory

**Issue**: OOM during recognition

**Solutions**:
1. Downscale large images before OCR
2. Release bitmap after use
3. Process one image at a time
4. Increase app heap size in manifest

```xml
<!-- AndroidManifest.xml -->
<application
    android:largeHeap="true"
    android:hardwareAccelerated="true">
```

## Custom Models

To use custom PaddleOCR models:

1. Place custom model files in `app/src/main/assets/ocr/`
2. Update model file names in PaddleOCRManager:

```kotlin
ocrKit?.apply {
    setDetModelFileName("custom_det.onnx")
    setRecModelFileName("custom_rec.onnx")
    setClsModelFileName("custom_cls.onnx")
    init()
}
```

## Model Files

The PaddleOCR Android SDK includes the following models:

- `ch_PP-OCRv4_det_infer.onnx` - Text detection model
- `ch_PP-OCRv4_rec_infer.onnx` - Text recognition model
- `ch_ppocr_mobile_v2.0_cls_infer.onfer.onnx` - Text direction classification model

These models are optimized for:
- Chinese text recognition (98.7% accuracy)
- Mobile device performance
- Offline operation
- Receipt and invoice formats

## Integration with Camera

The OCR service integrates with the camera module:

```kotlin
// CameraActivity.kt
private val cameraLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        val imagePath = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_PATH)
        
        // Perform OCR
        viewModel.importReceiptFromCamera(imagePath)
    }
}
```

## Testing

### Unit Tests

```kotlin
@Test
fun `OCR service recognizes text correctly`() = runTest {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.test_receipt)
    val result = paddleOCRManager.detectAndRecognize(bitmap)
    
    assertTrue(result.success)
    assertTrue(result.text.isNotEmpty())
    assertTrue(result.confidence > 0.5f)
}
```

### Integration Tests

```kotlin
@Test
fun `OCR service extracts invoice data`() = runTest {
    val imageFile = File(testResources, "vat_invoice.jpg")
    val result = ocrService.recognizeReceipt(imageFile)
    
    assertTrue(result.success)
    assertEquals(ReceiptType.VAT_INVOICE, result.receiptType)
    assertNotNull(result.data.invoiceNumber)
    assertNotNull(result.data.totalAmount)
}
```

## References

- [PaddleOCR GitHub](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddleOCR Android SDK](https://github.com/PaddlePaddle/PaddleOCR-android)
- [PP-OCRv4 Documentation](https://github.com/PaddlePaddle/PaddleOCR/blob/release/2.7/doc/doc_ch/ppocr_introduction.md)

## License

PaddleOCR is licensed under Apache 2.0. See [PaddleOCR License](https://github.com/PaddlePaddle/PaddleOCR/blob/develop/LICENSE) for details.