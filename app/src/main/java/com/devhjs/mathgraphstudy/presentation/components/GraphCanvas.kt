package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColorScheme
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.LocalAppColors
import com.devhjs.mathgraphstudy.presentation.graph.GraphViewportState
import com.devhjs.mathgraphstudy.presentation.graph.rememberGraphViewportState
import com.devhjs.mathgraphstudy.presentation.math.CurveSampler
import com.devhjs.mathgraphstudy.presentation.math.GraphGridCalculator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.sqrt

// 교점 표시를 위한 최소 줌 레벨 (이 값 이상일 때만 교점 표시)
private const val MIN_SCALE_FOR_INTERSECTIONS = 10f
// 교점 선택을 위한 터치 허용 반경 (픽셀 단위)
private const val INTERSECTION_TAP_RADIUS = 30f
// 트레이스를 시작할 때 곡선을 고르는 터치 허용 반경 (픽셀 단위)
private const val TRACE_PICK_RADIUS = 80f
// 제스처가 멈춘 뒤 보이는 범위를 ViewModel 에 알리기까지 기다리는 시간
private const val VISIBLE_RANGE_DEBOUNCE_MS = 150L
// 축 라벨과 화면 가장자리 사이 여백 (px)
private const val LABEL_MARGIN = 8f
// 버튼 한 번에 확대/축소하는 배율
private const val ZOOM_STEP = 1.5f

/** 곡선을 따라가며 좌표를 보는 트레이스 상태 */
private data class TraceState(val functionId: String, val x: Double)

