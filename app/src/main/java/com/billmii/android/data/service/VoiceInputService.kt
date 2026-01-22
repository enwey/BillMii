package com.billmii.android.data.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice input service using Android Speech Recognition API
 * Provides speech-to-text functionality for search and notes
 */
@Singleton
class VoiceInputService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Check if speech recognition is available on the device
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start voice recognition and return results as Flow
     * @param language Language code (e.g., "zh-CN", "en-US")
     * @param prompt Optional prompt to display to user
     * @return Flow emitting recognized text or errors
     */
    fun startVoiceRecognition(
        language: String = "zh-CN",
        prompt: String? = null
    ): Flow<VoiceInputResult> = callbackFlow {
        // Create speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        // Create recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            prompt?.let {
                putExtra(RecognizerIntent.EXTRA_PROMPT, it)
            }
        }

        // Create recognition listener
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(VoiceInputResult.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(VoiceInputResult.Listening)
            }

            override fun onRmsChanged(rmsdB: Float) {
                trySend(VoiceInputResult.Volume(rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }

            override fun onEndOfSpeech() {
                trySend(VoiceInputResult.Processing)
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                trySend(VoiceInputResult.Error(errorMessage))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val result = matches.firstOrNull() ?: ""
                    val confidence = confidences?.getOrNull(0) ?: 0f
                    trySend(VoiceInputResult.Success(result, confidence))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val result = matches.firstOrNull() ?: ""
                    trySend(VoiceInputResult.Partial(result))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        }

        // Set listener and start listening
        speechRecognizer?.setRecognitionListener(listener)
        speechRecognizer?.startListening(intent)

        // Cleanup when flow is cancelled
        awaitClose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    /**
     * Cancel ongoing voice recognition
     */
    fun cancelRecognition() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Stop voice recognition and get final results
     */
    fun stopRecognition() {
        speechRecognizer?.stopListening()
    }

    /**
     * Get human-readable error message
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限"
            SpeechRecognizer.ERROR_NETWORK -> "网络错误"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
            SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
            SpeechRecognizer.ERROR_SERVER -> "服务器错误"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时，未检测到语音输入"
            else -> "未知错误: $errorCode"
        }
    }

    /**
     * Voice input result sealed class
     */
    sealed class VoiceInputResult {
        /**
         * Speech recognizer is ready
         */
        data object Ready : VoiceInputResult()

        /**
         * Listening to speech
         */
        data object Listening : VoiceInputResult()

        /**
         * Processing speech
         */
        data object Processing : VoiceInputResult()

        /**
         * Partial result during speech recognition
         */
        data class Partial(val text: String) : VoiceInputResult()

        /**
         * Final recognition result
         */
        data class Success(val text: String, val confidence: Float) : VoiceInputResult()

        /**
         * Volume level during speech (0.0 to 1.0)
         */
        data class Volume(val level: Float) : VoiceInputResult()

        /**
         * Error occurred
         */
        data class Error(val message: String) : VoiceInputResult()
    }

    companion object {
        /**
         * Get supported languages
         */
        fun getSupportedLanguages(): List<Language> {
            return listOf(
                Language("zh-CN", "中文(简体)"),
                Language("zh-TW", "中文(繁體)"),
                Language("en-US", "English (US)"),
                Language("en-GB", "English (UK)"),
                Language("ja-JP", "日本語"),
                Language("ko-KR", "한국어"),
                Language("fr-FR", "Français"),
                Language("de-DE", "Deutsch"),
                Language("es-ES", "Español"),
                Language("it-IT", "Italiano")
            )
        }

        /**
         * Language data class
         */
        data class Language(
            val code: String,
            val displayName: String
        )
    }
}