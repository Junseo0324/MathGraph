package com.devhjs.mathgraphstudy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.devhjs.mathgraphstudy.domain.model.GraphFunction

@Composable
fun GraphCanvas(
    functions: List<GraphFunction>,
    viewportScale: Float,
    viewportOffsetX: Float,
    viewportOffsetY: Float,
    onViewportChange: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentScale by rememberUpdatedState(viewportScale)
    val currentOffsetX by rememberUpdatedState(viewportOffsetX)
    val currentOffsetY by rememberUpdatedState(viewportOffsetY)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Dark background like sample
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (currentScale * zoom).coerceIn(10f, 500f)
                    val newOffsetX = currentOffsetX + pan.x
                    val newOffsetY = currentOffsetY + pan.y
                    
                    currentOnViewportChange(newScale, newOffsetX, newOffsetY)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2 + viewportOffsetX
        val centerY = height / 2 + viewportOffsetY

        // Draw Grid
        val gridColor = Color(0xFF1F2937)
        val axisColor = Color(0xFFE5E7EB)
        
        // Vertical lines
        // Safety check to avoid infinite loop if scale is 0/NaN
        if (viewportScale > 1f) {
            // Draw Vertical Grid
            var x = (centerX % viewportScale)
            if (x < 0) x += viewportScale
            
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += viewportScale
            }

            // Draw Horizontal Grid
            var y = (centerY % viewportScale)
            if (y < 0) y += viewportScale
            
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += viewportScale
            }
        }

        // Draw Axes
        // X-Axis
        drawLine(
            color = axisColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2f
        )
        // Y-Axis
        drawLine(
            color = axisColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, height),
            strokeWidth = 2f
        )

        // Draw Functions
        functions.filter { it.isVisible }.forEach { func ->
            val path = Path()
            var started = false
            
            // Optimization: Step size can be adjusted. 1 pixel step for smoothness.
            val step = 1
            
            for (px in 0 until width.toInt() step step) {
                val x = (px - centerX) / viewportScale
                val y = func.calculate(x.toDouble())

                if (y.isFinite() && kotlin.math.abs(y) < 1000) {
                    val py = centerY - (y * viewportScale).toFloat()
                    
                    if (!started) {
                        path.moveTo(px.toFloat(), py)
                        started = true
                    } else {
                        // Check for huge jumps (discontinuity)
                        path.lineTo(px.toFloat(), py)
                    }
                } else {
                    started = false
                }
            }
            
            drawPath(
                path = path,
                color = func.color,
                style = Stroke(width = 3f)
            )
        }
    }
}
