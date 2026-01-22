package com.billmii.android.data.service

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 生物识别认证服务
 * 提供指纹、面容等生物识别认证功能
 */
@Singleton
class BiometricAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * 认证结果
     */
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
        object Cancelled : AuthResult()
    }
    
    /**
     * 检查设备是否支持生物识别
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false
        }
    }
    
    /**
     * 获取生物识别类型描述
     */
    fun getBiometricType(): String {
        val biometricManager = BiometricManager.from(context)
        return when {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) -> "指纹识别"
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FACE) -> "面容识别"
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS) -> "虹膜识别"
            else -> "生物识别"
        }
    }
    
    /**
     * 执行生物识别认证（异步）
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "生物识别验证",
        subtitle: String = "请使用生物识别进行验证",
        description: String = "验证失败时可使用备用密码",
        negativeButtonText: String = "取消"
    ): AuthResult = suspendCancellableCoroutine { continuation ->
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (continuation.isActive) {
                    continuation.resume(AuthResult.Success)
                }
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (continuation.isActive) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            continuation.resume(AuthResult.Cancelled)
                        }
                        else -> {
                            continuation.resume(AuthResult.Error(errString.toString()))
                        }
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // 不取消协程，允许用户重试
            }
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(AuthResult.Error(e.message ?: "认证失败"))
            }
        }
        
        continuation.invokeOnCancellation {
            // 协程取消时的清理工作
        }
    }
    
    /**
     * 执行生物识别认证（带回调）
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {},
        title: String = "生物识别验证",
        subtitle: String = "请使用生物识别进行验证",
        description: String = "验证失败时可使用备用密码",
        negativeButtonText: String = "取消"
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onCancel()
                    }
                    else -> {
                        onError(errString.toString())
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                // 认证失败，允许用户重试
            }
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * 检查是否已注册生物识别
     */
    fun isBiometricEnrolled(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            else -> false
        }
    }
    
    /**
     * 获取生物识别状态描述
     */
    fun getBiometricStatus(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "可用"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "设备不支持"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "硬件不可用"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "未设置"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "需要安全更新"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "不支持"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "状态未知"
        }
    }
    
    /**
     * 建议用户设置生物识别
     */
    fun promptToEnroll(): String {
        val biometricType = getBiometricType()
        return "请在系统设置中启用$biometricType以使用此功能"
    }
}