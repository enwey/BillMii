# BillMii 开发者指南

## 目录

1. [项目概述](#项目概述)
2. [架构设计](#架构设计)
3. [开发环境搭建](#开发环境搭建)
4. [代码结构](#代码结构)
5. [核心模块](#核心模块)
6. [开发规范](#开发规范)
7. [测试指南](#测试指南)
8. [性能优化](#性能优化)
9. [常见问题](#常见问题)

---

## 项目概述

BillMii 是一款基于 Android 平台的票据和发票管理应用，采用现代化的技术栈和架构设计。

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9.x | 主要开发语言 |
| Jetpack Compose | 1.5.x | UI 框架 |
| Room | 2.6.x | 数据库 ORM |
| Hilt | 2.48.x | 依赖注入 |
| Coroutines | 1.7.x | 异步处理 |
| Flow | 1.7.x | 响应式数据流 |
| CameraX | 1.3.x | 相机功能 |
| ML Kit | 17.x.x | 机器学习 |
| OkHttp3 | 4.12.x | 网络请求 |
| Apache POI | 5.2.x | Excel 处理 |
| SQLCipher | 4.5.x | 数据库加密 |
| PaddleOCR | PP-OCRv4 Mobile | OCR 识别 |

### 最低系统要求

- **Android 版本**：8.0 (API 26) 或更高
- **最小 SDK**：26
- **目标 SDK**：34
- **编译 SDK**：34

---

## 架构设计

### MVVM + Clean Architecture

BillMii 采用 MVVM（Model-View-ViewModel）架构结合 Clean Architecture 原则，实现关注点分离和可测试性。

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Compose    │  │  ViewModel   │  │   State      │  │
│  │     UI       │  │              │  │   Holder     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Use Cases  │  │   Entities   │  │  Repository  │  │
│  │              │  │              │  │  Interfaces  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Repository   │  │   Data       │  │   Service    │  │
│  │  Impl        │  │  Sources     │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 分层说明

#### Presentation Layer（表现层）

- **UI**：使用 Jetpack Compose 构建声明式 UI
- **ViewModel**：管理 UI 状态和业务逻辑
- **State**：使用 StateFlow 和 Compose State 管理状态

#### Domain Layer（领域层）

- **Use Cases**：封装业务逻辑和用例
- **Entities**：核心业务实体
- **Repository Interfaces**：定义数据访问契约

#### Data Layer（数据层）

- **Repository Implementation**：实现数据访问逻辑
- **Data Sources**：数据库、网络、文件等数据源
- **Services**：提供各种服务功能

---

## 开发环境搭建

### 前置要求

1. **Android Studio**：Giraffe (2023.1) 或更高版本
2. **JDK**：17 或更高版本
3. **Gradle**：8.0 或更高版本
4. **Kotlin**：1.9.x
5. **Git**：2.30 或更高版本

### 克隆项目

```bash
git clone https://github.com/your-org/billmii.git
cd billmii
```

### 配置 Gradle

项目使用 Gradle Kotlin DSL，配置文件位于 `build.gradle.kts`。

#### 项目级 build.gradle.kts

```kotlin
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

#### 应用级 build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.billmii.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.billmii.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // 其他依赖...
}
```

### 配置签名

在 `app/build.gradle.kts` 中配置签名：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 运行项目

1. 在 Android Studio 中打开项目
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 "Run" 按钮或按 `Shift + F10`

---

## 代码结构

### 项目目录结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/billmii/android/
│   │   │   ├── BillMiiApplication.kt          # 应用入口
│   │   │   ├── data/                          # 数据层
│   │   │   │   ├── database/                  # 数据库
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── dao/                   # DAO
│   │   │   │   │   │   ├── ReceiptDao.kt
│   │   │   │   │   │   ├── ReimbursementDao.kt
│   │   │   │   │   │   └── ...
│   │   │   │   │   └── converter/             # 类型转换器
│   │   │   │   ├── model/                     # 数据模型
│   │   │   │   │   ├── Receipt.kt
│   │   │   │   │   ├── Reimbursement.kt
│   │   │   │   │   └── ...
│   │   │   │   ├── repository/                # 仓储实现
│   │   │   │   │   ├── ReceiptRepository.kt
│   │   │   │   │   └── ...
│   │   │   │   └── service/                   # 服务
│   │   │   │       ├── VoiceInputService.kt
│   │   │   │       ├── QrCodeScannerService.kt
│   │   │   │       ├── LanIntegrationService.kt
│   │   │   │       └── ...
│   │   │   ├── domain/                        # 领域层
│   │   │   │   ├── usecase/                   # 用例
│   │   │   │   │   ├── GetReceiptsUseCase.kt
│   │   │   │   │   └── ...
│   │   │   │   └── repository/                # 仓储接口
│   │   │   │       ├── ReceiptRepository.kt
│   │   │   │       └── ...
│   │   │   ├── ui/                            # 表现层
│   │   │   │   ├── MainActivity.kt            # 主 Activity
│   │   │   │   ├── navigation/                # 导航
│   │   │   │   │   ├── Screen.kt
│   │   │   │   │   └── NavGraph.kt
│   │   │   │   ├── theme/                     # 主题
│   │   │   │   │   ├── Color.kt
│   │   │   │   │   ├── Theme.kt
│   │   │   │   │   └── Type.kt
│   │   │   │   ├── components/                # 通用组件
│   │   │   │   │   ├── CommonComponents.kt
│   │   │   │   │   ├── ReceiptCard.kt
│   │   │   │   │   └── ...
│   │   │   │   ├── icons/                     # 图标
│   │   │   │   │   └── Icons.kt
│   │   │   │   ├── receipt/                   # 票据模块
│   │   │   │   │   ├── ReceiptListScreen.kt
│   │   │   │   │   ├── ReceiptDetailScreen.kt
│   │   │   │   │   └── viewmodel/
│   │   │   │   │       ├── ReceiptListViewModel.kt
│   │   │   │   │       └── ReceiptDetailViewModel.kt
│   │   │   │   ├── reimbursement/             # 报销模块
│   │   │   │   ├── statistics/                # 统计模块
│   │   │   │   └── settings/                  # 设置模块
│   │   │   └── di/                            # 依赖注入
│   │   │       ├── AppModule.kt
│   │   │       ├── DatabaseModule.kt
│   │   │       └── ...
│   │   ├── res/                               # 资源文件
│   │   │   ├── drawable/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/                           # XML 配置
│   │   │       ├── file_paths.xml
│   │   │       └── backup_rules.xml
│   │   └── AndroidManifest.xml                # 清单文件
│   ├── test/                                  # 单元测试
│   │   └── java/com/billmii/android/
│   │       ├── data/model/
│   │       └── data/service/
│   └── androidTest/                           # 集成测试
│       └── java/com/billmii/android/
│           ├── data/database/
│           └── ui/
├── build.gradle.kts                           # 应用级构建配置
├── proguard-rules.pro                         # ProGuard 规则
└── libs/                                      # 本地库
    └── paddleocr-lite.aar                      # PaddleOCR 库
```

---

## 核心模块

### 1. 票据管理模块

#### 数据模型

```kotlin
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val merchantName: String? = null,
    val amount: Double,
    val currency: String = "CNY",
    val date: Date,
    val category: String? = null,
    val type: ReceiptType,
    val status: ReceiptStatus,
    val imagePath: String? = null,
    val thumbnailPath: String? = null,
    val notes: String? = null,
    val tags: String? = null,
    val ocrText: String? = null,
    val confidenceScore: Float? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
```

#### 数据访问对象 (DAO)

```kotlin
@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAll(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getById(id: Long): Flow<Receipt?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: Receipt): Long

    @Update
    suspend fun update(receipt: Receipt)

    @Delete
    suspend fun delete(receipt: Receipt)

    @Query("SELECT * FROM receipts WHERE title LIKE '%' || :query || '%'")
    fun searchByTitle(query: String): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE category = :category")
    fun getByCategory(category: String): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE status = :status")
    fun getByStatus(status: ReceiptStatus): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE date BETWEEN :startDate AND :endDate")
    fun getByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>>

    @Query("SELECT SUM(amount) FROM receipts WHERE category = :category")
    fun getTotalAmountByCategory(category: String): Flow<Double>
}
```

#### ViewModel

```kotlin
@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptListUiState>(ReceiptListUiState.Loading)
    val uiState: StateFlow<ReceiptListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        viewModelScope.launch {
            receiptRepository.getAll()
                .catch { e ->
                    _uiState.value = ReceiptListUiState.Error(e.message ?: "未知错误")
                }
                .collect { receipts ->
                    _uiState.value = ReceiptListUiState.Success(receipts)
                }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            receiptRepository.search(query)
                .catch { e ->
                    _uiState.value = ReceiptListUiState.Error(e.message ?: "搜索失败")
                }
                .collect { receipts ->
                    _uiState.value = ReceiptListUiState.Success(receipts)
                }
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            receiptRepository.delete(receipt)
        }
    }
}

sealed class ReceiptListUiState {
    object Loading : ReceiptListUiState()
    data class Success(val receipts: List<Receipt>) : ReceiptListUiState()
    data class Error(val message: String) : ReceiptListUiState()
}
```

### 2. OCR 识别模块

#### OCR 服务

```kotlin
@Singleton
class OcrRecognitionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ocrEngine: OcrEngine? = null

    fun initialize() {
        ocrEngine = OcrEngine(context)
        ocrEngine?.init()
    }

    suspend fun recognizeText(imagePath: String): OcrResult {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            ocrEngine?.recognize(bitmap) ?: throw OcrException("OCR 引擎未初始化")
        }
    }

    fun release() {
        ocrEngine?.release()
        ocrEngine = null
    }
}
```

### 3. 语音输入模块

#### 语音输入服务

```kotlin
@Singleton
class VoiceInputService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    fun startVoiceRecognition(language: String = "zh-CN"): Flow<VoiceInputResult> = callbackFlow {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(VoiceInputResult.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(VoiceInputResult.Listening)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    trySend(VoiceInputResult.Success(text))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    trySend(VoiceInputResult.Partial(text))
                }
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "无匹配结果"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误"
                }
                trySend(VoiceInputResult.Error(message))
            }

            override fun onRmsChanged(rmsdB: Float) {
                val level = (rmsdB / 10f).coerceIn(0f, 1f)
                trySend(VoiceInputResult.Volume(level))
            }

            override fun onEndOfSpeech() {
                trySend(VoiceInputResult.Processing)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(intent)

        awaitClose {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }
}
```

### 4. 二维码扫描模块

#### 二维码扫描服务

```kotlin
@Singleton
class QrCodeScannerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
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
    )

    suspend fun scanImage(imageProxy: ImageProxy): Flow<ScanningState> = flow {
        emit(ScanningState.Scanning)

        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            emit(ScanningState.Success(barcode.rawValue ?: "", barcode.format.name))
                        } else {
                            emit(ScanningState.NoCodeFound)
                        }
                    }
                    .addOnFailureListener { e ->
                        emit(ScanningState.Error(e.message ?: "扫描失败"))
                    }
                    .await()
            }
        } finally {
            imageProxy.close()
        }
    }

    fun parseReceiptData(qrData: String): ReceiptQrData? {
        // 实现二维码数据解析逻辑
        // ...
        return null
    }
}
```

---

## 开发规范

### 代码风格

#### Kotlin 编码规范

1. **命名约定**
   - 类名：PascalCase（如 `ReceiptViewModel`）
   - 函数名：camelCase（如 `loadReceipts`）
   - 常量：UPPER_SNAKE_CASE（如 `DEFAULT_TIMEOUT`）
   - 私有属性：camelCase（如 `_uiState`）

2. **注释规范**
   - 使用 KDoc 格式的文档注释
   - 公共 API 必须添加文档注释
   - 复杂逻辑添加行内注释

```kotlin
/**
 * 加载票据列表
 * 
 * @param refresh 是否强制刷新数据
 */
