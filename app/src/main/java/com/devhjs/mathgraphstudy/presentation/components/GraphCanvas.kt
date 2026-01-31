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
import androidx.compose.ui.tooling.preview.Preview
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.graph.GraphGridCalculator
import kotlin.math.abs
import kotlin.math.sqrt

// 교점 표시를 위한 최소 줌 레벨 (이 값 이상일 때만 교점 표시)
private const val MIN_SCALE_FOR_INTERSECTIONS = 20f
// 교점 선택을 위한 터치 허용 반경 (픽셀 단위)
private const val INTERSECTION_TAP_RADIUS = 30f

/**
 * 수학 그래프를 렌더링하고 사용자의 제스처(줌, 이동, 터치)를 처리하는 캔버스 컴포넌트입니다.
 *
 * @param modifier 컴포넌트의 레이아웃 수정자
 * @param functions 그릴 그래프 함수들의 리스트 (수식, 색상, 가시성 포함)
 * @param intersections 두 그래프 간의 교점 좌표 리스트
 * @param selectedIntersection 현재 사용자가 선택한 교점 (선택 시 좌표값 표시)
 * @param viewportScale 현재 화면의 줌 배율 (확대/축소 정도)
 * @param viewportOffsetX 현재 뷰포트의 X축 이동 거리 (Pan Offset)
 * @param viewportOffsetY 현재 뷰포트의 Y축 이동 거리 (Pan Offset)
 * @param onViewportChange 줌이나 이동이 발생했을 때 호출되는 콜백 (scale, offsetX, offsetY)
 * @param onIntersectionSelected 교점을 터치했을 때 호출되는 콜백
 * @param onIntersectionDismiss 교점 선택을 해제할 때 호출되는 콜백 (빈 공간 터치 등)
 */
