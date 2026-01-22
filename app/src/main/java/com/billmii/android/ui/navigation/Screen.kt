package com.billmii.android.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Screen routes and navigation definitions
 * Defines all navigation destinations in the app
 */
sealed class Screen(
    val route: String,
    val title: String? = null,
    val arguments: List<androidx.navigation.NavArgument> = emptyList()
) {
    
    /**
     * Create route with parameters
     */
    fun createRoute(vararg params: Any): String {
        return buildString {
            append(route)
            params.forEach { param ->
                append("/$param")
            }
        }
    }
    
    // Receipt screens - 票据界面
    object ReceiptList : Screen("receipt_list", "票据")
    
    object ReceiptDetail : Screen(
        route = "receipt_detail/{receiptId}",
        title = "票据详情",
        arguments = listOf(
            navArgument("receiptId") { type = NavType.LongType }
        )
    )
    
    object BatchOperations : Screen("batch_operations", "批量操作")
    
    object AdvancedSearch : Screen("advanced_search", "高级搜索")
    
    // Reimbursement screens - 报销界面
    object ReimbursementList : Screen("reimbursement_list", "报销")
    
    object ReimbursementDetail : Screen(
        route = "reimbursement_detail/{reimbursementId}",
        title = "报销详情",
        arguments = listOf(
            navArgument("reimbursementId") { type = NavType.LongType }
        )
    )
    
    object CreateReimbursement : Screen("create_reimbursement", "创建报销")
    
    object ApprovalWorkflow : Screen("approval_workflow", "审批工作流")
    
    object ComplianceValidation : Screen("compliance_validation", "合规性检查")
    
    // OCR screens - OCR界面
    object OcrTemplates : Screen("ocr_templates", "OCR模板")
    
    // Classification screens - 分类界面
    object ClassificationRules : Screen("classification_rules", "分类规则")
    
    // Statistics screen - 统计界面
    object Statistics : Screen("statistics", "统计")
    
    // Settings screens - 设置界面
    object Settings : Screen("settings", "设置")
    
    object BackupRestore : Screen("backup_restore", "备份与恢复")
    
    object ArchivePathManagement : Screen("archive_path_management", "归档路径管理")
    
    object OperationLog : Screen("operation_log", "操作日志")
    
    object LanIntegration : Screen("lan_integration", "局域网集成")
    
    object BiometricSettings : Screen("biometric_settings", "生物识别设置")
    
    // Export screens - 导出界面
    object Export : Screen("export", "数据导出")
    
    // Camera screen - 相机界面
    object Camera : Screen("camera")
    
    // QR Code Scanner screen - 二维码扫描界面
    object QrCodeScanner : Screen("qr_code_scanner", "扫码")
}