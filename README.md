# 票小秘（BillMii）- Android端票据文件整理分类程序

## 项目简介

票小秘（BillMii）是一款面向Android端的票据文件整理分类程序，专注于本地OCR识别、智能分类归档和财会报销管理。所有数据均在本地处理，确保财务数据安全，无需云端传输。

## 核心特性

- ✅ 本地优先：OCR识别、数据存储、流程处理全环节本地完成
- ✅ 财会适配：贴合票据审核、费用归集、报销合规校验等财会流程
- ✅ 精准高效：基于PaddleOCR的本地识别，支持批量处理
- ✅ 数据安全：SQLCipher加密数据库，支持生物识别解锁
- ✅ 移动端优化：适配触控操作，支持相机直采和文件导入

## 技术栈

### 核心技术
- **语言**: Kotlin
- **最低SDK**: Android 10 (API 29)
- **目标SDK**: Android 14 (API 34)
- **架构**: MVVM + Clean Architecture
- **UI框架**: Jetpack Compose + Material Design 3
- **依赖注入**: Hilt

### 主要依赖
- **数据库**: Room + SQLCipher（加密）
- **OCR**: PaddleOCR PP-OCRv5 Mobile
- **图像处理**: OpenCV
- **相机**: CameraX
- **导航**: Jetpack Navigation Compose
- **异步处理**: Kotlin Coroutines + Flow
- **状态管理**: StateFlow

## 项目结构

```
app/
├── src/main/java/com/billmii/android/
│   ├── BillMiiApplication.kt              # 应用入口
│   ├── data/
│   │   ├── model/                          # 数据模型
│   │   │   ├── ReceiptType.kt              # 票据类型枚举
│   │   │   ├── Receipt.kt                  # 票据实体
│   │   │   ├── Reimbursement.kt           # 报销单实体
│   │   │   ├── ClassificationRule.kt       # 分类规则实体
│   │   │   └── OperationLog.kt            # 操作日志实体
│   │   ├── database/
│   │   │   ├── BillMiiDatabase.kt         # 数据库配置
│   │   │   ├── converter/                   # 类型转换器
│   │   │   └── dao/                        # 数据访问对象
│   │   │       ├── ReceiptDao.kt
│   │   │       ├── ReimbursementDao.kt
│   │   │       ├── ClassificationRuleDao.kt
│   │   │       └── OperationLogDao.kt
│   │   ├── repository/                     # 数据仓库
│   │   │   ├── ReceiptRepository.kt
│   │   │   └── ReimbursementRepository.kt
│   │   └── service/                       # 业务服务
│   │       ├── FileStorageService.kt        # 文件存储服务
│   │       ├── CameraManager.kt            # 相机管理
│   │       └── OcrService.kt              # OCR识别服务
│   └── ui/
│       ├── MainActivity.kt                   # 主界面
│       ├── navigation/                       # 导航配置
│       │   └── Screen.kt
│       ├── theme/                           # 主题配置
│       │   ├── Theme.kt
│       │   └── Type.kt
│       ├── receipt/                         # 票据界面
│       │   ├── ReceiptListScreen.kt
│       │   ├── ReceiptDetailScreen.kt
│       │   └── viewmodel/
│       ├── reimbursement/                    # 报销界面
│       │   ├── ReimbursementListScreen.kt
│       │   ├── ReimbursementDetailScreen.kt
│       │   └── viewmodel/
│       ├── camera/                          # 相机界面
│       │   └── CameraActivity.kt
│       └── settings/                        # 设置界面
│           ├── SettingsScreen.kt
│           └── viewmodel/
└── build.gradle.kts                          # 应用级构建配置
```

## 已实现功能

### ✅ 基础架构
- [x] 项目结构搭建
- [x] Gradle配置和依赖管理
- [x] AndroidManifest配置
- [x] Material Design 3主题
- [x] 应用入口类

### ✅ 数据层
- [x] 数据模型定义（票据、报销单、分类规则、操作日志）
- [x] Room数据库配置（SQLCipher加密）
- [x] 类型转换器（枚举、日期、JSON等）
- [x] DAO接口（完整的CRUD操作）
- [x] 数据仓库模式

### ✅ 服务层
- [x] 文件存储服务（文件管理、哈希计算）
- [x] 相机管理服务（CameraX集成、图像预处理）
- [x] OCR识别服务（PaddleOCR集成框架）

### ✅ 票据管理模块
- [x] 票据列表界面（搜索、筛选）
- [x] 票据详情界面（信息展示、编辑）
- [x] 票据仓库（导入、删除、OCR更新）
- [x] 票据ViewModel（状态管理）

