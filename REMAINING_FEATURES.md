# Remaining Features Documentation

## Overview

This document describes the remaining features and enhancements to be implemented in the BillMii project.

## Recently Implemented Features

### 1. Statistics Module (Module 7 - Auxiliary Functions)

**Files Created:**
- [`app/src/main/java/com/billmii/android/ui/statistics/StatisticsScreen.kt`](app/src/main/java/com/billmii/android/ui/statistics/StatisticsScreen.kt)
- [`app/src/main/java/com/billmii/android/ui/statistics/viewmodel/StatisticsViewModel.kt`](app/src/main/java/com/billmii/android/ui/statistics/viewmodel/StatisticsViewModel.kt)

**Features:**
- Overview tab with summary cards (total receipts, reimbursements, amount, pending)
- Receipt statistics tab (by type, by category)
- Reimbursement statistics tab (by status, amount distribution)
- Charts tab (placeholder for MPAndroidChart integration)
- Recent activity feed
- Real-time statistics with refresh capability

### 2. Dialog Components

**Files Created:**
- [`app/src/main/java/com/billmii/android/ui/dialog/ConfirmDialog.kt`](app/src/main/java/com/billmii/android/ui/dialog/ConfirmDialog.kt)
- [`app/src/main/java/com/billmii/android/ui/dialog/InputDialog.kt`](app/src/main/java/com/billmii/android/ui/dialog/InputDialog.kt)

**Features:**
- Generic confirmation dialog with customizable title, message, and buttons
- Single-line input dialog with validation
- Multi-line input dialog for longer text
- Material Design 3 styling

### 3. Security Features

**Files Created:**
- [`app/src/main/java/com/billmii/android/security/BiometricAuthManager.kt`](app/src/main/java/com/billmii/android/security/BiometricAuthManager.kt)
- [`app/src/main/java/com/billmii/android/security/EncryptionKeyManager.kt`](app/src/main/java/com/billmii/android/security/EncryptionKeyManager.kt)
- [`app/src/main/java/com/billmii/android/ui/auth/AppLockScreen.kt`](app/src/main/java/com/billmii/android/ui/auth/AppLockScreen.kt)
- [`app/src/main/java/com/billmii/android/ui/auth/viewmodel/AppLockViewModel.kt`](app/src/main/java/com/billmii/android/ui/auth/viewmodel/AppLockViewModel.kt)

**Features:**
- Biometric authentication (fingerprint/face) using BiometricPrompt
- Android Keystore integration for secure key storage
- AES-256 encryption for sensitive data
- App lock screen with authentication
- Biometric availability checking
- Key management (create, delete, check existence)

### 4. Image Loading Enhancement

**Files Modified:**
- [`app/src/main/java/com/billmii/android/ui/receipt/ReceiptDetailScreen.kt`](app/src/main/java/com/billmii/android/ui/receipt/ReceiptDetailScreen.kt)
- [`app/build.gradle.kts`](app/build.gradle.kts)

**Features:**
- Coil image loading integration for Compose
- Async image display in ReceiptDetailScreen
- File existence checking
- Error state handling

### 5. Dependency Injection

**Files Modified:**
- [`app/build.gradle.kts`](app/build.gradle.kts)
- [`app/src/main/java/com/billmii/android/ui/MainActivity.kt`](app/src/main/java/com/billmii/android/ui/MainActivity.kt)

**Features:**
- Hilt dependency injection setup
- @AndroidEntryPoint annotations
- @Inject annotations for dependencies

### 6. Navigation Updates

**Files Modified:**
- [`app/src/main/java/com/billmii/android/ui/navigation/Screen.kt`](app/src/main/java/com/billmii/android/ui/navigation/Screen.kt)

**Features:**
- Added title property to Screen class
- Updated screen titles (票据, 报销, 统计, 设置)

## Remaining Tasks

### 1. Loading States and Indicators

**Priority: Medium**

Add loading states to ViewModels to provide better user feedback during async operations.

**Implementation Steps:**

1. Add `isLoading` StateFlow to existing ViewModels
2. Show CircularProgressIndicator during operations
3. Add LinearProgressIndicator for long-running operations
4. Implement error state display

**Example Implementation:**

```kotlin
// In ViewModel
private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

// In Composable
if (viewModel.isLoading.collectAsState().value) {
    CircularProgressIndicator()
}
```

**Files to Update:**
- [`ReceiptListViewModel.kt`](app/src/main/java/com/billmii/android/ui/receipt/viewmodel/ReceiptListViewModel.kt)
- [`ReceiptDetailViewModel.kt`](app/src/main/java/com/billmii/android/ui/receipt/viewmodel/ReceiptDetailViewModel.kt)
- [`ReimbursementListViewModel.kt`](app/src/main/java/com/billmii/android/ui/reimbursement/viewmodel/ReimbursementListViewModel.kt)
- [`ReimbursementDetailViewModel.kt`](app/src/main/java/com/billmii/android/ui/reimbursement/viewmodel/ReimbursementDetailViewModel.kt)