/**
 * 수학 그래프를 렌더링하고 사용자의 제스처를 처리하는 캔버스 컴포넌트입니다.
 *
 * - 핀치/드래그: 확대·이동
 * - 탭: 교점 선택 (좌표 표시), 빈 곳 탭은 선택/트레이스 해제
 * - 길게 누른 뒤 드래그: 가장 가까운 곡선을 따라가며 (x, y) 표시 (트레이스)
 * - 모서리 버튼: 확대, 축소, 원점으로
 *
 * 확대/이동 상태는 [viewport]에 직접 반영하고 그리기 단계에서만 읽으므로,
 * 드래그 중에는 컴포지션 없이 캔버스만 다시 그려집니다.
 *
 * @param viewport 확대/이동 상태
 * @param functions 그릴 그래프 함수들의 리스트 (수식, 색상, 가시성 포함)
 * @param intersections 두 그래프 간의 교점 좌표 리스트
 * @param selectedIntersection 현재 사용자가 선택한 교점 (선택 시 좌표값 표시)
 * @param controlsAlignment 확대/축소/원점 버튼 위치 (태블릿은 오른쪽 패널을 피해 왼쪽에 둠)
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
    controlsAlignment: Alignment = Alignment.TopEnd,
    onVisibleRangeChange: (Double, Double) -> Unit = { _, _ -> },
    onIntersectionSelected: (Offset) -> Unit = {},
    onIntersectionDismiss: () -> Unit = {}
) {
    // 제스처 감지 람다 내에서 최신 상태값을 참조하기 위해 rememberUpdatedState 사용
    val currentFunctions by rememberUpdatedState(functions)
    val currentIntersections by rememberUpdatedState(intersections)
    val currentOnIntersectionSelected by rememberUpdatedState(onIntersectionSelected)
    val currentOnIntersectionDismiss by rememberUpdatedState(onIntersectionDismiss)
    val currentOnVisibleRangeChange by rememberUpdatedState(onVisibleRangeChange)

    var trace by remember { mutableStateOf<TraceState?>(null) }

    // 보이는 범위가 바뀌면 제스처가 잠시 멈췄을 때 한 번만 알림 (교점 재계산 빈도 제한)
    LaunchedEffect(viewport) {
        snapshotFlow { viewport.visibleXRange() }
            .filterNotNull()
            .distinctUntilChanged()
            .debounce(VISIBLE_RANGE_DEBOUNCE_MS)
            .collect { range -> currentOnVisibleRangeChange(range.start, range.endInclusive) }
    }

    // Canvas 그리기 람다는 Composable 이 아니므로 테마 색상을 미리 읽어 둠
    val colors = LocalAppColors.current
    val paints = remember { GraphPaints() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 격자/라벨/곡선이 캔버스 영역 밖(상태바, 입력 패널)으로 그려지지 않도록 자름
                .clipToBounds()
                .background(colors.background)
                .onSizeChanged { viewport.onCanvasSizeChanged(it) }
                // 줌(Zoom) 및 팬(Pan) 제스처 처리
                .pointerInput(viewport) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        viewport.onGesture(centroid, pan, zoom)
                    }
                }
                // 길게 누른 뒤 드래그: 트레이스. 이동 제스처보다 안쪽에 두어 먼저 이벤트를 받고 소비함
                .pointerInput(viewport) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            trace = pickTraceFunction(currentFunctions, viewport, start)
                        },
                        onDrag = { change, _ ->
                            // 곡선을 잡았을 때만 이벤트를 소비 (못 잡았으면 일반 이동으로 동작)
                            val current = trace ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            trace = current.copy(x = viewport.toGraphX(change.position.x))
                        }
                    )
                }
                // 탭(Tap) 제스처 처리 (교점 선택, 선택/트레이스 해제)
                .pointerInput(viewport) {
                    detectTapGestures { tapOffset ->
                        trace = null
                        val tappedIntersection = if (viewport.scale >= MIN_SCALE_FOR_INTERSECTIONS) {
                            // 탭한 화면 위치에서 허용 반경 안에 있는 교점 찾기
                            currentIntersections.find { point ->
                                val px = viewport.toScreenX(point.x.toDouble())
                                val py = viewport.toScreenY(point.y.toDouble())
                                val dx = tapOffset.x - px
                                val dy = tapOffset.y - py
                                sqrt(dx * dx + dy * dy) <= INTERSECTION_TAP_RADIUS
                            }
                        } else {
                            null
                        }

                        if (tappedIntersection != null) {
                            currentOnIntersectionSelected(tappedIntersection)
                        } else {
                            // 빈 공간을 탭했으면 선택 해제
                            currentOnIntersectionDismiss()
                        }
                    }
                }
        ) {
            paints.applyColors(colors)
            drawGrid(viewport, colors, paints)
            drawFunctions(viewport, functions)
            drawIntersections(viewport, intersections, selectedIntersection, colors, paints)
            trace?.let { drawTrace(viewport, functions, it, paints) }
        }

        ViewportButtons(
            onZoomIn = { viewport.zoomBy(ZOOM_STEP) },
            onZoomOut = { viewport.zoomBy(1 / ZOOM_STEP) },
            onReset = { viewport.reset() },
            modifier = Modifier
                .align(controlsAlignment)
                .padding(12.dp)
        )
    }
}

/** 확대, 축소, 원점으로 버튼 */
@Composable
private fun ViewportButtons(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ViewportButton(Icons.Default.Add, "확대", onZoomIn)
        ViewportButton(Icons.Default.Remove, "축소", onZoomOut)
        ViewportButton(Icons.Default.CenterFocusStrong, "원점으로", onReset)
    }
}

@Composable
private fun ViewportButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = AppColors.Panel.copy(alpha = 0.9f),
        contentColor = AppColors.TextPrimary,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}

/** 그래프 위 글자를 그리는 Paint 모음 (매 프레임 새로 만들지 않도록 재사용) */
private class GraphPaints {
    // x축 눈금, 좌표 상자 글자 (가운데 정렬)
    val centerText = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 30f
        textAlign = android.graphics.Paint.Align.CENTER
    }

    // y축 눈금 (오른쪽 정렬)
    val rightText = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 30f
        textAlign = android.graphics.Paint.Align.RIGHT
    }

    // 좌표 표시 상자 배경
    val tooltipBackground = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
    }

    fun applyColors(colors: AppColorScheme) {
        centerText.color = colors.textPrimary.toArgb()
        rightText.color = colors.textPrimary.toArgb()
        tooltipBackground.color = colors.tooltipBackground.toArgb()
    }
}