suspend fun loadReceipts(refresh: Boolean = false) {
    // 实现逻辑
}
```

3. **异常处理**
   - 使用 `Result` 封装可能失败的操作
   - 提供有意义的错误信息
   - 使用 `@Throws` 注解声明检查异常

```kotlin
suspend fun recognizeText(imagePath: String): Result<OcrResult> {
    return try {
        val result = ocrEngine?.recognize(imagePath)
            ?: return Result.failure(OcrException("OCR 引擎未初始化"))
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Git 提交规范

#### Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type 类型

- `feat`：新功能
- `fix`：修复 bug
- `docs`：文档更新
- `style`：代码格式调整
- `refactor`：重构
- `test`：测试相关
- `chore`：构建/工具相关

#### 示例

```
feat(receipt): add QR code scanning feature

Implement QR code scanning using ML Kit Barcode Scanning.
Support multiple barcode formats including QR, Aztec, EAN, UPC, etc.

Closes #123
```

### 依赖注入规范

#### 使用 Hilt 进行依赖注入

```kotlin
@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel()

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val ocrService: OcrRecognitionService
) : ReceiptRepository

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "billmii.db"
        ).build()
    }
}
```

---

## 测试指南

### 单元测试

#### 测试位置

```
app/src/test/java/com/billmii/android/
├── data/model/
│   ├── ReceiptTest.kt
│   └── ReimbursementTest.kt
└── data/service/
    ├── VoiceInputServiceTest.kt
    └── QrCodeScannerServiceTest.kt
```

#### 测试示例

```kotlin
@RunWith(JUnit4::class)
class ReceiptTest {

    @Test
    fun `test receipt creation`() {
        val receipt = Receipt(
            id = 1,
            title = "测试票据",
            amount = 100.50,
            date = Date()
        )
        
        assertEquals(1, receipt.id)
        assertEquals("测试票据", receipt.title)
        assertEquals(100.50, receipt.amount, 0.001)
    }
}
```

### 集成测试

#### 测试位置

```
app/src/androidTest/java/com/billmii/android/
├── data/database/
│   └── ReceiptDaoIntegrationTest.kt
└── ui/
    └── receipt/
        └── ReceiptListScreenTest.kt
```

#### 测试示例

```kotlin
@RunWith(AndroidJUnit4::class)
class ReceiptDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var receiptDao: ReceiptDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        receiptDao = database.receiptDao()
    }

    @Test
    fun `test insert and retrieve receipt`() = runTest {
        val receipt = Receipt(
            id = 1,
            title = "测试票据",
            amount = 100.50,
            date = Date()
        )

        receiptDao.insert(receipt)

        val retrieved = receiptDao.getById(1)
        assertNotNull(retrieved)
        assertEquals("测试票据", retrieved?.title)
    }
}
```

### UI 测试

#### 使用 Compose Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class ReceiptListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test receipt list displays correctly`() {
        composeTestRule.setContent {
            ReceiptListScreen(
                receipts = testReceipts,
                isLoading = false,
                onReceiptClick = {}
            )
        }

        composeTestRule.onNodeWithText("测试票据").assertIsDisplayed()
    }
}
```

---

## 性能优化

### 1. 数据库优化

- 使用索引加速查询
- 使用 `@Transaction` 批量操作
- 避免在 UI 线程执行数据库操作

```kotlin
@Query("SELECT * FROM receipts WHERE category = :category")
@Index("category")
fun getByCategory(category: String): Flow<List<Receipt>>

