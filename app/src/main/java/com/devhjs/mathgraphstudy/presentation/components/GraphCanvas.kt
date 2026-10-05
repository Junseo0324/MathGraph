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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.KeyPoint
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColorScheme
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.LocalAppColors
import com.devhjs.mathgraphstudy.presentation.graph.GraphPoint
import com.devhjs.mathgraphstudy.presentation.graph.GraphViewportState
import com.devhjs.mathgraphstudy.presentation.graph.rememberGraphViewportState
import com.devhjs.mathgraphstudy.presentation.math.CurveSampler
import com.devhjs.mathgraphstudy.presentation.math.GraphGridCalculator
import com.devhjs.mathgraphstudy.presentation.util.GraphImageSharer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.sqrt

// 교점 표시를 위한 최소 줌 레벨 (이 값 이상일 때만 교점 표시)
private const val MIN_SCALE_FOR_INTERSECTIONS = 10f
// 교점/특징점 선택을 위한 터치 허용 반경 (픽셀 단위)
private const val POINT_TAP_RADIUS = 36f
// 곡선을 탭해 함수를 선택할 때의 허용 반경 (픽셀 단위)
private const val CURVE_TAP_RADIUS = 40f
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
 * - 탭: 교점/특징점 선택 (좌표 표시), 곡선 탭은 함수 선택 (특징점 표시), 빈 곳 탭은 선택/트레이스 해제
 * - 길게 누른 뒤 드래그: 가장 가까운 곡선을 따라가며 (x, y) 표시 (트레이스)
 * - 모서리 버튼: 확대, 축소, 원점으로, 이미지 공유
 *
 * 확대/이동 상태는 [viewport]에 직접 반영하고 그리기 단계에서만 읽으므로,
 * 드래그 중에는 컴포지션 없이 캔버스만 다시 그려집니다.
 *
 * @param viewport 확대/이동 상태
 * @param functions 그릴 그래프 함수들의 리스트 (수식, 색상, 가시성 포함)
 * @param parameterValues 매개변수 값 (이름 -> 값)
 * @param intersections 두 그래프 간의 교점 좌표 리스트
 * @param selectedFunctionId 곡선을 탭해 선택한 함수 (굵게 표시, 특징점 표시)
 * @param keyPoints 선택한 함수의 특징점
 * @param selectedPoint 현재 사용자가 선택한 교점/특징점 (선택 시 좌표값 표시)
 * @param controlsAlignment 확대/축소/원점 버튼 위치 (태블릿은 오른쪽 패널을 피해 왼쪽에 둠)
 * @param onVisibleRangeChange 화면에 보이는 x 범위가 바뀌었을 때 호출 (제스처가 멈춘 뒤 디바운스)
 * @param onFunctionSelected 곡선을 탭하면 그 함수 id, 빈 곳을 탭하면 null
 * @param onPointSelected 교점/특징점을 탭했을 때 호출되는 콜백
 * @param onPointDismiss 좌표 표시를 닫을 때 호출되는 콜백
 */