@Composable
fun GraphCanvas(
    modifier: Modifier = Modifier,
    functions: List<GraphFunction> = emptyList(),
    intersections: List<Offset> = emptyList(),
    selectedIntersection: Offset? = null,
    viewportScale: Float = 15f,
    viewportOffsetX: Float=15f,
    viewportOffsetY: Float= 15f,
    onViewportChange: (Float, Float, Float) -> Unit= { _, _, _ -> },
    onIntersectionSelected: (Offset) -> Unit = {},
    onIntersectionDismiss: () -> Unit = {}
) {
    // 제스처 감지 람다 내에서 최신 상태값을 참조하기 위해 rememberUpdatedState 사용
    // (컴포지션이 다시 일어나지 않더라도 제스처 콜백 내에서는 최신 값을 사용 보장)
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
            // 줌(Zoom) 및 팬(Pan) 제스처 처리
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // 줌 레벨 제한: 최소 10배 ~ 최대 500배
                    val newScale = (currentScale * zoom).coerceIn(10f, 500f)
                    // 현재 위치에서 드래그한 만큼 이동 (Pan)
                    val newOffsetX = currentOffsetX + pan.x
                    val newOffsetY = currentOffsetY + pan.y

                    currentOnViewportChange(newScale, newOffsetX, newOffsetY)
                }
            }
            // 탭(Tap) 제스처 처리 (교점 선택용)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // 줌 레벨이 충분히 확대되었을 때만 교점 탭 기능을 활성화
                    if (currentScale >= MIN_SCALE_FOR_INTERSECTIONS) {
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        // 현재 화면의 중심 좌표 계산
                        val centerX = width / 2 + currentOffsetX
                        val centerY = height / 2 + currentOffsetY

                        // 탭한 화면 위치에서 가장 가까운 교점 찾기 (유클리드 거리 계산)
                        val tappedIntersection = currentIntersections.find { point ->
                            // 교점 좌표(수학 좌표)를 화면 픽셀 좌표로 변환
                            val px = (point.x * currentScale) + centerX
                            val py = centerY - (point.y * currentScale)

                            // 터치한 위치와 교점 사이의 거리 계산
                            val distance =
                                sqrt((tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py))
                            distance <= INTERSECTION_TAP_RADIUS // 허용 반경 내인지 확인
                        }

                        if (tappedIntersection != null) {
                            currentOnIntersectionSelected(tappedIntersection)
                        } else {
                            // 빈 공간을 탭했으면 선택 해제
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
        // 그리드 간격 및 그릴 라인 계산 (별도 로직 클래스 사용)
        val gridStep = GraphGridCalculator.calculateGridStep(viewportScale)
        
        // --- X축 Grid 그리기 (세로선) ---
        // 화면에 보이는 X축 범위 계산
        val leftGraphX = -(centerX / viewportScale)
        val rightGraphX = (width - centerX) / viewportScale
        
        val xGridLines = GraphGridCalculator.calculateVisibleGridLines(
            minGraphVal = leftGraphX,
            maxGraphVal = rightGraphX,
            gridStep = gridStep
        )
        
        xGridLines.forEach { currentGridX ->
            val xPx = (currentGridX * viewportScale) + centerX
            
            drawLine(
                color = gridColor,
                start = Offset(xPx, 0f),
                end = Offset(xPx, height),
                strokeWidth = 1f
            )
            
            // 원점(0)이 아닌 경우에만 숫자 표시
            if (abs(currentGridX) > 0.001f) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${currentGridX.toInt()}",
                    xPx,
                    centerY + 40f, // X축 아래에 숫자 표시
                    textPaint
                )
            }
        }

        // --- Y축 Grid 그리기 (가로선) ---
        // 화면에 보이는 Y축 범위 계산 (Graph Y 좌표 기준)
        // Canvas 좌표계(아래로 증가)와 반대이므로 주의: Top이 Y값이 더 큼
        // 하지만 calculateVisibleGridLines는 min/max만 중요하므로 작은값~큰값으로 전달
        val topGraphY = centerY / viewportScale
        val bottomGraphY = (centerY - height) / viewportScale
        
        val yGridLines = GraphGridCalculator.calculateVisibleGridLines(
            minGraphVal = bottomGraphY, // 작은 값 (화면 하단)
            maxGraphVal = topGraphY,    // 큰 값 (화면 상단)
            gridStep = gridStep
        )
        
        yGridLines.forEach { currentGridY ->
            val yPx = centerY - (currentGridY * viewportScale)
            
            drawLine(
                color = gridColor,
                start = Offset(0f, yPx),
                end = Offset(width, yPx),
                strokeWidth = 1f
            )
            
            // 원점(0)이 아닌 경우에만 숫자 표시
            if (abs(currentGridY) > 0.001f) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${currentGridY.toInt()}",
                    centerX - 40f,
                    yPx + 10f,
                    textPaint
                )
            }
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
        // --- 그래프 함수 그리기 ---
        // 줌 레벨에 따라 계산 간격(dynamicStep)을 조정하여 성능과 퀄리티 균형 조절
        // 줌이 가까워질수록 step 계산을 조밀하게 하여 곡선을 부드럽게 표현
        val dynamicStep = (50f / viewportScale).coerceIn(1f, 4f).toInt().coerceAtLeast(1)
        
        functions.filter { it.isVisible }.forEach { func ->
            val path = Path()
            var started = false
            
            // 화면 가로 픽셀을 순회하며 y값 계산
            for (px in 0 until width.toInt() step dynamicStep) {
                val x = (px - centerX) / viewportScale
                val y = func.calculate(x.toDouble())

                // 유효한 값이고 화면 범위 내에서 너무 벗어나지 않은 경우에만 선을 그림
                if (y.isFinite() && abs(y) < 1000) {
                    val py = centerY - (y * viewportScale).toFloat()
                    
                    if (!started) {
                        path.moveTo(px.toFloat(), py)
                        started = true
                    } else {
                        // 불연속점 처리 (값이 갑자기 튀는 경우 선을 잇지 않음 - 예: 탄젠트 함수)
                        val prevX = (px - dynamicStep - centerX) / viewportScale
                        val prevY = func.calculate(prevX.toDouble())
                        if (prevY.isFinite() && abs(py - (centerY - (prevY * viewportScale).toFloat())) < height) {
                             path.lineTo(px.toFloat(), py)
                        } else {
                             path.moveTo(px.toFloat(), py)
                        }
                    }
                } else {
                    // 무한대나 유효하지 않은 값이면 경로 끊기
                    started = false
                }
            }
            
            drawPath(
                path = path,
                color = Color(func.color),
                style = Stroke(width = 3f)
            )
        }
        
        // --- 교점 그리기 ---
        // 사용자가 그래프를 충분히 확대했을 때만 교점을 표시 (혼잡도 방지)
        if (viewportScale >= MIN_SCALE_FOR_INTERSECTIONS) {
            intersections.forEach { point ->
                // 그래프 좌표(x,y)를 화면 픽셀 좌표(px, py)로 변환
                val px = (point.x * viewportScale) + centerX
                val py = centerY - (point.y * viewportScale)
                
                // 화면 밖의 교점은 그리지 않음 (화면 영역에 약간의 여유분 +20f 포함)
                if (px >= -20f && px <= width + 20f && py >= -20f && py <= height + 20f) {
                    // 현재 이 교점이 선택된 상태인지 확인
                    val isSelected = selectedIntersection?.let { 
                        abs(it.x - point.x) < 0.001f && abs(it.y - point.y) < 0.001f 
                    } ?: false
                    
                    // 교점 포인트 그리기 (선택되면 더 크고 노란색)
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
                    
                    // 선택된 교점인 경우: 좌표 정보를 텍스트로 표시
                    if (isSelected) {
                        val coordText = String.format("(%.2f, %.2f)", point.x, point.y)
                        val textWidth = textPaint.measureText(coordText)
                        val padding = 8f
                        
                        // 텍스트 배경 박스 그리기
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

@Preview
@Composable
private fun GraphCanvasPreview() {
    GraphCanvas()
}

