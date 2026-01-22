package com.billmii.android.ui.reimbursement.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.repository.ReimbursementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reimbursement Detail ViewModel
 * Manages reimbursement detail state
 */
@HiltViewModel
class ReimbursementDetailViewModel @Inject constructor(
    private val reimbursementRepository: ReimbursementRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val reimbursementId: Long = savedStateHandle.get<Long>("reimbursementId") ?: 0L
    
    private val _reimbursement = MutableStateFlow<Reimbursement?>(null)
    val reimbursement: StateFlow<Reimbursement?> = _reimbursement.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        if (reimbursementId > 0) {
            loadReimbursement(reimbursementId)
        }
    }
    
    fun loadReimbursement(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loaded = reimbursementRepository.getById(id)
                _reimbursement.value = loaded
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}