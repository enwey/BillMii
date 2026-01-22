package com.billmii.android.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * 柱状图数据项
 */
data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color = Color.Blue
)

/**
 * 柱状图组件
 */
@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    barWidth: Float = 40f,
    barSpacing: Float = 20f,
    showGridLines: Boolean = true
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOf { it.value }
    val minValue = data.minOf { it.value }
    val range = maxValue - minValue
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasSize = size
        val padding = 40f
        val chartWidth = canvasSize.width - padding * 2
        val chartHeight = canvasSize.height - padding * 2
        
        // 绘制网格线
        if (showGridLines) {
            drawGridLines(
                startX = padding,
                endX = canvasSize.width - padding,
                startY = padding,
                endY = canvasSize.height - padding,
                count = 5,
                range = range,
                minValue = minValue
            )
        }
        
        // 绘制柱状条
        val totalBarWidth = barWidth + barSpacing
        val barsWidth = data.size * totalBarWidth - barSpacing
        val startX = padding + (chartWidth - barsWidth) / 2
        
        data.forEachIndexed { index, item ->
            val x = startX + index * totalBarWidth
            val barHeight = if (range > 0) {
                ((item.value - minValue) / range) * chartHeight
            } else {
                chartHeight
            }
            val y = canvasSize.height - padding - barHeight
            
            drawBar(
                x = x,
                y = y,
                width = barWidth,
                height = barHeight,
                color = item.color
            )
            
            // 绘制标签
            drawLabel(
                text = item.label,
                x = x + barWidth / 2,
                y = canvasSize.height - padding + 20f
            )
        }
    }
}

private fun DrawScope.drawBar(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color
) {
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = androidx.compose.ui.geometry.Size(width, height)
    )
}

private fun DrawScope.drawGridLines(
    startX: Float,
    endX: Float,
    startY: Float,
    endY: Float,
    count: Int,
    range: Float,
    minValue: Float
) {
    val step = (endY - startY) / count
    
    for (i in 0..count) {
        val y = startY + i * step
        
        // 绘制水平线
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 1f
        )
        
        // 绘制Y轴标签
        val value = minValue + (range * (count - i) / count)
        drawValueLabel(
            text = value.formatValue(),
            x = startX - 10f,
            y = y
        )
    }
}

private fun DrawScope.drawLabel(text: String, x: Float, y: Float) {
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y,
        textPaint
    )
}

private fun DrawScope.drawValueLabel(text: String, x: Float, y: Float) {
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 20f
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }
    
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y + 6f,
        textPaint
    )
}

private fun Float.formatValue(): String {
    return when {
        this >= 10000 -> String.format("%.0f万", this / 10000)
        this >= 1000 -> String.format("%.1f千", this / 1000)
        this >= 100 -> String.format("%.0f", this)
        else -> String.format("%.2f", this)
    }
}

/**
 * 水平柱状图
 */
@Composable
fun HorizontalBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    barHeight: Float = 30f,
    barSpacing: Float = 15f
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOf { it.value }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasSize = size
        val padding = 40f
        val chartWidth = canvasSize.width - padding * 2
        
        data.forEachIndexed { index, item ->
            val y = padding + index * (barHeight + barSpacing)
            val barWidth = if (maxValue > 0) {
                (item.value / maxValue) * chartWidth
            } else {
                0f
            }
            
            drawBar(
                x = padding,
                y = y,
                width = barWidth,
                height = barHeight,
                color = item.color
            )
            
            // 绘制标签
            drawHorizontalLabel(
                text = item.label,
                x = padding - 10f,
                y = y + barHeight / 2
            )
            
            // 绘制数值
            drawValueLabel(
                text = item.value.formatValue(),
                x = padding + barWidth + 10f,
                y = y + barHeight / 2
            )
        }
    }
}

private fun DrawScope.drawHorizontalLabel(text: String, x: Float, y: Float) {
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }
    
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        y + 8f,
        textPaint
    )
}