@Transaction
suspend fun batchInsert(receipts: List<Receipt>) {
    receipts.forEach { insert(it) }
}
```

### 2. 图片优化

- 使用 Glide 或 Coil 加载图片
- 生成缩略图减少内存占用
- 使用 WebP 格式压缩图片

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(receipt.thumbnailPath)
        .crossfade(true)
        .build(),
    contentDescription = "票据图片",
    modifier = Modifier.size(80.dp)
)
```

### 3. 内存优化

- 使用 `LazyColumn` 实现列表虚拟化
- 及时释放大对象
- 使用 `WeakReference` 避免内存泄漏

```kotlin
LazyColumn {
    items(receipts) { receipt ->
        ReceiptCard(
            receipt = receipt,
            onClick = { /* ... */ }
        )
    }
}
```

### 4. 协程优化

- 使用 `Dispatchers.IO` 执行 I/O 操作
- 避免在协程中阻塞操作
- 使用 `flow` 处理数据流

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val result = repository.loadData()
    withContext(Dispatchers.Main) {
        _uiState.value = UiState.Success(result)
    }
}
```

---

## 常见问题

### Q: 如何添加新的数据模型？

A:
1. 在 `data/model` 中创建数据类
2. 添加 `@Entity` 注解并指定表名
3. 在 `AppDatabase` 中添加实体
4. 创建对应的 DAO
5. 创建 Repository

### Q: 如何添加新的 UI 页面？

A:
1. 在 `ui` 下创建对应的包
2. 创建 Screen Composable
3. 创建 ViewModel
4. 在 `NavGraph` 中添加导航路由
5. 在 `Screen.kt` 中定义 Screen 对象

### Q: 如何调试 OCR 识别问题？

A:
1. 检查图片质量和分辨率
2. 查看置信度分数
3. 尝试调整 OCR 模板
4. 查看日志中的错误信息

### Q: 如何处理内存泄漏？

A:
1. 使用 LeakCanary 检测内存泄漏
2. 确保 ViewModel 正确清理资源
3. 避免在 Composable 中持有长生命周期对象
4. 使用 `remember` 和 `rememberCoroutineScope` 管理状态

### Q: 如何优化应用启动速度？

A:
1. 使用 App Startup 初始化组件
2. 延迟非必要的初始化
3. 使用 ProGuard/R8 优化代码
4. 优化资源文件

---

## 参考资源

### 官方文档

- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt](https://dagger.dev/hilt/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### 第三方库

- [CameraX](https://developer.android.com/training/camerax)
- [ML Kit](https://developers.google.com/ml-kit)
- [OkHttp](https://square.github.io/okhttp/)
- [Apache POI](https://poi.apache.org/)

---

## 版本信息

- **当前版本**：1.0.0
- **最后更新**：2024年1月
- **维护团队**：BillMii 开发团队