### 2. Batch Operations UI

**Priority: High**

Implement batch operations for managing multiple receipts and reimbursements.

**Features to Implement:**

1. **Batch Delete**
   - Selection mode in ReceiptListScreen
   - Multi-select with checkboxes
   - Confirm dialog before deletion
   - Progress indicator during deletion

2. **Batch Export**
   - Select multiple receipts
   - Export to Excel/CSV
   - Progress tracking

3. **Batch OCR**
   - Trigger OCR on multiple unprocessed receipts
   - Progress indicator
   - Results summary

4. **Batch Classification**
   - Apply classification rules to multiple receipts
   - Archive number generation
   - Progress tracking

**Implementation Steps:**

```kotlin
// In ReceiptListViewModel
private val _selectedReceipts = MutableStateFlow<Set<Long>>(emptySet())
val selectedReceipts: StateFlow<Set<Long>> = _selectedReceipts.asStateFlow()

suspend fun deleteSelectedReceipts() {
    _isLoading.value = true
    try {
        selectedReceipts.value.forEach { id ->
            receiptRepository.deleteReceipt(id)
        }
        _selectedReceipts.value = emptySet()
    } finally {
        _isLoading.value = false
    }
}
```

**Files to Create/Update:**
- Update [`ReceiptListScreen.kt`](app/src/main/java/com/billmii/android/ui/receipt/ReceiptListScreen.kt) with selection mode
- Update [`ReceiptListViewModel.kt`](app/src/main/java/com/billmii/android/ui/receipt/viewmodel/ReceiptListViewModel.kt) with batch operations

### 3. Material Icons Resources

**Priority: Low**

Add custom Material Icons for the app branding.

**Implementation Steps:**

1. Create icon vector drawables in `res/drawable/`
2. Use ImageVector resource in Compose

**Icons Needed:**
- App logo/icon
- Custom action icons
- Category icons

### 4. Unit Tests

**Priority: High**

Write comprehensive unit tests for core services and business logic.

**Test Coverage Goals:**

1. **Repository Tests**
   - [`ReceiptRepositoryTest.kt`](app/src/test/java/com/billmii/android/data/repository/ReceiptRepositoryTest.kt)
   - [`ReimbursementRepositoryTest.kt`](app/src/test/java/com/billmii/android/data/repository/ReimbursementRepositoryTest.kt)

2. **Service Tests**
   - [`FileStorageServiceTest.kt`](app/src/test/java/com/billmii/android/data/service/FileStorageServiceTest.kt)
   - [`OcrServiceTest.kt`](app/src/test/java/com/billmii/android/data/service/OcrServiceTest.kt)
   - [`ClassificationServiceTest.kt`](app/src/test/java/com/billmii/android/data/service/ClassificationServiceTest.kt)
   - [`BackupServiceTest.kt`](app/src/test/java/com/billmii/android/data/service/BackupServiceTest.kt)

3. **ViewModel Tests**
   - [`ReceiptListViewModelTest.kt`](app/src/test/java/com/billmii/android/ui/receipt/viewmodel/ReceiptListViewModelTest.kt)
   - [`StatisticsViewModelTest.kt`](app/src/test/java/com/billmii/android/ui/statistics/viewmodel/StatisticsViewModelTest.kt)

**Test Dependencies (already in build.gradle.kts):**
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
```

**Example Test:**

```kotlin
@RunWith(MockitoJUnitRunner::class)
class ReceiptRepositoryTest {
    @Mock
    private lateinit var receiptDao: ReceiptDao
    
    @Mock
    private lateinit var fileStorageService: FileStorageService
    
    private lateinit var repository: ReceiptRepository
    
    @Before
    fun setup() {
        repository = ReceiptRepository(receiptDao, fileStorageService)
    }
    
    @Test
    fun `getAllReceipts returns list of receipts`() = runTest {
        // Given
        val expectedReceipts = listOf(testReceipt)
        whenever(receiptDao.getAllReceipts()).thenReturn(flowOf(expectedReceipts))
        
        // When
        val result = repository.getAllReceipts().first()
        
        // Then
        assertEquals(expectedReceipts, result)
    }
}
```

### 5. Integration Tests

**Priority: Medium**

Write integration tests for database operations and full workflows.

**Test Coverage Goals:**

1. **Database Tests**
   - [`BillMiiDatabaseTest.kt`](app/src/androidTest/java/com/billmii/android/data/database/BillMiiDatabaseTest.kt)
   - Test Room database with SQLCipher encryption
   - Test DAO operations

2. **Workflow Tests**
   - Test complete receipt import → OCR → classification → reimbursement flow
   - Test backup and restore operations

**Test Dependencies (already in build.gradle.kts):**
```kotlin
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.room:room-testing:2.6.1")
```

### 6. UI Tests

**Priority: Medium**

Write Compose UI tests for critical user flows.

**Test Coverage Goals:**

1. **Navigation Tests**
   - Test navigation between screens
   - Test back navigation

2. **Screen Tests**
   - [`ReceiptListScreenTest.kt`](app/src/androidTest/java/com/billmii/android/ui/receipt/ReceiptListScreenTest.kt)
   - [`ReceiptDetailScreenTest.kt`](app/src/androidTest/java/com/billmii/android/ui/receipt/ReceiptDetailScreenTest.kt)
   - [`StatisticsScreenTest.kt`](app/src/androidTest/java/com/billmii/android/ui/statistics/StatisticsScreenTest.kt)

**Test Dependencies (already in build.gradle.kts):**
```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

