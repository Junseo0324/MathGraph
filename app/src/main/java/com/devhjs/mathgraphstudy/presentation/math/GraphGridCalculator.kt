package com.devhjs.mathgraphstudy.presentation.math

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 그래프의 그리드(격자) 시스템을 계산하는 유틸리티 클래스입니다.
 *
 * 뷰포트의 줌 레벨(scale)과 위치(offset)에 따라
 * 1. 적절한 그리드 간격(Step)을 결정하고
 * 2. 화면에 보여져야 할 그리드 선들의 좌표 리스트를 계산하고
 * 3. 눈금 라벨 문자열을 만듭니다.
 *
 * UI 레이어에서 복잡한 계산 로직을 분리하기 위해 작성되었습니다.
 */
object GraphGridCalculator {

    // 그리드 선 하나당 최소 픽셀 간격 (너무 촘촘하지 않게 조정)
    private const val MIN_PIXELS_BETWEEN_GRID_LINES = 100f

    // 한 번에 그릴 수 있는 최대 그리드 선 개수 (안전장치)
    private const val MAX_GRID_LINES = 1000

    /**
     * 현재 줌 레벨에 최적화된 그리드 간격(Step)을 계산합니다.
     * 화면상 간격이 최소 100px 이 되는 가장 작은 1·2·5 × 10ⁿ 값을 고릅니다.
     * 예: ... 0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50 ...
     *
     * @param viewportScale 현재 화면의 줌 배율 (Pixels per Unit)
     * @return 계산된 그리드 간격
     */
    fun calculateGridStep(viewportScale: Float): Double {
        val rawStep = MIN_PIXELS_BETWEEN_GRID_LINES / viewportScale.toDouble()
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        // 부동소수점 오차로 2.0000001 처럼 살짝 넘는 경우를 같은 값으로 취급
        return listOf(1.0, 2.0, 5.0, 10.0)
            .map { it * magnitude }
            .first { it >= rawStep * (1 - 1e-9) }
    }

    /**
     * 현재 화면 영역(Viewport) 내에 보여져야 할 그리드 선들의 좌표를 계산합니다.
     * 누적 덧셈 오차가 생기지 않도록 정수 배수(k × step)로 계산합니다.
     *
     * @param minGraphVal 화면에 보이는 그래프 좌표의 최소값 (예: X축의 왼쪽 끝, Y축의 아래쪽 끝)
     * @param maxGraphVal 화면에 보이는 그래프 좌표의 최대값
     * @param gridStep calculateGridStep()으로 구한 그리드 간격
     * @return 화면에 그려야 할 좌표 값들의 리스트 (오름차순)
     */
    fun calculateVisibleGridLines(
        minGraphVal: Double,
        maxGraphVal: Double,
        gridStep: Double
    ): List<Double> {
        // 부동소수점 오차 감안하여 약간의 여유 허용
        val epsilon = gridStep * 1e-6
        val first = ceil((minGraphVal - epsilon) / gridStep).toLong()
        val last = floor((maxGraphVal + epsilon) / gridStep).toLong()
        if (last < first) return emptyList()

        return (first..minOf(last, first + MAX_GRID_LINES)).map { it * gridStep }
    }

    /**
     * 눈금 라벨 문자열을 만듭니다. 간격에 맞는 소수 자릿수만 표시합니다.
     * 예: step 0.5 -> "1.5", step 0.01 -> "0.03", step 2 -> "4"
     */
    fun formatLabel(value: Double, gridStep: Double): String {
        val decimals = (-floor(log10(gridStep))).toInt().coerceAtLeast(0)
        if (decimals == 0) {
            val rounded = value.roundToLong()
            return if (rounded == 0L) "0" else rounded.toString()
        }
        val text = "%.${decimals}f".format(value)
        // "-0.00" 같은 음수 0 표기 제거
        return if (abs(value) < gridStep * 1e-6) "0" else text
    }
}
