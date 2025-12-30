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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import kotlin.math.abs

@Composable
fun GraphCanvas(
    modifier: Modifier = Modifier,
    functions: List<GraphFunction>,
    intersections: List<Offset> = emptyList(),
    viewportScale: Float,
    viewportOffsetX: Float,
    viewportOffsetY: Float,
    onViewportChange: (Float, Float, Float) -> Unit
) {
    val currentScale by rememberUpdatedState(viewportScale)
    val currentOffsetX by rememberUpdatedState(viewportOffsetX)
    val currentOffsetY by rememberUpdatedState(viewportOffsetY)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)

    val textPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 30f
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
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

        val gridColor = Color(0xFF1F2937)
        val axisColor = Color(0xFFE5E7EB)
        val minPxPerUnit = 100f
        val rawStep = minPxPerUnit / viewportScale
        
        var gridStep = 1
        val multipliers = listOf(2, 5, 10)
        var mIdx = 0
        while (gridStep < rawStep) {
            val multiplier = multipliers[mIdx % multipliers.size]
            if (mIdx < multipliers.size) {
                 gridStep = multiplier // 2, 5, 10
            } else {
                 var p10 = 1
                 var tempIdx = mIdx
                 while (tempIdx >= 3) {
                     p10 *= 10
                     tempIdx -= 3
                 }
                 gridStep = multipliers[tempIdx] * p10
            }
            mIdx++
        }
        
        val leftGraphX = -(centerX / viewportScale)
        val firstGridX = (kotlin.math.ceil(leftGraphX / gridStep) * gridStep).toInt()
        
        var currentGridX = firstGridX.toFloat()
        while ((currentGridX * viewportScale) + centerX < width) {
            val xPx = (currentGridX * viewportScale) + centerX
            
            if (xPx >= 0 && xPx <= width) {
                drawLine(
                    color = gridColor,
                    start = Offset(xPx, 0f),
                    end = Offset(xPx, height),
                    strokeWidth = 1f
                )
                
                if (abs(currentGridX) > 0.001f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${currentGridX.toInt()}",
                        xPx,
                        centerY + 40f,
                        textPaint
                    )
                }
            }
            currentGridX += gridStep
        }

        // Horizontal lines (Y-Axis)
        // Top edge y in graph units.
        // y_graph = -(y_px - centerY) / scale
        // y_px = 0 => y_graph = centerY / scale
        // y_px = height => y_graph = (centerY - height) / scale
        // We iterate from top (positive Y) to bottom (negative Y) or vice versa?
        // Let's iterate normally.
        // Top graph Y is roughly (centerY / scale). Bottom is (centerY - height)/scale.
        
        // Let's start from bottom-most visible grid line? 
        // Or just scan visible range.
        val topGraphY = centerY / viewportScale
        val bottomGraphY = (centerY - height) / viewportScale
        
        // Snap to grid
        var currentGridY = (kotlin.math.floor(bottomGraphY / gridStep) * gridStep).toInt().toFloat()
        
        while (currentGridY <= topGraphY + gridStep) {
            val yPx = centerY - (currentGridY * viewportScale)
            
            if (yPx >= 0 && yPx <= height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, yPx),
                    end = Offset(width, yPx),
                    strokeWidth = 1f
                )
                
                if (abs(currentGridY) > 0.001f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "${currentGridY.toInt()}",
                        centerX - 40f,
                        yPx + 10f,
                        textPaint
                    )
                }
            }
            currentGridY += gridStep
        }

        // Draw Axes (Main X/Y)
        drawLine(
            color = axisColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2f
        )
        drawLine(
            color = axisColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, height),
            strokeWidth = 2f
        )

        // Draw Functions
        // Optimization: Only Draw visible functions
        functions.filter { it.isVisible }.forEach { func ->
            val path = Path()
            var started = false
            
            val step = 2 // Optimization
            
            for (px in 0 until width.toInt() step step) {
                val x = (px - centerX) / viewportScale
                val y = func.calculate(x.toDouble())

                if (y.isFinite() && abs(y) < 1000) {
                    val py = centerY - (y * viewportScale).toFloat()
                    
                    if (!started) {
                        path.moveTo(px.toFloat(), py)
                        started = true
                    } else {
                        // Check for huge jumps (discontinuity)
                        if (abs(py - (centerY - (func.calculate(((px - step) - centerX) / viewportScale.toDouble()) * viewportScale).toFloat())) < height) {
                             path.lineTo(px.toFloat(), py)
                        } else {
                             path.moveTo(px.toFloat(), py)
                        }
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
        
        // Draw Intersections
        intersections.forEach { point ->
            // Point (x, y) is in graph coordinates. Convert to pixels.
            val px = (point.x * viewportScale) + centerX
            val py = centerY - (point.y * viewportScale)
            
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.Red,
                radius = 5f,
                center = Offset(px, py)
            )
            
            // Optional: Draw text coordinates
            drawContext.canvas.nativeCanvas.drawText(
                String.format("(%.1f, %.1f)", point.x, point.y),
                px + 10f,
                py - 10f,
                textPaint
            )
        }
    }
}
