package com.example.deviceinfo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LiveTelemetryChart(
    dataPoints: List<Float>, // Values between 0.0f and 1.0f
    lineColor: Color = Color(0xFF00E5FF),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (dataPoints.size < 2) return@Canvas

        val path = Path()
        val widthPerPoint = size.width / (dataPoints.size - 1)

        dataPoints.forEachIndexed { index, point ->
            val x = index * widthPerPoint
            val y = size.height - (point * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
