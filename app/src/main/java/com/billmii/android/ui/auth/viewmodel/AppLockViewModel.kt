package com.billmii.android.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App Lock ViewModel
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {
    
    private val _biometricResult = MutableStateFlow<BiometricAuthManager.BiometricAuthResult?>(null)
    val biometricResult: StateFlow<BiometricAuthManager.BiometricAuthResult?> = _biometricResult.asStateFlow()
    
    private val _authInProgress = MutableStateFlow(false)
    val authInProgress: StateFlow<Boolean> = _authInProgress.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Check if biometric authentication is available
     */
    fun checkBiometricAvailability(): BiometricAuthManager.BiometricResult {
        return biometricAuthManager.canAuthenticate()
    }
    
    /**
     * Perform biometric authentication
     */
    fun authenticate(activity: androidx.fragment.app.FragmentActivity) {
        viewModelScope.launch {
            _authInProgress.value = true
            _errorMessage.value = null
            
            try {
                val result = biometricAuthManager.authenticate(
                    activity = activity,
                    title = "BillMii 身份验证",
                    subtitle = "请使用指纹或面容验证",
                    description = "验证以访问应用",
                    negativeButtonText = "取消"
                )
                _biometricResult.value = result
            } catch (e: BiometricAuthManager.BiometricAuthenticationException) {
                if (!e.isUserCancelled) {
                    _errorMessage.value = e.message ?: "验证失败"
                }
                _biometricResult.value = BiometricAuthManager.BiometricAuthResult.Failed(e.message ?: "验证失败")
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "验证失败"
                _biometricResult.value = BiometricAuthManager.BiometricAuthResult.Failed(e.message ?: "验证失败")
            } finally {
                _authInProgress.value = false
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}