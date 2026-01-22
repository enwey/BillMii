package com.billmii.android.ui.reimbursement

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billmii.android.ui.reimbursement.viewmodel.ReimbursementListViewModel

/**
 * Reimbursement List Screen - 报销单列表界面
 * Displays all reimbursement applications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimbursementListScreen(
    paddingValues: PaddingValues,
    onReimbursementClick: (Long) -> Unit,
    viewModel: ReimbursementListViewModel = hiltViewModel()
) {
    val reimbursements by viewModel.reimbursements.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报销管理") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Navigate to create reimbursement */ }
            ) {
                Text("新建")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (reimbursements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("暂无报销单")
                }
            } else {
                // TODO: Implement reimbursement list
                Text("报销单列表（待实现）")
            }
        }
    }
}