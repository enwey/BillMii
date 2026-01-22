package com.billmii.android.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 饼图数据项
 */
data class PieChartData(
    val value: Float,
    val color: Color,
    val label: String
)

/**
 * 饼图组件
 */
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    holeSize: Float = 0f // 0 = 实心饼图, 0.5 = 半圆
) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasSize = size
        val centerX = canvasSize.width / 2
        val centerY = canvasSize.height / 2
        val radius = min(centerX, centerY) * (1 - holeSize)
        
        var startAngle = -90f // 从12点钟方向开始
        
        data.forEach { item ->
            if (item.value > 0) {
                val sweepAngle = (item.value / total) * 360f
                
                drawPieSlice(
                    centerX = centerX,
                    centerY = centerY,
                    radius = radius,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    color = item.color,
                    holeSize = holeSize
                )
                
                startAngle += sweepAngle
            }
        }
        
        // 绘制中心空洞（环形图）
        if (holeSize > 0) {
            drawCircle(
                color = Color.White,
                radius = radius * holeSize,
                center = Offset(centerX, centerY)
            )
        }
    }
}

private fun DrawScope.drawPieSlice(
    centerX: Float,
    centerY: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    holeSize: Float
) {
    val startAngleRad = Math.toRadians(startAngle.toDouble()).toFloat()
    val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble()).toFloat()
    
    val startX = centerX + radius * cos(startAngleRad)
    val startY = centerY + radius * sin(startAngleRad)
    val endX = centerX + radius * cos(endAngleRad)
    val endY = centerY + radius * sin(endAngleRad)
    
    val holeRadius = radius * holeSize
    val holeStartX = centerX + holeRadius * cos(startAngleRad)
    val holeStartY = centerY + holeRadius * sin(startAngleRad)
    val holeEndX = centerX + holeRadius * cos(endAngleRad)
    val holeEndY = centerY + holeRadius * sin(endAngleRad)
    
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(startX, startY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = centerX - radius,
                    top = centerY - radius,
                    right = centerX + radius,
                    bottom = centerY + radius
                ),
                startAngleDegrees = startAngle,
                sweepAngleDegrees = sweepAngle,
                forceMoveTo = false
            )
            if (holeSize > 0) {
                lineTo(holeEndX, holeEndY)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = centerX - holeRadius,
                        top = centerY - holeRadius,
                        right = centerX + holeRadius,
                        bottom = centerY + holeRadius
                    ),
                    startAngleDegrees = startAngle + sweepAngle,
                    sweepAngleDegrees = -sweepAngle,
                    forceMoveTo = false
                )
                close()
            }
        },
        color = color
    )
}

/**
 * 饼图图例
 */
@Composable
fun PieChartLegend(
    data: List<PieChartData>
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { item ->
            LegendItem(
                color = item.color,
                label = item.label,
                value = item.value
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: Float
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.foundation.layout.size(16.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.draw.clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color)
                    .size(12.dp)
            )
        }
        androidx.compose.material3.Text(
            text = "$label: $value",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
    }
}