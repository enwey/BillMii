package com.billmii.android.ui.statistics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.ReceiptType
import com.billmii.android.data.model.ReimbursementStatus
import com.billmii.android.data.model.ReceiptCategory
import com.billmii.android.data.repository.ReceiptRepository
import com.billmii.android.data.repository.ReimbursementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Statistics ViewModel
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val reimbursementRepository: ReimbursementRepository
) : ViewModel() {
    
    private val _statistics = MutableStateFlow<StatisticsData?>(null)
    val statistics: StateFlow<StatisticsData?> = _statistics.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    init {
        loadStatistics()
    }
    
    fun refreshStatistics() {
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val allReceipts = receiptRepository.getAllReceipts()
                val allReimbursements = reimbursementRepository.getAllReimbursements()
                
                val receiptTypeStats = mutableMapOf<String, Int>()
                val receiptCategoryStats = mutableMapOf<String, Int>()
                var totalAmount = 0.0
                var pendingCount = 0
                
                allReceipts.forEach { receipt ->
                    receiptTypeStats[receipt.receiptType.displayName] = 
                        (receiptTypeStats[receipt.receiptType.displayName] ?: 0) + 1
                    receiptCategoryStats[receipt.category.displayName] = 
                        (receiptCategoryStats[receipt.category.displayName] ?: 0) + 1
                    
                    if (receipt.amount > 0) {
                        totalAmount += receipt.amount
                    }
                    
                    if (receipt.validationStatus.name == "PENDING" || 
                        receipt.ocrStatus.name == "PENDING") {
                        pendingCount++
                    }
                }
                
                val reimbursementStatusStats = mutableMapOf<String, Int>()
                var approvedAmount = 0.0
                var pendingAmount = 0.0
                
                allReimbursements.forEach { reimbursement ->
                    reimbursementStatusStats[reimbursement.status.displayName] = 
                        (reimbursementStatusStats[reimbursement.status.displayName] ?: 0) + 1
                    
                    when (reimbursement.status) {
                        ReimbursementStatus.APPROVED -> approvedAmount += reimbursement.totalAmount
                        ReimbursementStatus.SUBMITTED -> pendingAmount += reimbursement.totalAmount
                        else -> {}
                    }
                }
                
                val recentActivity = generateRecentActivity(allReceipts, allReimbursements)
                
                _statistics.value = StatisticsData(
                    totalReceipts = allReceipts.size,
                    totalReimbursements = allReimbursements.size,
                    totalAmount = totalAmount,
                    pendingCount = pendingCount,
                    receiptTypeStats = receiptTypeStats,
                    receiptCategoryStats = receiptCategoryStats,
                    reimbursementStatusStats = reimbursementStatusStats,
                    approvedAmount = approvedAmount,
                    pendingAmount = pendingAmount,
                    recentActivity = recentActivity
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun generateRecentActivity(
        receipts: List<com.billmii.android.data.model.Receipt>,
        reimbursements: List<com.billmii.android.data.model.Reimbursement>
    ): List<RecentActivity> {
        val activities = mutableListOf<RecentActivity>()
        
        // Add recent receipts
        receipts.takeLast(5).forEach { receipt ->
            activities.add(
                RecentActivity(
                    type = "receipt",
                    description = "新增票据: ${receipt.fileName}",
                    timestamp = receipt.createdAt?.let { dateFormat.format(it) } ?: ""
                )
            )
        }
        
        // Add recent reimbursements
        reimbursements.takeLast(5).forEach { reimbursement ->
            activities.add(
                RecentActivity(
                    type = "reimbursement",
                    description = "创建报销单: ${reimbursement.title}",
                    timestamp = reimbursement.createdAt?.let { dateFormat.format(it) } ?: ""
                )
            )
            
            if (reimbursement.approvalDate != null) {
                activities.add(
                    RecentActivity(
                        type = "approval",
                        description = "报销单${reimbursement.status.displayName}: ${reimbursement.title}",
                        timestamp = dateFormat.format(reimbursement.approvalDate)
                    )
                )
            }
        }
        
        // Sort by timestamp (most recent first)
        return activities.sortedByDescending { it.timestamp }.take(10)
    }
}

/**
 * Statistics Data
 */
data class StatisticsData(
    val totalReceipts: Int,
    val totalReimbursements: Int,
    val totalAmount: Double,
    val pendingCount: Int,
    val receiptTypeStats: Map<String, Int>,
    val receiptCategoryStats: Map<String, Int>,
    val reimbursementStatusStats: Map<String, Int>,
    val approvedAmount: Double,
    val pendingAmount: Double,
    val recentActivity: List<RecentActivity>
)

/**
 * Recent Activity
 */
data class RecentActivity(
    val type: String,
    val description: String,
    val timestamp: String
)