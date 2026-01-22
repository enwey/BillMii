# BillMii 部署指南

## 目录

1. [概述](#概述)
2. [构建配置](#构建配置)
3. [签名配置](#签名配置)
4. [构建变体](#构建变体)
5. [发布流程](#发布流程)
6. [版本管理](#版本管理)
7. [持续集成](#持续集成)
8. [常见问题](#常见问题)

---

## 概述

本指南介绍如何构建、签名和发布 BillMii Android 应用。

### 构建工具

- **Gradle**：8.0+
- **Android Gradle Plugin**：8.1.0+
- **Kotlin Compiler**：1.9.0+

### 支持的构建类型

- **Debug**：开发调试版本
- **Release**：生产发布版本

### 支持的产品风味

- **Free**：免费版（可选）
- **Pro**：专业版（可选）

---

## 构建配置

### Gradle 配置文件

#### 项目级 build.gradle.kts

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

#### 应用级 build.gradle.kts

```kotlin
// app/build.gradle.kts
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // 依赖项...
}
```

### ProGuard 规则

创建 `app/proguard-rules.pro` 文件：

```proguard
# 保留 Kotlin 数据类
-keep class com.billmii.android.data.model.** { *; }

# 保留 Room 实体
-keep class com.billmii.android.data.database.entities.** { *; }

# 保留 Hilt 生成的类
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# 保留 Compose 生成的类
-keep class androidx.compose.** { *; }

# 保留序列化类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留反射调用的类和方法
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 移除日志
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
```

---

## 签名配置

### 生成签名密钥

#### 使用 keytool 生成密钥

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

#### 输入信息

```
输入密钥库口令: [输入密码]
再次输入新口令: [再次输入密码]
您的名字与姓氏是什么?
  [Unknown]:  BillMii Team
您的组织单位名称是什么?
  [Unknown]:  Development
您的组织名称是什么?
  [Unknown]:  BillMii
您所在的城市或区域名称是什么?
  [Unknown]:  Beijing
您所在的省/市/国家名称是什么?
  [Unknown]:  Beijing
该单位的双字母国家/地区代码是什么?
  [Unknown]:  CN
CN=BillMii Team, OU=Development, O=BillMii, L=Beijing, ST=Beijing, C=CN 是否正确?
  [否]:  y

输入 <release> 的密钥口令
        (如果和密钥库口令相同, 按回车):
```

### 配置签名

#### 方式一：在 build.gradle.kts 中配置

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            storePassword = "your_store_password"
            keyAlias = "release"
            keyPassword = "your_key_password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### 方式二：使用环境变量（推荐）

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "../keystore/release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "release"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### 方式三：使用 keystore.properties 文件

创建项目根目录下的 `keystore.properties` 文件（不要提交到 Git）：

```properties
storeFile=../keystore/release.jks
storePassword=your_store_password
keyAlias=release
keyPassword=your_key_password
```

在 `app/build.gradle.kts` 中读取：

```kotlin
// 在文件开头添加
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}
```

### 添加到 .gitignore

确保签名密钥不被提交到版本控制：

```gitignore
# Keystore files
*.jks
*.keystore

# Keystore properties
keystore.properties

# Signing files
keystore/
```

---

## 构建变体

### Debug 构建

#### 命令行构建

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Debug APK 并运行测试
./gradlew assembleDebug testDebugUnitTest

# 安装到连接的设备
./gradlew installDebug
```

#### Android Studio 构建

1. 选择 `Build > Build Bundle(s) / APK(s) > Build APK(s)`
2. 构建完成后点击通知中的 `locate`
3. 在 `app/build/outputs/apk/debug/` 目录下找到 APK

### Release 构建

#### 命令行构建

```bash
# 构建 Release APK
./gradlew assembleRelease

# 构建 Release AAB（用于 Google Play）
./gradlew bundleRelease

# 构建 Release 并运行测试
./gradlew assembleRelease testReleaseUnitTest

# 检查 Release 构建
./gradlew assembleRelease check
```

#### Android Studio 构建

1. 选择 `Build > Generate Signed Bundle / APK`
2. 选择 `APK` 或 `Android App Bundle`
3. 选择或创建签名密钥
4. 选择 `release` 构建变体
5. 点击 `Finish`

### 构建输出位置

```
app/build/outputs/
├── apk/
│   ├── debug/
│   │   └── app-debug.apk
│   └── release/
│       └── app-release.apk
└── bundle/
    └── release/
        └── app-release.aab
```

---

## 发布流程

### 发布到 Google Play

#### 1. 准备发布

1. **更新版本号**
   ```kotlin
   // app/build.gradle.kts
   defaultConfig {
       versionCode = 2  // 每次发布递增
       versionName = "1.0.1"
   }
   ```

2. **运行测试**
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

3. **构建 Release 包**
   ```bash
   ./gradlew bundleRelease
   ```

4. **测试 Release 包**
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

#### 2. 创建 Google Play Console 账户

1. 访问 [Google Play Console](https://play.google.com/console)
2. 注册开发者账户（支付 $25 注册费）
3. 完成身份验证

#### 3. 创建应用

1. 在 Play Console 中点击"创建应用"
2. 输入应用名称（BillMii）
3. 选择应用类型（应用）
4. 选择免费或付费
5. 声明应用内容（是否包含广告等）

#### 4. 准备应用信息

1. **应用详情**
   - 应用名称：BillMii
   - 简短描述：票据和发票管理应用
   - 完整描述：详细的应用介绍
   - 应用图标：512x512 高分辨率图标
   - 特征图形：1024x500 宣传图

2. **商店列表**
   - 应用截图：至少 2 张，最多 8 张
   - 视频（可选）

3. **隐私政策**
   - 创建隐私政策页面
   - 说明数据收集和使用

4. **内容评级**
   - 完成内容评级问卷

#### 5. 上传应用

1. 进入"发布" > "生产环境"或"测试轨道"
2. 点击"创建新发布"
3. 上传 AAB 文件（`app-release.aab`）
4. 等待 Google Play 处理

#### 6. 配置发布

1. **发布内容**
   - 选择要发布的语言
   - 添加发布说明

2. **发布类型**
   - 标准发布
   - 分阶段发布（可选）

3. **发布时间**
   - 立即发布
   - 定时发布

#### 7. 提交审核

1. 检查所有必需信息是否完整
2. 点击"开始发布到正式版"
3. 等待 Google Play 审核（通常 1-3 天）

#### 8. 发布后

1. 监控崩溃和 ANR
2. 回复用户评价
3. 分析用户反馈
4. 准备下一个版本

### 发布到其他应用商店

#### 国内应用商店

| 应用商店 | 上传方式 | 审核时间 |
|----------|----------|----------|
| 华为应用市场 | 开发者后台 | 1-3 天 |
| 小米应用商店 | 开发者后台 | 1-2 天 |
| OPPO 软件商店 | 开发者后台 | 1-3 天 |
| vivo 应用商店 | 开发者后台 | 1-3 天 |
| 应用宝 | 开发者后台 | 1-3 天 |
| 百度手机助手 | 开发者后台 | 1-3 天 |

#### 发布步骤（通用）

1. 注册开发者账户
2. 创建应用
3. 填写应用信息
4. 上传 APK
5. 提交审核
6. 等待审核结果

### 内部分发

#### 使用 Firebase App Distribution

```bash
# 安装 Firebase CLI
npm install -g firebase-tools

# 登录
firebase login

# 初始化项目
firebase init

# 分发应用
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
  --app 1:123456789:android:abcdef \
  --groups "testers" \
  --release-notes "版本 1.0.0 - 初始发布"
```

#### 使用第三方分发平台

- **蒲公英**：https://www.pgyer.com/
- **fir.im**：https://fir.im/
- **TestFlight**：iOS（如支持）

---

## 版本管理

### 版本号规范

#### Semantic Versioning

采用语义化版本控制：`MAJOR.MINOR.PATCH`

- **MAJOR**：不兼容的 API 更改
- **MINOR**：向后兼容的功能新增
- **PATCH**：向后兼容的 Bug 修复

#### 版本号示例

```
1.0.0 - 初始发布
1.0.1 - Bug 修复
1.1.0 - 新增功能
2.0.0 - 重大更新
```

#### Gradle 配置

```kotlin
android {
    defaultConfig {
        versionCode = 1  // 每次发布递增
        versionName = "1.0.0"
    }
}
```

### 版本发布检查清单

#### 发布前

- [ ] 更新版本号（versionCode 和 versionName）
- [ ] 更新 CHANGELOG.md
- [ ] 运行所有测试
- [ ] 检查代码覆盖率
- [ ] 更新文档
- [ ] 检查依赖项安全性
- [ ] 测试 Release 构建版本
- [ ] 准备发布说明

#### 发布中

- [ ] 构建 Release 包
- [ ] 签名验证
- [ ] 上传到应用商店
- [ ] 填写应用信息
- [ ] 提交审核

#### 发布后

- [ ] 监控崩溃和 ANR
- [ ] 监控用户反馈
- [ ] 监控下载量和评分
- [ ] 分析用户行为数据
- [ ] 收集 Bug 报告
- [ ] 规划下一个版本

### 版本回滚

如果发现严重问题，可以回滚到之前的版本：

1. **Google Play**
   - 在 Play Console 中找到应用
   - 进入"发布" > "生产环境"
   - 点击"取消发布"
   - 发布之前的版本

2. **其他应用商店**
   - 联系应用商店客服
   - 提供回滚请求

---

## 持续集成

### GitHub Actions 配置

创建 `.github/workflows/build.yml`：

```yaml
name: Build and Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run unit tests
      run: ./gradlew test
    
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        target: default
        arch: x86_64
        profile: Nexus 6
        script: ./gradlew connectedAndroidTest
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
    
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: app/build/test-results/
```

### 自动发布到 Firebase

创建 `.github/workflows/release.yml`：

```yaml
name: Release to Firebase

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Decode keystore
      run: |
        echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > keystore/release.jks
    
    - name: Build Release APK
      run: |
        ./gradlew assembleRelease \
          -PKEYSTORE_PASSWORD=${{ secrets.KEYSTORE_PASSWORD }} \
          -PKEY_ALIAS=${{ secrets.KEY_ALIAS }} \
          -PKEY_PASSWORD=${{ secrets.KEY_PASSWORD }}
    
    - name: Install Firebase CLI
      run: npm install -g firebase-tools
    
    - name: Deploy to Firebase
      run: |
        firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
          --app ${{ secrets.FIREBASE_APP_ID }} \
          --groups "testers" \
          --token ${{ secrets.FIREBASE_TOKEN }}
```

### 环境变量配置

在 GitHub 仓库设置中添加 Secrets：

| Secret 名称 | 描述 |
|-------------|------|
| KEYSTORE_BASE64 | Base64 编码的 keystore 文件 |
| KEYSTORE_PASSWORD | Keystore 密码 |
| KEY_ALIAS | 密钥别名 |
| KEY_PASSWORD | 密钥密码 |
| FIREBASE_APP_ID | Firebase 应用 ID |
| FIREBASE_TOKEN | Firebase CLI 令牌 |

---

## 常见问题

### Q: 构建失败，提示签名错误？

A: 检查以下几点：
1. 确认 keystore 文件存在
2. 确认密码配置正确
3. 确认 keyAlias 配置正确
4. 检查 keystore 文件权限

### Q: APK 安装失败，提示签名不匹配？

A: 确保使用相同的签名密钥：
- Debug 和 Release 使用不同的密钥
- 卸载旧版本后再安装新版本
- 检查 applicationId 是否一致

### Q: Google Play 审核被拒绝？

A: 常见原因：
1. 违反内容政策
2. 缺少隐私政策
3. 应用描述不准确
4. 应用图标不符合规范
5. 权限使用不当

### Q: 如何验证 APK 签名？

A: 使用以下命令：

```bash
# 检查 APK 签名
keytool -printcert -jarfile app-release.apk

# 验证签名
jarsigner -verify -verbose -certs app-release.apk
```

### Q: 如何减小 APK 大小？

A: 优化方法：
1. 启用 R8/ProGuard
2. 启用资源压缩
3. 使用 App Bundle
4. 优化图片资源
5. 移除未使用的依赖
6. 启用动态功能模块

### Q: 如何测试 Release 构建？

A: 方法：
1. 使用测试轨道发布
2. 使用 Firebase App Distribution
3. 使用内部测试组
4. 手动安装 APK 进行测试

### Q: 如何处理崩溃和 ANR？

A: 步骤：
1. 集成 Firebase Crashlytics
2. 启用 Play Console 崩溃报告
3. 分析崩溃堆栈
4. 修复问题
5. 发布新版本

---

## 附录

### 有用的命令

```bash
# 清理构建
./gradlew clean

# 构建所有变体
./gradlew assemble

# 运行所有测试
./gradlew test

# 生成测试覆盖率报告
./gradlew jacocoTestReport

# 检查依赖更新
./gradlew dependencyUpdates

# 分析 APK
./gradlew analyzeReleaseBundle

# 导出依赖树
./gradlew app:dependencies
```

### 有用的链接

- [Android App Bundle](https://developer.android.com/guide/app-bundle)
- [Google Play Console](https://play.google.com/console)
- [Firebase App Distribution](https://firebase.google.com/docs/app-distribution)
- [ProGuard](https://www.guardsquare.com/manual/home)
- [R8](https://developer.android.com/studio/build/shrink-code)

---

## 版本信息

- **当前版本**：1.0.0
- **最后更新**：2024年1月
- **维护团队**：BillMii 开发团队