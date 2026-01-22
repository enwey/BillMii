# BillMii API 文档

## 目录

1. [概述](#概述)
2. [数据模型](#数据模型)
3. [服务层](#服务层)
4. [数据库接口](#数据库接口)
5. [API 端点](#api-端点)
6. [错误处理](#错误处理)
7. [安全认证](#安全认证)

---

## 概述

BillMii API 文档描述了应用内部的服务接口和数据模型。这些 API 主要用于内部组件通信，部分服务也支持与外部财务软件的集成。

### 技术栈

- **语言**：Kotlin
- **架构**：MVVM + Clean Architecture
- **数据库**：Room (SQLite + SQLCipher)
- **依赖注入**：Hilt
- **异步处理**：Kotlin Coroutines + Flow
- **网络**：OkHttp3

---

## 数据模型

### Receipt（票据）

票据数据模型，存储票据的基本信息和 OCR 识别结果。

#### 属性

| 属性名 | 类型 | 描述 | 必填 |
|--------|------|------|------|
| id | Long | 票据唯一标识 | 是 |
| title | String | 票据标题 | 是 |
| merchantName | String | 商户名称 | 否 |
| amount | Double | 金额 | 是 |
| currency | String | 货币代码（默认 CNY） | 否 |
| date | Date | 票据日期 | 是 |
| category | String | 分类 | 否 |
| type | ReceiptType | 票据类型 | 是 |
| status | ReceiptStatus | 状态 | 是 |
| imagePath | String | 图片路径 | 否 |
| thumbnailPath | String | 缩略图路径 | 否 |
| notes | String | 备注 | 否 |
| tags | String | 标签（逗号分隔） | 否 |
| ocrText | String | OCR 识别文本 | 否 |
| confidenceScore | Float | OCR 置信度 (0-1) | 否 |
| createdAt | Date | 创建时间 | 是 |
| updatedAt | Date | 更新时间 | 是 |

#### ReceiptType 枚举

| 值 | 显示名称 | 描述 |
|----|----------|------|
| INVOICE | 发票 | 增值税发票 |
| RECEIPT | 收据 | 普通收据 |
| VOUCHER | 凭证 | 费用凭证 |
| CONTRACT | 合同 | 合同文件 |
| OTHER | 其他 | 其他类型 |

#### ReceiptStatus 枚举

| 值 | 显示名称 | 描述 |
|----|----------|------|
| PENDING | 待处理 | 新创建的票据 |
| PROCESSING | 处理中 | 正在 OCR 识别 |
| CLASSIFIED | 已分类 | 已完成分类 |
| APPROVED | 已通过 | 已通过审核 |
| REJECTED | 已拒绝 | 已被拒绝 |

---

### Reimbursement（报销单）

报销单数据模型，存储报销申请信息。

#### 属性

| 属性名 | 类型 | 描述 | 必填 |
|--------|------|------|------|
| id | Long | 报销单唯一标识 | 是 |
| title | String | 报销单标题 | 是 |
| applicant | String | 申请人 | 是 |
| department | String | 部门 | 否 |
| totalAmount | Double | 总金额 | 是 |
| currency | String | 货币代码（默认 CNY） | 否 |
| status | ReimbursementStatus | 状态 | 是 |
| submitDate | Date | 提交日期 | 否 |
| approvalDate | Date | 审批日期 | 否 |
| approver | String | 审批人 | 否 |
| description | String | 描述 | 否 |
| receiptIds | String | 关联票据 ID（逗号分隔） | 否 |
| category | String | 报销类别 | 否 |
| expenseType | String | 费用类型 | 否 |
| projectCode | String | 项目代码 | 否 |
| createdAt | Date | 创建时间 | 是 |
| updatedAt | Date | 更新时间 | 是 |

#### ReimbursementStatus 枚举

| 值 | 显示名称 | 颜色 | 描述 |
|----|----------|------|------|
| PENDING | 草稿 | #FFA500 | 未提交 |
| SUBMITTED | 已提交 | #2196F3 | 等待审批 |
| APPROVED | 已通过 | #4CAF50 | 审批通过 |
| REJECTED | 已拒绝 | #F44336 | 审批拒绝 |
| PAID | 已支付 | #9C27B0 | 已完成支付 |
| CANCELLED | 已取消 | #9E9E9E | 已取消 |

---

### ClassificationRule（分类规则）

智能分类规则数据模型。

#### 属性

| 属性名 | 类型 | 描述 | 必填 |
|--------|------|------|------|
| id | Long | 规则唯一标识 | 是 |
| name | String | 规则名称 | 是 |
| category | String | 目标分类 | 是 |
| keywords | String | 关键词（逗号分隔） | 否 |
| merchantPattern | String | 商户匹配模式（正则） | 否 |
| minAmount | Double | 最小金额 | 否 |
| maxAmount | Double | 最大金额 | 否 |
| priority | Int | 优先级（数字越大优先级越高） | 是 |
| isEnabled | Boolean | 是否启用 | 是 |
| createdAt | Date | 创建时间 | 是 |
| updatedAt | Date | 更新时间 | 是 |

---

### OperationLog（操作日志）

操作日志数据模型，记录所有操作历史。

#### 属性

| 属性名 | 类型 | 描述 | 必填 |
|--------|------|------|------|
| id | Long | 日志唯一标识 | 是 |
| operationType | OperationType | 操作类型 | 是 |
| entityType | EntityType | 实体类型 | 是 |
| entityId | Long | 实体 ID | 是 |
| userId | String | 用户 ID | 是 |
| userName | String | 用户名 | 否 |
| operation | String | 操作描述 | 是 |
| details | String | 详细信息（JSON） | 否 |
| timestamp | Date | 时间戳 | 是 |

---

## 服务层

### VoiceInputService（语音输入服务）

提供语音转文字功能。

#### 方法

##### startVoiceRecognition(language: String = "zh-CN")

启动语音识别。

**参数：**
- `language`：语言代码（默认 zh-CN）

**返回：** `Flow<VoiceInputResult>`

**支持的语音输入结果类型：**

| 类型 | 描述 |
|------|------|
| Ready | 准备就绪 |
| Listening | 正在监听 |
| Processing | 正在处理 |
| Partial | 部分识别结果（text: String） |
| Success | 识别成功（text: String） |
| Volume | 音量变化（level: Float, 0-1） |
| Error | 错误（message: String） |

**示例：**
```kotlin
voiceInputService.startVoiceRecognition("zh-CN")
    .collect { result ->
        when (result) {
            is VoiceInputResult.Success -> {
                // 处理识别结果
                val text = result.text
            }
            is VoiceInputResult.Error -> {
                // 处理错误
                val error = result.message
            }
            // ... 其他状态
        }
    }
```

---

### QrCodeScannerService（二维码扫描服务）

提供二维码和条形码扫描功能。

#### 方法

##### scanImage(imageProxy: ImageProxy)

扫描 CameraX 图像中的二维码。

**参数：**
- `imageProxy`：CameraX ImageProxy 对象

**返回：** `Flow<ScanningState>`

##### scanBitmap(bitmap: Bitmap)

扫描位图中的二维码。

**参数：**
- `bitmap`：要扫描的位图

**返回：** `Flow<ScanningState>`

**支持的扫描状态类型：**

| 类型 | 描述 |
|------|------|
| Idle | 空闲状态 |
| Scanning | 正在扫描 |
| NoCodeFound | 未找到二维码 |
| Success | 扫描成功（data: String, format: String） |
| Error | 扫描错误（message: String） |

**支持的条码格式：**
- QR_CODE
- AZTEC
- EAN_13, EAN_8
- UPC_A, UPC_E
- CODE_128, CODE_39, CODE_93
- CODABAR, ITF
- DATA_MATRIX
- PDF_417

##### parseReceiptData(qrData: String)

解析票据二维码数据。

**参数：**
- `qrData`：二维码数据

**返回：** `ReceiptQrData?`

**ReceiptQrData 属性：**
- `type`：类型
- `invoiceNumber`：发票号码
- `invoiceDate`：发票日期
- `amount`：金额
- `verificationCode`：校验码
- `paymentMethod`：支付方式
- `transactionId`：交易 ID
- `merchantName`：商户名称
- `rawData`：原始数据

**示例：**
```kotlin
qrCodeScannerService.scanImage(imageProxy)
    .collect { state ->
        when (state) {
            is QrCodeScannerService.ScanningState.Success -> {
                val qrData = state.data
                val receiptData = qrCodeScannerService.parseReceiptData(qrData)
                // 处理解析结果
            }
            // ... 其他状态
        }
    }
```

---

### LanIntegrationService（局域网集成服务）

提供与局域网财务软件的集成功能。

#### 方法

##### testConnection(serverUrl: String, timeout: Long = 5000)

测试服务器连接。

**参数：**
- `serverUrl`：服务器 URL
- `timeout`：超时时间（毫秒）

**返回：** `Flow<ConnectionResult>`

##### syncReceipts(serverUrl: String, receipts: List<Receipt>)

同步票据到服务器。

**参数：**
- `serverUrl`：服务器 URL
- `receipts`：要同步的票据列表

**返回：** `Flow<SyncResult>`

##### pullReimbursements(serverUrl: String)

从服务器拉取报销单。

**参数：**
- `serverUrl`：服务器 URL

**返回：** `Flow<SyncResult>`

**同步结果类型：**

| 类型 | 描述 |
|------|------|
| Connecting | 正在连接 |
| Connected | 连接成功 |
| Syncing | 正在同步 |
| Success | 同步成功（count: Int） |
| Error | 同步失败（message: String） |

**示例：**
```kotlin
lanIntegrationService.syncReceipts(serverUrl, receipts)
    .collect { result ->
        when (result) {
            is LanIntegrationService.SyncResult.Success -> {
                val syncedCount = result.count
                // 处理成功
            }
            is LanIntegrationService.SyncResult.Error -> {
                val error = result.message
                // 处理错误
            }
            // ... 其他状态
        }
    }
```

---

### BackupEncryptionService（备份加密服务）

提供数据加密和解密功能。

#### 方法

##### encryptData(data: ByteArray, password: String, salt: ByteArray? = null)

加密数据。

**参数：**
- `data`：要加密的数据
- `password`：加密密码
- `salt`：盐值（可选，默认随机生成）

**返回：** `EncryptionResult`（encryptedData: ByteArray, salt: ByteArray, iv: ByteArray）

##### decryptData(encryptedData: ByteArray, password: String, salt: ByteArray, iv: ByteArray)

解密数据。

**参数：**
- `encryptedData`：加密的数据
- `password`：解密密码
- `salt`：盐值
- `iv`：初始化向量

**返回：** `ByteArray`（解密后的数据）

**加密参数：**
- 算法：AES-256-GCM
- 密钥派生：PBKDF2WithHmacSHA256
- 迭代次数：100,000
- 密钥长度：256 位
- IV 长度：128 位
- Salt 长度：128 位

**示例：**
```kotlin
// 加密
val result = backupEncryptionService.encryptData(data, password)
val encrypted = result.encryptedData
val salt = result.salt
val iv = result.iv

// 解密
val decrypted = backupEncryptionService.decryptData(encrypted, password, salt, iv)
```

---

### BiometricAuthService（生物识别认证服务）

提供生物识别认证功能。

#### 方法

##### authenticate(title: String, subtitle: String? = null, description: String? = null)

启动生物识别认证。

**参数：**
- `title`：标题
- `subtitle`：副标题（可选）
- `description`：描述（可选）

**返回：** `Flow<AuthenticationResult>`

**认证结果类型：**

| 类型 | 描述 |
|------|------|
| Success | 认证成功 |
| Failed | 认证失败 |
| Error | 认证错误（message: String） |
| Cancelled | 用户取消 |
| NotEnrolled | 未注册生物识别 |
| NotAvailable | 生物识别不可用 |

**示例：**
```kotlin
biometricAuthService.authenticate("请验证身份")
    .collect { result ->
        when (result) {
            is BiometricAuthService.AuthenticationResult.Success -> {
                // 认证成功
            }
            is BiometricAuthService.AuthenticationResult.Failed -> {
                // 认证失败
            }
            // ... 其他状态
        }
    }
```

---

## 数据库接口

### ReceiptDao（票据数据访问对象）

#### 方法

##### getAll(): Flow<List<Receipt>>

获取所有票据。

##### getById(id: Long): Flow<Receipt?>

根据 ID 获取票据。

##### insert(receipt: Receipt): Long

插入票据。

**返回：** 新插入记录的 ID

##### update(receipt: Receipt)

更新票据。

##### delete(receipt: Receipt)

删除票据。

##### searchByTitle(query: String): Flow<List<Receipt>>

根据标题搜索票据。

##### getByCategory(category: String): Flow<List<Receipt>>

根据分类获取票据。

##### getByStatus(status: ReceiptStatus): Flow<List<Receipt>>

根据状态获取票据。

##### getByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>>

根据日期范围获取票据。

##### getTotalAmountByCategory(category: String): Flow<Double>

获取指定分类的总金额。

---

### ReimbursementDao（报销单数据访问对象）

#### 方法

##### getAll(): Flow<List<Reimbursement>>

获取所有报销单。

##### getById(id: Long): Flow<Reimbursement?>

根据 ID 获取报销单。

##### insert(reimbursement: Reimbursement): Long

插入报销单。

**返回：** 新插入记录的 ID

##### update(reimbursement: Reimbursement)

更新报销单。

##### delete(reimbursement: Reimbursement)

删除报销单。

##### getByStatus(status: ReimbursementStatus): Flow<List<Reimbursement>>

根据状态获取报销单。

##### getByApplicant(applicant: String): Flow<List<Reimbursement>>

根据申请人获取报销单。

##### getByDateRange(startDate: Date, endDate: Date): Flow<List<Reimbursement>>

根据日期范围获取报销单。

---

### ClassificationRuleDao（分类规则数据访问对象）

#### 方法

##### getAll(): Flow<List<ClassificationRule>>

获取所有分类规则。

##### getEnabled(): Flow<List<ClassificationRule>>

获取所有启用的分类规则。

##### getById(id: Long): Flow<ClassificationRule?>

根据 ID 获取分类规则。

##### insert(rule: ClassificationRule): Long

插入分类规则。

**返回：** 新插入记录的 ID

##### update(rule: ClassificationRule)

更新分类规则。

##### delete(rule: ClassificationRule)

删除分类规则。

---

### OperationLogDao（操作日志数据访问对象）

#### 方法

##### getAll(): Flow<List<OperationLog>>

获取所有操作日志。

##### getById(id: Long): Flow<OperationLog?>

根据 ID 获取操作日志。

##### getByEntityType(entityType: EntityType, entityId: Long): Flow<List<OperationLog>>

根据实体类型和 ID 获取操作日志。

##### getByDateRange(startDate: Date, endDate: Date): Flow<List<OperationLog>>

根据日期范围获取操作日志。

##### insert(log: OperationLog): Long

插入操作日志。

**返回：** 新插入记录的 ID

##### deleteOldLogs(beforeDate: Date)

删除指定日期之前的日志。

---

## API 端点

### 局域网集成 API 端点

#### POST /api/receipts/sync

同步票据到服务器。

**请求体：**
```json
{
  "receipts": [
    {
      "id": 1,
      "title": "票据标题",
      "amount": 100.50,
      "date": "2024-01-01",
      "category": "餐饮"
    }
  ]
}
```

**响应：**
```json
{
  "success": true,
  "message": "同步成功",
  "syncedCount": 1
}
```

#### GET /api/reimbursements

获取报销单列表。

**查询参数：**
- `status`：状态（可选）
- `startDate`：开始日期（可选）
- `endDate`：结束日期（可选）

**响应：**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "报销单标题",
      "amount": 1000.00,
      "status": "approved"
    }
  ]
}
```

#### GET /api/health

健康检查。

**响应：**
```json
{
  "status": "ok",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 错误处理

### 错误代码

| 代码 | 描述 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 错误响应格式

```json
{
  "success": false,
  "error": {
    "code": 400,
    "message": "参数错误",
    "details": "缺少必需的参数"
  }
}
```

---

## 安全认证

### API 认证

局域网集成 API 支持以下认证方式：

1. **API Key**：在请求头中添加 `X-API-Key`
2. **Basic Auth**：使用 HTTP Basic 认证
3. **JWT Token**：使用 JWT 令牌

### 数据加密

- 传输加密：HTTPS/TLS
- 存储加密：AES-256-GCM
- 密钥派生：PBKDF2WithHmacSHA256

### 生物识别

- 支持指纹识别
- 支持面部识别
- 支持虹膜识别（设备支持）

---

## 版本信息

- **API 版本**：1.0.0
- **最后更新**：2024年1月
- **兼容性**：Android 8.0 (API 26) 或更高