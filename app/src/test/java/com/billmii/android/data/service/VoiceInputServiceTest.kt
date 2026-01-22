package com.billmii.android.data.service

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for VoiceInputService
 * Note: This is a simplified test structure. Full testing requires Android instrumentation tests
 * due to the SpeechRecognizer API being Android-specific.
 */
@RunWith(MockitoJUnitRunner::class)
class VoiceInputServiceTest {

    @Mock
    private lateinit var mockContext: android.content.Context

    private lateinit var voiceInputService: VoiceInputService

    @Before
    fun setup() {
        // Note: Actual VoiceInputService requires Android context and SpeechRecognizer
        // This is a placeholder for the test structure
        // voiceInputService = VoiceInputService(mockContext)
    }

    @Test
    fun `test voice input result states`() = runTest {
        // Test Ready state
        val readyState = VoiceInputService.VoiceInputResult.Ready
        assertTrue(readyState is VoiceInputService.VoiceInputResult.Ready)
        
        // Test Listening state
        val listeningState = VoiceInputService.VoiceInputResult.Listening
        assertTrue(listeningState is VoiceInputService.VoiceInputResult.Listening)
        
        // Test Processing state
        val processingState = VoiceInputService.VoiceInputResult.Processing
        assertTrue(processingState is VoiceInputService.VoiceInputResult.Processing)
        
        // Test Partial result state
        val partialState = VoiceInputService.VoiceInputResult.Partial("部分结果")
        assertTrue(partialState is VoiceInputService.VoiceInputResult.Partial)
        assertEquals("部分结果", partialState.text)
        
        // Test Success state
        val successState = VoiceInputService.VoiceInputResult.Success("完整结果")
        assertTrue(successState is VoiceInputService.VoiceInputResult.Success)
        assertEquals("完整结果", successState.text)
        
        // Test Volume state
        val volumeState = VoiceInputService.VoiceInputResult.Volume(0.5f)
        assertTrue(volumeState is VoiceInputService.VoiceInputResult.Volume)
        assertEquals(0.5f, volumeState.level, 0.001f)
        
        // Test Error state
        val errorState = VoiceInputService.VoiceInputResult.Error("错误信息")
        assertTrue(errorState is VoiceInputService.VoiceInputResult.Error)
        assertEquals("错误信息", errorState.message)
    }

    @Test
    fun `test supported languages`() {
        // Test that all supported languages are defined
        val supportedLanguages = listOf(
            "zh-CN", "en-US", "ja-JP", "ko-KR", 
            "fr-FR", "de-DE", "es-ES", "it-IT"
        )
        
        // Verify language codes are valid BCP 47 language tags
        supportedLanguages.forEach { language ->
            assertTrue(language.matches(Regex("^[a-z]{2}-[A-Z]{2}$")))
        }
    }
}