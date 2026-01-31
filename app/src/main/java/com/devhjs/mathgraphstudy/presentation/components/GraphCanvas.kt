package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

// 교점 표시를 위한 최소 줌 레벨 (이 값 이상일 때만 교점 표시)
private const val MIN_SCALE_FOR_INTERSECTIONS = 20f
// 교점 선택을 위한 터치 허용 반경 (픽셀)
private const val INTERSECTION_TAP_RADIUS = 30f

@Composable
fun GraphCanvas(
    modifier: Modifier = Modifier,
    functions: List<GraphFunction>,
    intersections: List<Offset> = emptyList(),
    selectedIntersection: Offset? = null,
    viewportScale: Float,
    viewportOffsetX: Float,
    viewportOffsetY: Float,
    onViewportChange: (Float, Float, Float) -> Unit,
    onIntersectionSelected: (Offset) -> Unit = {},
    onIntersectionDismiss: () -> Unit = {}
) {
    val currentScale by rememberUpdatedState(viewportScale)
    val currentOffsetX by rememberUpdatedState(viewportOffsetX)
    val currentOffsetY by rememberUpdatedState(viewportOffsetY)
    val currentOnViewportChange by rememberUpdatedState(onViewportChange)
    val currentIntersections by rememberUpdatedState(intersections)
    val currentOnIntersectionSelected by rememberUpdatedState(onIntersectionSelected)
    val currentOnIntersectionDismiss by rememberUpdatedState(onIntersectionDismiss)

    val textPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 30f
        color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    // 선택된 교점의 좌표 표시용 배경 페인트
    val coordBgPaint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        color = android.graphics.Color.argb(200, 40, 40, 40)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BlackCharcoal)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (currentScale * zoom).coerceIn(10f, 500f)
                    val newOffsetX = currentOffsetX + pan.x
                    val newOffsetY = currentOffsetY + pan.y
                    
                    currentOnViewportChange(newScale, newOffsetX, newOffsetY)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // 줌 레벨이 충분할 때만 교점 탭 감지
                    if (currentScale >= MIN_SCALE_FOR_INTERSECTIONS) {
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        val centerX = width / 2 + currentOffsetX
                        val centerY = height / 2 + currentOffsetY
                        
                        // 탭한 위치에서 가장 가까운 교점 찾기
                        val tappedIntersection = currentIntersections.find { point ->
                            val px = (point.x * currentScale) + centerX
                            val py = centerY - (point.y * currentScale)
                            val distance = sqrt((tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py))
                            distance <= INTERSECTION_TAP_RADIUS
                        }
                        
                        if (tappedIntersection != null) {
                            currentOnIntersectionSelected(tappedIntersection)
                        } else {
                            // 빈 공간 탭 시 선택 해제
                            currentOnIntersectionDismiss()
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2 + viewportOffsetX
        val centerY = height / 2 + viewportOffsetY

        val gridColor = AppColors.GridColor
        val axisColor = AppColors.TextPrimary
        val minPxPerUnit = 100f
        val rawStep = minPxPerUnit / viewportScale
        
        var gridStep = 1
        val multipliers = listOf(2, 5, 10)
        var mIdx = 0
        while (gridStep < rawStep) {
            val multiplier = multipliers[mIdx % multipliers.size]
            if (mIdx < multipliers.size) {
                 gridStep = multiplier
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
        val firstGridX = (ceil(leftGraphX / gridStep) * gridStep).toInt()
        
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
        // 동적 step: 줌 레벨에 따라 조정하여 부드러운 곡선 렌더링
        val dynamicStep = (50f / viewportScale).coerceIn(1f, 4f).toInt().coerceAtLeast(1)
        
        functions.filter { it.isVisible }.forEach { func ->
            val path = Path()
            var started = false
            
            for (px in 0 until width.toInt() step dynamicStep) {
                val x = (px - centerX) / viewportScale
                val y = func.calculate(x.toDouble())

                if (y.isFinite() && abs(y) < 1000) {
                    val py = centerY - (y * viewportScale).toFloat()
                    
                    if (!started) {
                        path.moveTo(px.toFloat(), py)
                        started = true
                    } else {
                        // Check for huge jumps (discontinuity)
                        val prevX = (px - dynamicStep - centerX) / viewportScale
                        val prevY = func.calculate(prevX.toDouble())
                        if (prevY.isFinite() && abs(py - (centerY - (prevY * viewportScale).toFloat())) < height) {
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
                color = Color(func.color),
                style = Stroke(width = 3f)
            )
        }
        
        // Draw Intersections - 줌 레벨이 충분할 때만 표시
        if (viewportScale >= MIN_SCALE_FOR_INTERSECTIONS) {
            intersections.forEach { point ->
                // Point (x, y) is in graph coordinates. Convert to pixels.
                val px = (point.x * viewportScale) + centerX
                val py = centerY - (point.y * viewportScale)
                
                // 화면 내에 있는지 확인
                if (px >= -20f && px <= width + 20f && py >= -20f && py <= height + 20f) {
                    // 선택된 교점인지 확인
                    val isSelected = selectedIntersection?.let { 
                        abs(it.x - point.x) < 0.001f && abs(it.y - point.y) < 0.001f 
                    } ?: false
                    
                    // 교점 원 그리기
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 12f else 8f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = if (isSelected) Color.Yellow else Color.Red,
                        radius = if (isSelected) 8f else 5f,
                        center = Offset(px, py)
                    )
                    
                    // 선택된 교점일 때만 좌표 텍스트 표시
                    if (isSelected) {
                        val coordText = String.format("(%.2f, %.2f)", point.x, point.y)
                        val textWidth = textPaint.measureText(coordText)
                        val padding = 8f
                        
                        // 배경 그리기
                        drawContext.canvas.nativeCanvas.drawRoundRect(
                            px - textWidth / 2 - padding,
                            py - 50f,
                            px + textWidth / 2 + padding,
                            py - 20f,
                            8f, 8f,
                            coordBgPaint
                        )
                        
                        // 좌표 텍스트 그리기
                        drawContext.canvas.nativeCanvas.drawText(
                            coordText,
                            px,
                            py - 30f,
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

