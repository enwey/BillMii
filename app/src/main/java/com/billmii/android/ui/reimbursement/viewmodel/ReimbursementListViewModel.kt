package com.billmii.android.ui.reimbursement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.repository.ReimbursementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reimbursement List ViewModel
 * Manages reimbursement list state
 */
@HiltViewModel
class ReimbursementListViewModel @Inject constructor(
    private val reimbursementRepository: ReimbursementRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    val reimbursements = reimbursementRepository.getAllReimbursements()
    
    init {
        loadReimbursements()
    }
    
    private fun loadReimbursements() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Reimbursements are loaded via flow
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}