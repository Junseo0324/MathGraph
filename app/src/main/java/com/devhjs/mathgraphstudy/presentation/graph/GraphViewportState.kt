package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * 그래프 화면의 확대/이동 상태입니다.
 *
 * 제스처마다 바뀌는 값이라 ViewModel 이 아닌 Compose 상태로 들고 있어,
 * 드래그 중에는 캔버스만 다시 그려지고 다른 UI 는 재구성되지 않습니다.
 * ViewModel 에는 화면에 보이는 x 범위만 디바운스해서 전달합니다. (교점 계산용)
 *
 * @property scale 1 단위당 픽셀 수
 * @property offsetX 원점이 캔버스 중앙에서 떨어진 거리 (px)
 * @property offsetY 원점이 캔버스 중앙에서 떨어진 거리 (px)
 */
@Stable
class GraphViewportState(
    scale: Float = DEFAULT_SCALE,
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {
    var scale by mutableFloatStateOf(scale)
        private set
    var offsetX by mutableFloatStateOf(offsetX)
        private set
    var offsetY by mutableFloatStateOf(offsetY)
        private set

    /** 현재 캔버스 크기. 측정 전에는 [IntSize.Zero] */
    var canvasSize by mutableStateOf(IntSize.Zero)
        private set

    /**
     * 캔버스 크기가 바뀌면(입력 패널 열고 닫기 등) 원점의 화면상 위치가 유지되도록 보정합니다.
     * 원점은 캔버스 중앙 + offset 이므로, 중앙이 움직인 만큼 offset 을 반대로 옮깁니다.
     */
    fun onCanvasSizeChanged(size: IntSize) {
        val previous = canvasSize
        if (previous != IntSize.Zero && previous != size) {
            offsetX += (previous.width - size.width) / 2f
            offsetY += (previous.height - size.height) / 2f
        }
        canvasSize = size
    }

    /**
     * 핀치/드래그 제스처를 적용합니다.
     * 손가락 중심([centroid]) 아래의 그래프 좌표가 줌 전후로 같은 위치에 머물도록 원점을 보정합니다.
     */
    fun onGesture(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val appliedZoom = newScale / scale

        // 화면 중심 기준 centroid 위치에서, 원점까지의 거리를 줌 배율만큼 늘이거나 줄임
        val anchorX = centroid.x - canvasSize.width / 2f
        val anchorY = centroid.y - canvasSize.height / 2f
        offsetX = anchorX - (anchorX - offsetX) * appliedZoom + pan.x
        offsetY = anchorY - (anchorY - offsetY) * appliedZoom + pan.y
        scale = newScale
    }

    /** 화면에 보이는 x 범위. 캔버스 측정 전에는 null */
    fun visibleXRange(): ClosedFloatingPointRange<Double>? {
        if (canvasSize == IntSize.Zero) return null
        val halfWidth = canvasSize.width / 2.0
        val start = (-halfWidth - offsetX) / scale
        val end = (halfWidth - offsetX) / scale
        return start..end
    }

    companion object {
        const val DEFAULT_SCALE = 40f
        const val MIN_SCALE = 10f
        const val MAX_SCALE = 500f

        val Saver = listSaver<GraphViewportState, Float>(
            save = { listOf(it.scale, it.offsetX, it.offsetY) },
            restore = { GraphViewportState(it[0], it[1], it[2]) }
        )
    }
}

@Composable
fun rememberGraphViewportState(): GraphViewportState =
    rememberSaveable(saver = GraphViewportState.Saver) { GraphViewportState() }
