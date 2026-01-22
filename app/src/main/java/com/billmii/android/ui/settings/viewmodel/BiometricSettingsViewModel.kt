package com.billmii.android.ui.settings.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.service.BiometricAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricSettingsViewModel @Inject constructor(
    private val biometricAuthService: BiometricAuthService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    data class UiState(
        val isAvailable: Boolean = false,
        val isEnabled: Boolean = false,
        val canToggle: Boolean = true,
        val biometricType: String = "生物识别",
        val statusMessage: String = "检查中...",
        val enrollPrompt: String = "",
        val testResult: TestResult? = null,
        val isLoading: Boolean = false
    )
    
    data class TestResult(
        val isSuccess: Boolean,
        val message: String
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        checkBiometricAvailability()
    }
    
    fun checkBiometricAvailability() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                statusMessage = "检查中..."
            )
            
            val canAuth = biometricAuthService.canAuthenticate()
            val biometricType = biometricAuthService.getBiometricType()
            val status = biometricAuthService.getBiometricStatus()
            val isEnrolled = biometricAuthService.isBiometricEnrolled()
            
            _uiState.value = _uiState.value.copy(
                isAvailable = canAuth,
                isEnabled = isEnrolled && canAuth,
                canToggle = canAuth,
                biometricType = biometricType,
                statusMessage = status,
                enrollPrompt = biometricAuthService.promptToEnroll(),
                isLoading = false
            )
        }
    }
    
    fun toggleBiometric(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isEnabled = enabled
        )
        
        // In a real implementation, you would save this preference
        viewModelScope.launch {
            // TODO: Save biometric preference to DataStore
        }
    }
    
    fun testAuthentication(activity: FragmentActivity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = biometricAuthService.authenticate(
                activity = activity,
                title = "测试生物识别",
                subtitle = "请使用${_uiState.value.biometricType}进行验证",
                description = "验证您的生物识别功能是否正常工作",
                negativeButtonText = "取消"
            )
            
            when (result) {
                is BiometricAuthService.AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        testResult = TestResult(
                            isSuccess = true,
                            message = "${_uiState.value.biometricType}验证成功！您的设备已正确设置生物识别功能。"
                        ),
                        isLoading = false
                    )
                }
                is BiometricAuthService.AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        testResult = TestResult(
                            isSuccess = false,
                            message = "验证失败：${result.message}"
                        ),
                        isLoading = false
                    )
                }
                is BiometricAuthService.AuthResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(
                        testResult = TestResult(
                            isSuccess = false,
                            message = "验证已取消"
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }
    
    fun getBiometricStatus(): String {
        return biometricAuthService.getBiometricStatus()
    }
    
    fun getBiometricType(): String {
        return biometricAuthService.getBiometricType()
    }
}