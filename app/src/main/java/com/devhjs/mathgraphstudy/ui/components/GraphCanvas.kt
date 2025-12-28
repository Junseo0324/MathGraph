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
import androidx.compose.ui.graphics.nativeCanvas
import com.devhjs.mathgraphstudy.domain.model.GraphFunction

@Composable
fun GraphCanvas(
    functions: List<GraphFunction>,
    intersections: List<Offset> = emptyList(), // Default empty for preview
    modifier: Modifier = Modifier,
    viewportScale: Float,
    viewportOffsetX: Float,
    viewportOffsetY: Float,
    onViewportChange: (Float, Float, Float) -> Unit
) {
    val currentScale by rememberUpdatedState(viewportScale)
    val currentOffsetX by rememberUpdatedState(viewportOffsetX)
    val currentOffsetY by rememberUpdatedState(viewportOffsetY)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)

    val textPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 30f
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
    }

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
        
        val intersections = mutableListOf<Offset>()
        // Assume screen width approx 1080px. Center is 540.
        // x_start_pixel = 0. x_end_pixel = 1080.
        // x_graph = (x_pixel - (width/2 + offsetX)) / scale
        // x_start_graph = (0 - (540 + offsetX)) / scale
        // x_end_graph = (1080 - (540 + offsetX)) / scale
        
        val startX = (-540f - viewportOffsetX) / viewportScale
        val endX = (540f - viewportOffsetX) / viewportScale
        
        val rangeStart = startX.toDouble()
        val rangeEnd = endX.toDouble()
        
        // Vertical lines
        if (viewportScale > 1f) {
            // Draw Vertical Grid & X-Axis Labels
            var x = (centerX % viewportScale)
            if (x < 0) x += viewportScale
            
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                
                // Draw X label
                // Value at x: (x - centerX) / scale
                // Only draw every 2nd or 5th based on scale to avoid clutter?
                // For now draw every grid line but skip close to 0 if it overlaps Y axis
                val value = (x - centerX) / viewportScale
                if (kotlin.math.abs(value) > 0.1) { // Skip near 0
                     drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", value),
                        x,
                        centerY + 40f, // Below axis
                        textPaint
                    )
                }

                x += viewportScale
            }

            // Draw Horizontal Grid & Y-Axis Labels
            var y = (centerY % viewportScale)
            if (y < 0) y += viewportScale
            
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                
                 val value = -(y - centerY) / viewportScale
                 if (kotlin.math.abs(value) > 0.1) {
                     drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f", value),
                        centerX - 40f, // Left of axis
                        y + 10f, // Center vertically roughly
                        textPaint
                    )
                 }

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
            
            val step = 2 // Optimization
            
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
                        if (kotlin.math.abs(py - (centerY - (func.calculate(((px - step) - centerX) / viewportScale.toDouble()) * viewportScale).toFloat())) < height) {
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