### ✅ 报销管理模块
- [x] 报销单列表界面
- [x] 报销单详情界面
- [x] 报销单仓库
- [x] 报销单ViewModel

### ✅ 相机模块
- [x] 相机预览界面
- [x] 拍照功能
- [x] 图像预处理（OpenCV）

### ✅ 设置模块
- [x] 设置界面
- [x] OCR模式选择
- [x] 备份设置
- [x] 安全设置

## 待实现功能

### 📋 模块二：本地OCR识别（部分完成）
- [ ] PaddleOCR模型文件集成
- [ ] 实际OCR识别逻辑（当前为占位符）
- [ ] 识别结果优化和校验
- [ ] 批量识别优化
- [ ] OCR模板自定义

### 📋 模块三：智能分类与归档
- [ ] 分类规则引擎实现
- [ ] 自动分类逻辑
- [ ] 归档编号生成
- [ ] 分类规则管理界面
- [ ] 归档路径管理

### 📋 模块四：报销管理
- [ ] 报销单创建界面
- [ ] 票据关联报销单
- [ ] 报销合规校验
- [ ] 审批流程实现
- [ ] 审批通知
- [ ] 报销凭证生成

### 📋 模块五：账务对接与数据导出
- [ ] Excel导出功能
- [ ] PDF导出功能
- [ ] 自定义导出模板
- [ ] 局域网财务软件对接
- [ ] 数据格式转换

### 📋 模块六：数据安全与备份
- [ ] 自动备份实现
- [ ] 手动备份功能
- [ ] 备份恢复功能
- [ ] 备份文件加密
- [ ] 操作日志记录
- [ ] 生物识别集成

### 📋 模块七：辅助功能
- [ ] 统计分析界面
- [ ] 可视化图表（饼图、柱状图）
- [ ] 批量操作工具
- [ ] 高级搜索（多维度）
- [ ] 语音输入

### 📋 UI组件
- [ ] 图标资源（Material Icons）
- [ ] 图片加载（Coil集成）
- [ ] 对话框组件
- [ ] 加载指示器
- [ ] 空状态视图
- [ ] 错误提示

### 📋 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] UI测试

### 📋 文档
- [ ] API文档
- [ ] 用户手册
- [ ] 开发文档
- [ ] 部署指南

## 开发计划

### 第一阶段（已完成）
- ✅ 项目架构搭建
- ✅ 数据模型设计
- ✅ 数据库实现
- ✅ 基础UI框架

### 第二阶段（进行中）
- 🔄 OCR服务集成
- 🔄 票据采集完善
- 🔄 基础UI实现

### 第三阶段（待开始）
- ⏳ 智能分类实现
- ⏳ 报销流程完善
- ⏳ 数据导出功能

### 第四阶段（待开始）
- ⏳ 安全功能完善
- ⏳ 备份恢复功能
- ⏳ 统计分析功能

## 快速开始

### 环境要求
- Android Studio Arctic Fox (2022.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 构建步骤
1. 克隆项目
```bash
git clone https://github.com/your-repo/BillMii.git
cd BillMii
```

2. 同步Gradle
```bash
./gradlew build
```

3. 运行应用
```bash
./gradlew installDebug
```

## 配置说明

### OCR模型配置
将PaddleOCR模型文件放置在以下目录：
```
app/src/main/assets/models/
```

模型文件：
- det_infer_ch_PP-OCRv3_det_infer.tar
- rec_infer_ch_PP-OCRv3_rec_infer.tar
- cls_infer_ch_PP-OCRv3_cls_infer.tar

### 数据库加密
默认数据库密码在 `BillMiiDatabase.kt` 中配置，生产环境应使用安全存储：
```kotlin
private const val DATABASE_PASSPHRASE = "BillMiiSecureKey2024!"
```

## 注意事项

1. **权限管理**：应用需要相机、存储等权限，已在AndroidManifest中声明
2. **OCR模型**：需要下载PaddleOCR模型文件并放置在assets目录
3. **加密数据库**：首次运行会创建加密数据库，请妥善保管密码
4. **内存优化**：大图片处理时注意内存使用，建议压缩后处理

## 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- 项目主页: [GitHub](https://github.com/your-repo/BillMii)
- 问题反馈: [Issues](https://github.com/your-repo/BillMii/issues)
- 邮箱: support@billmii.com

## 致谢

- PaddleOCR团队提供的OCR引擎
- Android团队提供的Jetpack组件
- 开源社区的贡献者

---

**票小秘（BillMii）** - 让票据管理更简单！