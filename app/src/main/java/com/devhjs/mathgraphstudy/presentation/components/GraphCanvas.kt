package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.graph.GraphViewportState
import com.devhjs.mathgraphstudy.presentation.graph.rememberGraphViewportState
import com.devhjs.mathgraphstudy.presentation.math.GraphGridCalculator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.sqrt

// 교점 표시를 위한 최소 줌 레벨 (이 값 이상일 때만 교점 표시)
private const val MIN_SCALE_FOR_INTERSECTIONS = 20f
// 교점 선택을 위한 터치 허용 반경 (픽셀 단위)
private const val INTERSECTION_TAP_RADIUS = 30f
// 제스처가 멈춘 뒤 보이는 범위를 ViewModel 에 알리기까지 기다리는 시간
private const val VISIBLE_RANGE_DEBOUNCE_MS = 150L

/**
 * 수학 그래프를 렌더링하고 사용자의 제스처(줌, 이동, 터치)를 처리하는 캔버스 컴포넌트입니다.
 *
 * 확대/이동 상태는 [viewport]에 직접 반영하고 그리기 단계에서만 읽으므로,
 * 드래그 중에는 컴포지션 없이 캔버스만 다시 그려집니다.
 *
 * @param modifier 컴포넌트의 레이아웃 수정자
 * @param viewport 확대/이동 상태
 * @param functions 그릴 그래프 함수들의 리스트 (수식, 색상, 가시성 포함)
 * @param intersections 두 그래프 간의 교점 좌표 리스트
 * @param selectedIntersection 현재 사용자가 선택한 교점 (선택 시 좌표값 표시)
 * @param onVisibleRangeChange 화면에 보이는 x 범위가 바뀌었을 때 호출 (제스처가 멈춘 뒤 디바운스)
 * @param onIntersectionSelected 교점을 터치했을 때 호출되는 콜백
 * @param onIntersectionDismiss 교점 선택을 해제할 때 호출되는 콜백 (빈 공간 터치 등)
 */
@OptIn(FlowPreview::class)
@Composable
fun GraphCanvas(
    modifier: Modifier = Modifier,
    viewport: GraphViewportState = rememberGraphViewportState(),
    functions: List<GraphFunction> = emptyList(),
    intersections: List<Offset> = emptyList(),
    selectedIntersection: Offset? = null,
    onVisibleRangeChange: (Double, Double) -> Unit = { _, _ -> },
    onIntersectionSelected: (Offset) -> Unit = {},
    onIntersectionDismiss: () -> Unit = {}
) {
    // 제스처 감지 람다 내에서 최신 상태값을 참조하기 위해 rememberUpdatedState 사용
    val currentIntersections by rememberUpdatedState(intersections)
    val currentOnIntersectionSelected by rememberUpdatedState(onIntersectionSelected)
    val currentOnIntersectionDismiss by rememberUpdatedState(onIntersectionDismiss)
    val currentOnVisibleRangeChange by rememberUpdatedState(onVisibleRangeChange)

    // 보이는 범위가 바뀌면 제스처가 잠시 멈췄을 때 한 번만 알림 (교점 재계산 빈도 제한)
    LaunchedEffect(viewport) {
        snapshotFlow { viewport.visibleXRange() }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(VISIBLE_RANGE_DEBOUNCE_MS)
            .collect { range -> currentOnVisibleRangeChange(range.start, range.endInclusive) }
    }

    val textPaint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            textSize = 30f
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    
    // 선택된 교점의 좌표 표시용 배경 페인트
    val coordBgPaint = remember {
        Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(200, 40, 40, 40)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // 격자/라벨/곡선이 캔버스 영역 밖(상태바, 입력 패널)으로 그려지지 않도록 자름
            .clipToBounds()
            .background(AppColors.BlackCharcoal)
            .onSizeChanged { viewport.onCanvasSizeChanged(it) }
            // 줌(Zoom) 및 팬(Pan) 제스처 처리
            .pointerInput(viewport) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    viewport.onGesture(centroid, pan, zoom)
                }
            }
            // 탭(Tap) 제스처 처리 (교점 선택용)
            .pointerInput(viewport) {
                detectTapGestures { tapOffset ->
                    val scale = viewport.scale
                    // 줌 레벨이 충분히 확대되었을 때만 교점 탭 기능을 활성화
                    if (scale >= MIN_SCALE_FOR_INTERSECTIONS) {
                        // 현재 화면의 원점 좌표 계산
                        val centerX = size.width / 2 + viewport.offsetX
                        val centerY = size.height / 2 + viewport.offsetY

                        // 탭한 화면 위치에서 가장 가까운 교점 찾기 (유클리드 거리 계산)
                        val tappedIntersection = currentIntersections.find { point ->
                            // 교점 좌표(수학 좌표)를 화면 픽셀 좌표로 변환
                            val px = (point.x * scale) + centerX
                            val py = centerY - (point.y * scale)

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
        // 확대/이동 상태는 그리기 단계에서 읽음 -> 값이 바뀌면 다시 그리기만 발생
        val viewportScale = viewport.scale
        val width = size.width
        val height = size.height
        val centerX = width / 2 + viewport.offsetX
        val centerY = height / 2 + viewport.offsetY

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
            // 직전 점의 화면 y 좌표 (NaN 이면 경로가 끊긴 상태)
            var prevPy = Float.NaN
            
            // 화면 가로 픽셀을 순회하며 y값 계산
            for (px in 0 until width.toInt() step dynamicStep) {
                val x = (px - centerX) / viewportScale
                val y = func.evaluate(x.toDouble())

                if (y.isFinite()) {
                    // 화면 밖으로 크게 벗어난 값은 화면 위아래 한 화면 높이까지로 제한 (float 오버플로 방지)
                    val py = (centerY - y * viewportScale).toFloat().coerceIn(-height, 2 * height)
                    
                    // 불연속점 처리 (값이 갑자기 튀는 경우 선을 잇지 않음 - 예: 탄젠트 함수)
                    if (!prevPy.isNaN() && abs(py - prevPy) < height) {
                        path.lineTo(px.toFloat(), py)
                    } else {
                        path.moveTo(px.toFloat(), py)
                    }
                    prevPy = py
                } else {
                    // 무한대나 유효하지 않은 값이면 경로 끊기
                    prevPy = Float.NaN
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