**Example Test:**

```kotlin
@RunWith(AndroidJUnit4::class)
class ReceiptListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun receiptList_displaysReceipts() {
        // Given
        val receipts = listOf(testReceipt)
        
        // When
        composeTestRule.setContent {
            ReceiptListScreen(
                paddingValues = PaddingValues(0.dp),
                receipts = receipts,
                onReceiptClick = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Test Receipt").assertIsDisplayed()
    }
}
```

### 7. User Guide

**Priority: Low**

Create comprehensive user documentation with screenshots.

**Sections to Include:**

1. **Getting Started**
   - Installation
   - First-time setup
   - Permissions

2. **Core Features**
   - Capturing receipts with camera
   - Importing files from gallery
   - OCR recognition
   - Viewing receipt details

3. **Classification & Archiving**
   - Manual classification
   - Automatic classification rules
   - Archive number system

4. **Reimbursement Management**
   - Creating reimbursement requests
   - Approval workflow
   - Tracking status

5. **Statistics**
   - Overview dashboard
   - Receipt statistics
   - Reimbursement statistics

6. **Settings**
   - Biometric authentication
   - Data backup
   - Export options

**File to Create:**
- [`USER_GUIDE.md`](USER_GUIDE.md)

### 8. Developer Documentation

**Priority: Medium**

Create comprehensive API reference and developer guide.

**Sections to Include:**

1. **Architecture**
   - MVVM + Clean Architecture
   - Repository pattern
   - Dependency injection with Hilt

2. **Data Layer**
   - Room database schema
   - DAO API reference
   - Type converters

3. **Service Layer**
   - FileStorageService API
   - OcrService API
   - ClassificationService API
   - BackupService API
   - ExportService API

4. **UI Layer**
   - Composable components
   - Navigation
   - State management

5. **Security**
   - Encryption implementation
   - Biometric authentication
   - Key management

**File to Create:**
- [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md)

### 9. Deployment Guide

**Priority: Medium**

Create guide for building and releasing the app.

**Sections to Include:**

1. **Build Configuration**
   - Release build settings
   - ProGuard rules
   - Signing configuration

2. **Testing Checklist**
   - Pre-release testing
   - Device compatibility
   - Performance testing

3. **Release Process**
   - Version management
   - Release notes
   - Upload to Play Store

4. **Continuous Integration**
   - GitHub Actions setup
   - Automated testing
   - Automated builds

**File to Create:**
- [`DEPLOYMENT_GUIDE.md`](DEPLOYMENT_GUIDE.md)

## Known Limitations

### OCR Service
The OCR service uses placeholder implementation. To complete it:

1. Download PaddleOCR model files (~50MB)
2. Place in `app/src/main/assets/ocr/`
3. Update [`OcrService.kt`](app/src/main/java/com/billmii/android/data/service/OcrService.kt) to load models

### Charts
The Charts tab in StatisticsScreen is a placeholder. To complete it:

1. Integrate MPAndroidChart library (already included in dependencies)
2. Create Compose wrappers for chart components
3. Update [`StatisticsScreen.kt`](app/src/main/java/com/billmii/android/ui/statistics/StatisticsScreen.kt)

### Biometric Authentication
The AppLockScreen is implemented but not integrated into MainActivity. To complete:

1. Add app lock check in MainActivity
2. Show AppLockScreen before main content if enabled
3. Handle biometric result

## Development Best Practices

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs

### Error Handling
- Use Kotlin Result<T> for operations that can fail
- Provide user-friendly error messages
- Log errors for debugging

### Testing
- Write unit tests for business logic
- Write integration tests for database operations
- Write UI tests for critical user flows

### Performance
- Use Flow for reactive data streams
- Implement pagination for large lists
- Cache expensive operations

### Security
- Use SQLCipher for database encryption
- Use Android Keystore for cryptographic keys
- Validate user inputs
- Sanitize file paths

## Next Steps

1. **Immediate Priority:**
   - Implement batch operations UI
   - Add loading states to ViewModels
   - Write unit tests for core services

2. **Short Term:**
   - Write integration tests
   - Write UI tests
   - Create user guide

3. **Long Term:**
   - Create developer documentation
   - Create deployment guide
   - Add advanced features (charts, advanced search, etc.)

## Contact

For questions or issues, please refer to the main [README.md](README.md) or contact the development team.