/**
 * 격자, 축, 눈금 라벨을 그립니다.
 * 축이 화면 밖으로 나가면 라벨은 화면 가장자리에 붙여서 계속 보이게 합니다.
 */
private fun DrawScope.drawGrid(viewport: GraphViewportState, colors: AppColorScheme, paints: GraphPaints) {
    val width = size.width
    val height = size.height
    val originX = viewport.toScreenX(0.0)
    val originY = viewport.toScreenY(0.0)
    val gridStep = GraphGridCalculator.calculateGridStep(viewport.scale)

    val xLines = GraphGridCalculator.calculateVisibleGridLines(
        minGraphVal = viewport.toGraphX(0f),
        maxGraphVal = viewport.toGraphX(width),
        gridStep = gridStep
    )
    // 화면 y 는 아래로 증가하므로 위쪽 끝이 큰 값
    val yLines = GraphGridCalculator.calculateVisibleGridLines(
        minGraphVal = (originY - height) / viewport.scale.toDouble(),
        maxGraphVal = originY / viewport.scale.toDouble(),
        gridStep = gridStep
    )

    xLines.forEach { x ->
        val px = viewport.toScreenX(x)
        drawLine(colors.gridColor, Offset(px, 0f), Offset(px, height), strokeWidth = 1f)
    }
    yLines.forEach { y ->
        val py = viewport.toScreenY(y)
        drawLine(colors.gridColor, Offset(0f, py), Offset(width, py), strokeWidth = 1f)
    }

    // 축
    drawLine(colors.axisColor, Offset(0f, originY), Offset(width, originY), strokeWidth = 2f)
    drawLine(colors.axisColor, Offset(originX, 0f), Offset(originX, height), strokeWidth = 2f)

    val canvas = drawContext.canvas.nativeCanvas
    // x 눈금: x축 바로 아래, 축이 화면 밖이면 위/아래 가장자리
    val xLabelY = (originY + 40f).coerceIn(30f + LABEL_MARGIN, height - LABEL_MARGIN)
    xLines.forEach { x ->
        if (abs(x) < gridStep / 2) return@forEach // 원점은 생략
        canvas.drawText(GraphGridCalculator.formatLabel(x, gridStep), viewport.toScreenX(x), xLabelY, paints.centerText)
    }
    // y 눈금: y축 바로 왼쪽, 축이 화면 밖이면 왼쪽/오른쪽 가장자리
    yLines.forEach { y ->
        if (abs(y) < gridStep / 2) return@forEach
        val label = GraphGridCalculator.formatLabel(y, gridStep)
        val textWidth = paints.rightText.measureText(label)
        val labelX = (originX - 12f).coerceIn(textWidth + LABEL_MARGIN, width - LABEL_MARGIN)
        canvas.drawText(label, labelX, viewport.toScreenY(y) + 10f, paints.rightText)
    }
}

/** 보이는 함수들의 곡선을 적응형 샘플링으로 그립니다. */
private fun DrawScope.drawFunctions(viewport: GraphViewportState, functions: List<GraphFunction>) {
    val sampler = CurveSampler(
        width = size.width,
        height = size.height,
        toGraphX = viewport::toGraphX,
        toScreenY = viewport::toScreenY
    )
    functions.filter { it.isVisible }.forEach { function ->
        val path = Path()
        sampler.sample(function::evaluate).forEach { segment ->
            path.moveTo(segment.first().x, segment.first().y)
            for (i in 1 until segment.size) path.lineTo(segment[i].x, segment[i].y)
        }
        drawPath(path = path, color = Color(function.color), style = Stroke(width = 3f))
    }
}

