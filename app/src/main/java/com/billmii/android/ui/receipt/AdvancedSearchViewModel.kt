package com.billmii.android.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.model.ReceiptType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Advanced Search
 */
@HiltViewModel
class AdvancedSearchViewModel @Inject constructor(
    private val receiptDao: ReceiptDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AdvancedSearchUiState())
    val uiState: StateFlow<AdvancedSearchUiState> = _uiState.asStateFlow()
    
    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()
    
    private val searchQueryFlow = MutableStateFlow("")
    private val filtersFlow = MutableStateFlow<List<SearchFilter>>(emptyList())
    
    init {
        // Combine search query and filters
        combine(searchQueryFlow, filtersFlow) { query, filters ->
            Pair(query, filters)
        }.debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { (query, filters) ->
                performSearch(query, filters)
            }
            .onEach { results ->
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
            .launchIn(viewModelScope)
    }
    
    private fun performSearch(query: String, filters: List<SearchFilter>): Flow<List<ReceiptSummary>> {
        return receiptDao.getAllReceipts()
            .map { receipts ->
                receipts.filter { receipt ->
                    // Apply text search
                    val matchesQuery = query.isEmpty() || 
                        (receipt.invoiceNumber?.contains(query, ignoreCase = true) == true) ||
                        (receipt.buyerName?.contains(query, ignoreCase = true) == true) ||
                        (receipt.sellerName?.contains(query, ignoreCase = true) == true) ||
                        (receipt.fileName?.contains(query, ignoreCase = true) == true)
                    
                    // Apply filters
                    val matchesFilters = filters.all { filter ->
                        when (filter) {
                            is SearchFilter.FilterByType -> receipt.receiptType == filter.type
                            is SearchFilter.FilterByStartDate -> {
                                val invoiceDate = receipt.invoiceDate ?: receipt.createdAt
                                invoiceDate >= filter.date
                            }
                            is SearchFilter.FilterByEndDate -> {
                                val invoiceDate = receipt.invoiceDate ?: receipt.createdAt
                                invoiceDate <= filter.date
                            }
                            is SearchFilter.FilterByMinAmount -> {
                                val amount = receipt.totalAmount ?: receipt.amount ?: 0.0
                                amount >= filter.amount
                            }
                            is SearchFilter.FilterByMaxAmount -> {
                                val amount = receipt.totalAmount ?: receipt.amount ?: 0.0
                                amount <= filter.amount
                            }
                        }
                    }
                    
                    matchesQuery && matchesFilters
                }.map { it.toSummary() }
            }
    }
    
    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun clearSearch() {
        updateSearchQuery("")
    }
    
    fun showFilterDialog() {
        _showFilterDialog.value = true
    }
    
    fun hideFilterDialog() {
        _showFilterDialog.value = false
    }
    
    fun applyFilters(filters: List<SearchFilter>) {
        filtersFlow.value = filters
        _uiState.value = _uiState.value.copy(activeFilters = filters)
    }
    
    fun removeFilter(filter: SearchFilter) {
        val updated = _uiState.value.activeFilters.toMutableList()
        updated.remove(filter)
        applyFilters(updated)
    }
    
    fun clearFilters() {
        applyFilters(emptyList())
    }
}

// UI State
data class AdvancedSearchUiState(
    val searchQuery: String = "",
    val searchResults: List<ReceiptSummary> = emptyList(),
    val activeFilters: List<SearchFilter> = emptyList(),
    val isLoading: Boolean = false
)

// Extension function
private fun com.billmii.android.data.model.Receipt.toSummary(): ReceiptSummary {
    return ReceiptSummary(
        id = id,
        receiptType = receiptType,
        sellerName = sellerName,
        amount = totalAmount ?: amount ?: 0.0,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate
    )
}