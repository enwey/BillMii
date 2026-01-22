package com.billmii.android.ui.receipt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.OcrStatus
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.ReceiptCategory
import com.billmii.android.data.model.ValidationStatus
import com.billmii.android.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receipt List ViewModel
 * Manages receipt list state and operations
 */
@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(ReceiptFilter.ALL)
    val selectedFilter: StateFlow<ReceiptFilter> = _selectedFilter.asStateFlow()
    
    // Combined receipts flow with filtering
    val receipts: StateFlow<List<Receipt>> = combine(
        receiptRepository.getAllReceipts(),
        _searchQuery,
        _selectedFilter
    ) { allReceipts, query, filter ->
        filterReceipts(allReceipts, query, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        loadReceipts()
    }
    
    /**
     * Load all receipts
     */
    private fun loadReceipts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Receipts are loaded via flow
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Update filter
     */
    fun updateFilter(filter: ReceiptFilter) {
        _selectedFilter.value = filter
    }
    
    /**
     * Delete receipt
     */
    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            try {
                receiptRepository.deleteReceipt(receipt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Delete multiple receipts
     */
    fun deleteReceipts(receiptIds: List<Long>) {
        viewModelScope.launch {
            try {
                receiptRepository.deleteReceipts(receiptIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Filter receipts based on query and filter type
     */
    private fun filterReceipts(
        receipts: List<Receipt>,
        query: String,
        filter: ReceiptFilter
    ): List<Receipt> {
        var filtered = receipts
        
        // Apply type filter
        filtered = when (filter) {
            ReceiptFilter.ALL -> filtered
            ReceiptFilter.PENDING -> filtered.filter { it.ocrStatus == OcrStatus.PENDING }
            ReceiptFilter.PROCESSED -> filtered.filter { it.ocrStatus == OcrStatus.SUCCESS }
            ReceiptFilter.INVOICE -> filtered.filter { it.receiptCategory == ReceiptCategory.INVOICE }
            ReceiptFilter.EXPENSE -> filtered.filter { it.receiptCategory == ReceiptCategory.EXPENSE }
        }
        
        // Apply search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter { receipt ->
                receipt.fileName.lowercase().contains(lowerQuery) ||
                receipt.invoiceNumber?.lowercase()?.contains(lowerQuery) == true ||
                receipt.buyerName?.lowercase()?.contains(lowerQuery) == true ||
                receipt.sellerName?.lowercase()?.contains(lowerQuery) == true
            }
        }
        
        return filtered
    }
}

/**
 * Receipt Filter enumeration
 */
enum class ReceiptFilter(val displayName: String) {
    ALL("全部"),
    PENDING("待处理"),
    PROCESSED("已处理"),
    INVOICE("发票"),
    EXPENSE("费用")
}