package com.billmii.android.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Screen routes and navigation definitions
 * Defines all navigation destinations in the app
 */
sealed class Screen(
    val route: String,
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
        arguments = listOf(
            navArgument("receiptId") { type = NavType.LongType }
        )
    )
    
    // Reimbursement screens - 报销界面
    object ReimbursementList : Screen("reimbursement_list", "报销")
    
    object ReimbursementDetail : Screen(
        route = "reimbursement_detail/{reimbursementId}",
        arguments = listOf(
            navArgument("reimbursementId") { type = NavType.LongType }
        )
    )
    
    object CreateReimbursement : Screen("create_reimbursement")
    
    // Camera screen - 相机界面
    object Camera : Screen("camera")
    
    // Statistics screen - 统计界面
    object Statistics : Screen("statistics", "统计")
    
    // Settings screen - 设置界面
    object Settings : Screen("settings", "设置")
    
    // Archive screen - 归档界面
    object Archive : Screen("archive")
    
    // Search screen - 搜索界面
    object Search : Screen("search")
    
    // Classification rules screen - 分类规则界面
    object ClassificationRules : Screen("classification_rules")
    
    object CreateClassificationRule : Screen("create_classification_rule")
    
    // Backup screen - 备份界面
    object Backup : Screen("backup")
    
    // Export screen - 导出界面
    object Export : Screen("export")
}