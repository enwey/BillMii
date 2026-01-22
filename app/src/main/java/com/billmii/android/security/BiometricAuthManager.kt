package com.billmii.android.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Biometric Authentication Manager
 * Handles fingerprint and face authentication
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    private val context: Context
) {
    
    /**
     * Check if biometric authentication is available
     */
    fun canAuthenticate(): BiometricResult {
        val biometricManager = BiometricManager.from(context)
        
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                return BiometricResult.Success
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                return BiometricResult.HardwareNotAvailable
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                return BiometricResult.HardwareUnavailable
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                return BiometricResult.NoBiometricEnrolled
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                return BiometricResult.SecurityUpdateRequired
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                return BiometricResult.Unsupported
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                return BiometricResult.UnknownError
            }
            else -> {
                return BiometricResult.UnknownError
            }
        }
    }
    
    /**
     * Perform biometric authentication
     * @param activity The fragment activity
     * @param title Title for the biometric prompt
     * @param subtitle Subtitle for the biometric prompt
     * @param description Description for the biometric prompt
     * @param negativeButtonText Text for the negative button (cancel)
     * @return BiometricAuthResult indicating success or failure
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "身份验证",
        subtitle: String = "请使用指纹或面容进行验证",
        description: String = "验证以访问应用",
        negativeButtonText: String = "取消"
    ): BiometricAuthResult {
        return suspendCancellableCoroutine { continuation ->
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(BiometricAuthResult.Success)
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            BiometricAuthenticationException(errorCode, errString.toString())
                        )
                    }
                }
                
                override fun onAuthenticationFailed() {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            BiometricAuthenticationException(
                                BiometricPrompt.ERROR_CANCELED,
                                "Authentication failed"
                            )
                        )
                    }
                }
            }
            
            val biometricPrompt = BiometricPrompt(
                activity,
                context.mainExecutor,
                callback
            )
            
            biometricPrompt.authenticate(promptInfo)
            
            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }
}

/**
 * Biometric result types
 */
sealed class BiometricResult {
    object Success : BiometricResult()
    object HardwareNotAvailable : BiometricResult()
    object HardwareUnavailable : BiometricResult()
    object NoBiometricEnrolled : BiometricResult()
    object SecurityUpdateRequired : BiometricResult()
    object Unsupported : BiometricResult()
    object UnknownError : BiometricResult()
}

/**
 * Biometric authentication result
 */
sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Failed(val error: String) : BiometricAuthResult()
}

/**
 * Biometric authentication exception
 */
class BiometricAuthenticationException(
    val errorCode: Int,
    message: String
) : Exception(message) {
    val isUserCancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                          errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
}