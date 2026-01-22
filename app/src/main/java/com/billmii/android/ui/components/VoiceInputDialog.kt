package com.billmii.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.billmii.android.data.service.VoiceInputService
import kotlinx.coroutines.launch

/**
 * Voice input dialog with animated waveform
 * Provides speech-to-text interface for search and notes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    voiceInputService: VoiceInputService,
    modifier: Modifier = Modifier,
    prompt: String = "请开始说话..."
) {
    val scope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var volume by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Animation for listening indicator
    val infiniteTransition = rememberInfiniteTransition(label = "listeningAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Start/stop voice recognition
    val startListening = {
        scope.launch {
            voiceInputService.startVoiceRecognition(prompt = prompt).collect { result ->
                when (result) {
                    is VoiceInputService.VoiceInputResult.Ready -> {
                        isProcessing = false
                    }
                    is VoiceInputService.VoiceInputResult.Listening -> {
                        isListening = true
                        isProcessing = false
                        errorMessage = null
                    }
                    is VoiceInputService.VoiceInputResult.Processing -> {
                        isListening = false
                        isProcessing = true
                    }
                    is VoiceInputService.VoiceInputResult.Partial -> {
                        recognizedText = result.text
                    }
                    is VoiceInputService.VoiceInputResult.Success -> {
                        recognizedText = result.text
                        isListening = false
                        isProcessing = false
                        onResult(result.text)
                    }
                    is VoiceInputService.VoiceInputResult.Volume -> {
                        volume = result.level
                    }
                    is VoiceInputService.VoiceInputResult.Error -> {
                        isListening = false
                        isProcessing = false
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    val stopListening = {
        voiceInputService.stopRecognition()
        isListening = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "语音输入",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status text
                val statusText = when {
                    isListening -> "正在聆听..."
                    isProcessing -> "处理中..."
                    errorMessage != null -> errorMessage ?: ""
                    recognizedText.isNotEmpty -> "识别完成"
                    else -> "点击麦克风开始"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        errorMessage != null -> MaterialTheme.colorScheme.error
                        isListening || isProcessing -> MaterialTheme.colorScheme.primary
                        recognizedText.isNotEmpty -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Waveform visualization
                AnimatedVisibility(
                    visible = isListening,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    VoiceWaveform(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        volume = volume
                    )
                }

                // Recognized text display
                AnimatedVisibility(
                    visible = recognizedText.isNotEmpty,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = recognizedText,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Microphone button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) {
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        )
                        .clickable {
                            if (isListening) {
                                stopListening()
                            } else {
                                startListening()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "停止录音" else "开始录音",
                        modifier = Modifier.size(40.dp),
                        tint = if (isListening) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = {
                            voiceInputService.cancelRecognition()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("取消")
                    }

                    // Confirm button (only show when text is recognized)
                    if (recognizedText.isNotEmpty && !isListening && !isProcessing) {
                        Button(
                            onClick = {
                                onResult(recognizedText)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("确认")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Voice waveform visualization
 */
@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    volume: Float = 0f
) {
    val bars = remember { listOf(0.2f, 0.5f, 0.8f, 1.0f, 0.8f, 0.5f, 0.2f) }
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    
    val animatedBars = bars.map { baseHeight ->
        infiniteTransition.animateFloat(
            initialValue = baseHeight * 0.5f,
            targetValue = baseHeight * (0.5f + volume),
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "barHeight"
        )
    }

    Canvas(
        modifier = modifier
    ) {
        val barWidth = size.width / (bars.size * 2f)
        val spacing = barWidth
        val centerY = size.height / 2f

        bars.forEachIndexed { index, _ ->
            val animatedHeight = animatedBars[index].value * size.height * 0.8f
            val x = index * (barWidth + spacing) + spacing

            drawRoundRect(
                color = MaterialTheme.colorScheme.primary,
                topLeft = Offset(x, centerY - animatedHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, animatedHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
                alpha = 0.8f
            )
        }
    }
}

/**
 * Voice input button for inline use
 */
@Composable
fun VoiceInputButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: VoiceInputButtonSize = VoiceInputButtonSize.Medium
) {
    val buttonSize = when (size) {
        VoiceInputButtonSize.Small -> 40.dp
        VoiceInputButtonSize.Medium -> 56.dp
        VoiceInputButtonSize.Large -> 72.dp
    }
    val iconSize = when (size) {
        VoiceInputButtonSize.Small -> 20.dp
        VoiceInputButtonSize.Medium -> 28.dp
        VoiceInputButtonSize.Large -> 36.dp
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "语音输入",
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Voice input button size options
 */
enum class VoiceInputButtonSize {
    Small,
    Medium,
    Large
}