@OptIn(FlowPreview::class)
@Composable
fun GraphCanvas(
    modifier: Modifier = Modifier,
    viewport: GraphViewportState = rememberGraphViewportState(),
    functions: List<GraphFunction> = emptyList(),
    parameterValues: Map<String, Double> = emptyMap(),
    intersections: List<Offset> = emptyList(),
    selectedFunctionId: String? = null,
    keyPoints: List<KeyPoint> = emptyList(),
    selectedPoint: GraphPoint? = null,
    controlsAlignment: Alignment = Alignment.TopEnd,
    onVisibleRangeChange: (Double, Double) -> Unit = { _, _ -> },
    onFunctionSelected: (String?) -> Unit = {},
    onPointSelected: (GraphPoint) -> Unit = {},
    onPointDismiss: () -> Unit = {}
) {
    // 제스처 감지 람다 내에서 최신 상태값을 참조하기 위해 rememberUpdatedState 사용
    val currentFunctions by rememberUpdatedState(functions)
    val currentParams by rememberUpdatedState(parameterValues)
    val currentIntersections by rememberUpdatedState(intersections)
    val currentKeyPoints by rememberUpdatedState(keyPoints)
    val currentOnFunctionSelected by rememberUpdatedState(onFunctionSelected)
    val currentOnPointSelected by rememberUpdatedState(onPointSelected)
    val currentOnPointDismiss by rememberUpdatedState(onPointDismiss)
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

    // 공유용 이미지 캡처: 캔버스 그리기를 기록해 두었다가 공유 버튼을 누르면 비트맵으로 변환
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }
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
                            trace = pickFunctionAt(currentFunctions, currentParams, viewport, start, TRACE_PICK_RADIUS)
                                ?.let { TraceState(it.id, viewport.toGraphX(start.x)) }
                        },
                        onDrag = { change, _ ->
                            // 곡선을 잡았을 때만 이벤트를 소비 (못 잡았으면 일반 이동으로 동작)
                            val current = trace ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            trace = current.copy(x = viewport.toGraphX(change.position.x))
                        }
                    )
                }
                // 탭(Tap) 제스처 처리: 점 선택 > 곡선 선택 > 선택 해제 순서로 판단
                .pointerInput(viewport) {
                    detectTapGestures { tap ->
                        trace = null
                        val tappedPoint = findTappedPoint(
                            tap = tap,
                            viewport = viewport,
                            intersections = currentIntersections,
                            keyPoints = currentKeyPoints
                        )
                        if (tappedPoint != null) {
                            currentOnPointSelected(tappedPoint)
                            return@detectTapGestures
                        }
                        currentOnPointDismiss()
                        val function = pickFunctionAt(currentFunctions, currentParams, viewport, tap, CURVE_TAP_RADIUS)
                        currentOnFunctionSelected(function?.id)
                    }
                }
        ) {
            paints.applyColors(colors)
            drawGrid(viewport, colors, paints)
            drawFunctions(viewport, functions, parameterValues, selectedFunctionId)
            drawIntersections(viewport, intersections, colors)
            functions.find { it.id == selectedFunctionId }?.let { drawKeyPoints(viewport, keyPoints, Color(it.color), colors) }
            selectedPoint?.let { drawSelectedPoint(viewport, it, colors, paints) }
            trace?.let { drawTrace(viewport, functions, parameterValues, it, paints) }
        }

        ViewportButtons(
            onZoomIn = { viewport.zoomBy(ZOOM_STEP) },
            onZoomOut = { viewport.zoomBy(1 / ZOOM_STEP) },
            onReset = { viewport.reset() },
            onShare = {
                scope.launch { GraphImageSharer.share(context, graphicsLayer.toImageBitmap()) }
            },
            modifier = Modifier
                .align(controlsAlignment)
                .padding(12.dp)
        )
    }
}

