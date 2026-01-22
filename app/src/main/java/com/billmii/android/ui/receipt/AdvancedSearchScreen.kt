package com.billmii.android.ui.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced Search Screen
 * Allows users to search receipts with multiple filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchScreen(
    onBack: () -> Unit,
    onReceiptClick: (Long) -> Unit,
    viewModel: AdvancedSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showFilterDialog by viewModel.showFilterDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("高级搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showFilterDialog() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            FilterSummaryBar(
                filters = uiState.activeFilters,
                onRemoveFilter = { viewModel.removeFilter(it) },
                onClearAll = { viewModel.clearFilters() }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.searchResults.isEmpty() && uiState.searchQuery.isEmpty()) {
            EmptySearchView(modifier = Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("搜索发票号、销售方、购买方...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true
                )
                
                // Results
                if (uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                    NoResultsView(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.searchResults) { receipt ->
                            SearchResultItem(
                                receipt = receipt,
                                onClick = { onReceiptClick(receipt.id) }
                            )
                        }
                    }
                }
                
                // Result count
                if (uiState.searchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = "找到 ${uiState.searchResults.size} 条结果",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            filters = uiState.activeFilters,
            onDismiss = { viewModel.hideFilterDialog() },
            onApply = { newFilters ->
                viewModel.applyFilters(newFilters)
            }
        )
    }
}

@Composable
fun SearchResultItem(
    receipt: ReceiptSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.receiptType.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = receipt.sellerName ?: "未知销售方",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                receipt.invoiceNumber?.let {
                    Text(
                        text = "发票号: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "¥${formatAmount(receipt.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            receipt.invoiceDate?.let { date ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FilterSummaryBar(
    filters: List<SearchFilter>,
    onRemoveFilter: (SearchFilter) -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (filters.isEmpty()) {
                Text(
                    text = "无筛选条件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filters.forEach { filter ->
                    FilterChip(
                        selected = true,
                        onClick = { onRemoveFilter(filter) },
                        label = { Text(filter.displayName) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
                        }
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(onClick = onClearAll) {
                    Text("清除全部")
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    filters: List<SearchFilter>,
    onDismiss: () -> Unit,
    onApply: (List<SearchFilter>) -> Unit
) {
    var selectedType by remember { mutableStateOf<ReceiptType?>(null) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选条件") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Receipt Type Filter
                Text(
                    text = "票据类型",
                    style = MaterialTheme.typography.titleSmall
                )
                var showTypeDropdown by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showTypeDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedType?.name ?: "全部类型")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    
                    if (showTypeDropdown) {
                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部类型") },
                                onClick = {
                                    selectedType = null
                                    showTypeDropdown = false
                                }
                            )
                            ReceiptType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        selectedType = type
                                        showTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Date Range Filter
                Text(
                    text = "日期范围",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate?.let { formatDate(it) } ?: "",
                        onValueChange = {},
                        label = { Text("开始日期") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { /* TODO: Show date picker */ }) {
                                Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = endDate?.let { formatDate(it) } ?: "",
                        onValueChange = {},
                        label = { Text("结束日期") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { /* TODO: Show date picker */ }) {
                                Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                            }
                        }
                    )
                }
                
                // Amount Range Filter
                Text(
                    text = "金额范围",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmount,
                        onValueChange = { minAmount = it },
                        label = { Text("最小金额") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmount,
                        onValueChange = { maxAmount = it },
                        label = { Text("最大金额") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newFilters = mutableListOf<SearchFilter>()
                    selectedType?.let {
                        newFilters.add(SearchFilter.FilterByType(it))
                    }
                    startDate?.let {
                        newFilters.add(SearchFilter.FilterByStartDate(it))
                    }
                    endDate?.let {
                        newFilters.add(SearchFilter.FilterByEndDate(it))
                    }
                    minAmount.toDoubleOrNull()?.let {
                        newFilters.add(SearchFilter.FilterByMinAmount(it))
                    }
                    maxAmount.toDoubleOrNull()?.let {
                        newFilters.add(SearchFilter.FilterByMaxAmount(it))
                    }
                    onApply(newFilters)
                }
            ) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EmptySearchView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "开始搜索",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "输入关键词或添加筛选条件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoResultsView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "未找到结果",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "请尝试其他搜索条件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Search filter types
sealed class SearchFilter(val displayName: String) {
    data class FilterByType(val type: ReceiptType) : SearchFilter("类型: ${type.name}")
    data class FilterByStartDate(val date: Date) : SearchFilter("开始: ${formatDate(date)}")
    data class FilterByEndDate(val date: Date) : SearchFilter("结束: ${formatDate(date)}")
    data class FilterByMinAmount(val amount: Double) : SearchFilter("最小: ¥$amount")
    data class FilterByMaxAmount(val amount: Double) : SearchFilter("最大: ¥$amount")
}

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(date)
}

fun formatAmount(amount: Double): String {
    return String.format("%.2f", amount)
}