/** 교점과, 선택된 교점의 좌표를 그립니다. 충분히 확대했을 때만 표시합니다. (혼잡도 방지) */
private fun DrawScope.drawIntersections(
    viewport: GraphViewportState,
    intersections: List<Offset>,
    selectedIntersection: Offset?,
    colors: AppColorScheme,
    paints: GraphPaints
) {
    if (viewport.scale < MIN_SCALE_FOR_INTERSECTIONS) return
    intersections.forEach { point ->
        val px = viewport.toScreenX(point.x.toDouble())
        val py = viewport.toScreenY(point.y.toDouble())
        // 화면 밖의 교점은 그리지 않음 (약간의 여유분 포함)
        if (px < -20f || px > size.width + 20f || py < -20f || py > size.height + 20f) return@forEach

        val isSelected = selectedIntersection?.let {
            abs(it.x - point.x) < 0.001f && abs(it.y - point.y) < 0.001f
        } ?: false

        // 교점 포인트 그리기 (선택되면 더 크고 강조색)
        drawCircle(colors.background, radius = if (isSelected) 12f else 8f, center = Offset(px, py))
        drawCircle(
            color = if (isSelected) colors.primaryGold else colors.red500,
            radius = if (isSelected) 8f else 5f,
            center = Offset(px, py)
        )
        if (isSelected) {
            drawCoordinateLabel(point.x.toDouble(), point.y.toDouble(), Offset(px, py), viewport.scale, paints)
        }
    }
}

/** 트레이스 중인 곡선 위의 점과 좌표를 그립니다. */
private fun DrawScope.drawTrace(
    viewport: GraphViewportState,
    functions: List<GraphFunction>,
    trace: TraceState,
    paints: GraphPaints
) {
    val function = functions.find { it.id == trace.functionId && it.isVisible } ?: return
    val y = function.evaluate(trace.x)
    if (!y.isFinite()) return
    val point = Offset(viewport.toScreenX(trace.x), viewport.toScreenY(y))

    drawCircle(Color(function.color).copy(alpha = 0.25f), radius = 22f, center = point)
    drawCircle(Color(function.color), radius = 9f, center = point)
    drawCoordinateLabel(trace.x, y, point, viewport.scale, paints)
}

/**
 * 점 위에 "(x, y)" 좌표 상자를 그립니다. 화면 위쪽 끝이면 점 아래에 그립니다.
 * 확대할수록 더 많은 소수 자릿수를 보여줍니다.
 */
private fun DrawScope.drawCoordinateLabel(x: Double, y: Double, point: Offset, scale: Float, paints: GraphPaints) {
    val decimals = floor(log10(scale.toDouble())).toInt().coerceIn(2, 6)
    val text = "(%.${decimals}f, %.${decimals}f)".format(x, y)
    val textWidth = paints.centerText.measureText(text)
    val padding = 10f
    val boxHeight = 44f

    val centerX = point.x.coerceIn(textWidth / 2 + padding, size.width - textWidth / 2 - padding)
    val above = point.y - 24f - boxHeight > 0
    val top = if (above) point.y - 24f - boxHeight else point.y + 24f

    val canvas = drawContext.canvas.nativeCanvas
    canvas.drawRoundRect(
        centerX - textWidth / 2 - padding, top,
        centerX + textWidth / 2 + padding, top + boxHeight,
        12f, 12f,
        paints.tooltipBackground
    )
    canvas.drawText(text, centerX, top + boxHeight - 13f, paints.centerText)
}

/** 길게 누른 위치에서 세로로 가장 가까운 곡선을 고릅니다. 반경 안에 없으면 null */
private fun pickTraceFunction(
    functions: List<GraphFunction>,
    viewport: GraphViewportState,
    touch: Offset
): TraceState? {
    val x = viewport.toGraphX(touch.x)
    return functions
        .filter { it.isVisible }
        .mapNotNull { function ->
            val y = function.evaluate(x)
            if (!y.isFinite()) return@mapNotNull null
            function to abs(viewport.toScreenY(y) - touch.y)
        }
        .filter { (_, distance) -> distance <= TRACE_PICK_RADIUS }
        .minByOrNull { (_, distance) -> distance }
        ?.let { (function, _) -> TraceState(function.id, x) }
}

@Preview
@Composable
private fun GraphCanvasPreview() {
    GraphCanvas()
}