/** 확대, 축소, 원점으로, 공유 버튼 */
@Composable
private fun ViewportButtons(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ViewportButton(Icons.Default.Add, "확대", onZoomIn)
        ViewportButton(Icons.Default.Remove, "축소", onZoomOut)
        ViewportButton(Icons.Default.CenterFocusStrong, "원점으로", onReset)
        ViewportButton(Icons.Default.Share, "그래프 이미지 공유", onShare)
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
private fun DrawScope.drawFunctions(
    viewport: GraphViewportState,
    functions: List<GraphFunction>,
    params: Map<String, Double>,
    selectedFunctionId: String?
) {
    val sampler = CurveSampler(
        width = size.width,
        height = size.height,
        toGraphX = viewport::toGraphX,
        toScreenY = viewport::toScreenY
    )
    functions.filter { it.isVisible }.forEach { function ->
        val path = Path()
        sampler.sample { x -> function.evaluate(x, params) }.forEach { segment ->
            path.moveTo(segment.first().x, segment.first().y)
            for (i in 1 until segment.size) path.lineTo(segment[i].x, segment[i].y)
        }
        // 선택한 함수는 굵게
        val strokeWidth = if (function.id == selectedFunctionId) 6f else 3f
        drawPath(path = path, color = Color(function.color), style = Stroke(width = strokeWidth))
    }
}

/** 교점을 그립니다. 충분히 확대했을 때만 표시합니다. (혼잡도 방지) */
private fun DrawScope.drawIntersections(
    viewport: GraphViewportState,
    intersections: List<Offset>,
    colors: AppColorScheme
) {
    if (viewport.scale < MIN_SCALE_FOR_INTERSECTIONS) return
    intersections.forEach { point ->
        val center = Offset(viewport.toScreenX(point.x.toDouble()), viewport.toScreenY(point.y.toDouble()))
        if (!isNearCanvas(center)) return@forEach
        drawCircle(colors.background, radius = 8f, center = center)
        drawCircle(colors.red500, radius = 5f, center = center)
    }
}

/** 선택한 함수의 특징점(근, y절편, 극대, 극소)을 함수 색 테두리 원으로 그립니다. */
private fun DrawScope.drawKeyPoints(
    viewport: GraphViewportState,
    keyPoints: List<KeyPoint>,
    color: Color,
    colors: AppColorScheme
) {
    keyPoints.forEach { point ->
        val center = Offset(viewport.toScreenX(point.x), viewport.toScreenY(point.y))
        if (!isNearCanvas(center)) return@forEach
        drawCircle(colors.background, radius = 9f, center = center)
        drawCircle(color, radius = 9f, center = center, style = Stroke(width = 4f))
    }
}

/** 선택한 점을 강조하고 "이름 (x, y)" 를 표시합니다. */
private fun DrawScope.drawSelectedPoint(
    viewport: GraphViewportState,
    point: GraphPoint,
    colors: AppColorScheme,
    paints: GraphPaints
) {
    val center = Offset(viewport.toScreenX(point.x), viewport.toScreenY(point.y))
    if (!isNearCanvas(center)) return
    drawCircle(colors.background, radius = 12f, center = center)
    drawCircle(colors.primaryGold, radius = 8f, center = center)
    drawCoordinateLabel(point.x, point.y, center, viewport.scale, paints, prefix = point.label)
}

/** 화면 안(약간의 여유 포함)에 있는 점인지 */
private fun DrawScope.isNearCanvas(point: Offset): Boolean =
    point.x >= -20f && point.x <= size.width + 20f && point.y >= -20f && point.y <= size.height + 20f

/** 트레이스 중인 곡선 위의 점과 좌표를 그립니다. */
private fun DrawScope.drawTrace(
    viewport: GraphViewportState,
    functions: List<GraphFunction>,
    params: Map<String, Double>,
    trace: TraceState,
    paints: GraphPaints
) {
    val function = functions.find { it.id == trace.functionId && it.isVisible } ?: return
    val y = function.evaluate(trace.x, params)
    if (!y.isFinite()) return
    val point = Offset(viewport.toScreenX(trace.x), viewport.toScreenY(y))

    drawCircle(Color(function.color).copy(alpha = 0.25f), radius = 22f, center = point)
    drawCircle(Color(function.color), radius = 9f, center = point)
    drawCoordinateLabel(trace.x, y, point, viewport.scale, paints)
}

/**
 * 점 위에 "(x, y)" 좌표 상자를 그립니다. 화면 위쪽 끝이면 점 아래에 그립니다.
 * 확대할수록 더 많은 소수 자릿수를 보여줍니다.
 *
 * @param prefix 좌표 앞에 붙일 이름 (예: "극대")
 */
private fun DrawScope.drawCoordinateLabel(
    x: Double,
    y: Double,
    point: Offset,
    scale: Float,
    paints: GraphPaints,
    prefix: String? = null
) {
    val decimals = floor(log10(scale.toDouble())).toInt().coerceIn(2, 6)
    val coordinates = "(%.${decimals}f, %.${decimals}f)".format(x, y)
    val text = if (prefix != null) "$prefix $coordinates" else coordinates
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

/** 터치 위치에서 세로로 가장 가까운 곡선(반경 [radius] px 안)을 고릅니다. 없으면 null */
private fun pickFunctionAt(
    functions: List<GraphFunction>,
    params: Map<String, Double>,
    viewport: GraphViewportState,
    touch: Offset,
    radius: Float
): GraphFunction? {
    val x = viewport.toGraphX(touch.x)
    return functions
        .filter { it.isVisible }
        .mapNotNull { function ->
            val y = function.evaluate(x, params)
            if (!y.isFinite()) return@mapNotNull null
            function to abs(viewport.toScreenY(y) - touch.y)
        }
        .filter { (_, distance) -> distance <= radius }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

/** 탭 위치 근처의 교점/특징점을 찾습니다. 교점은 충분히 확대했을 때만 대상입니다. */
private fun findTappedPoint(
    tap: Offset,
    viewport: GraphViewportState,
    intersections: List<Offset>,
    keyPoints: List<KeyPoint>
): GraphPoint? {
    fun isNear(x: Double, y: Double): Boolean {
        val dx = tap.x - viewport.toScreenX(x)
        val dy = tap.y - viewport.toScreenY(y)
        return sqrt(dx * dx + dy * dy) <= POINT_TAP_RADIUS
    }

    keyPoints.firstOrNull { isNear(it.x, it.y) }?.let { return GraphPoint(it.x, it.y, it.type.label()) }
    if (viewport.scale >= MIN_SCALE_FOR_INTERSECTIONS) {
        intersections.firstOrNull { isNear(it.x.toDouble(), it.y.toDouble()) }
            ?.let { return GraphPoint(it.x.toDouble(), it.y.toDouble(), "교점") }
    }
    return null
}

/** 특징점 종류의 화면 표시 이름 */
private fun KeyPoint.Type.label(): String = when (this) {
    KeyPoint.Type.ROOT -> "근"
    KeyPoint.Type.Y_INTERCEPT -> "y절편"
    KeyPoint.Type.MAXIMUM -> "극대"
    KeyPoint.Type.MINIMUM -> "극소"
}

@Preview
@Composable
private fun GraphCanvasPreview() {
    GraphCanvas()
}
