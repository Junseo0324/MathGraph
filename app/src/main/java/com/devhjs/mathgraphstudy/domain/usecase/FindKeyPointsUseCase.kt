package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.KeyPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

/**
 * 주어진 범위에서 함수의 특징점(근, y절편, 극대, 극소)을 수치적으로 찾습니다.
 *
 * - 근: 함수값의 부호가 바뀌는 구간을 이분 탐색. 점근선(tan x 등)에서 부호가 바뀌는 경우는 제외
 * - 극대/극소: 기울기(중앙 차분)의 부호가 바뀌는 구간을 이분 탐색
 *   (x² 처럼 극값이 0 에 닿는 경우 근으로도 함께 표시)
 * - y절편: x = 0 이 범위 안이고 함수값이 정의될 때
 */
class FindKeyPointsUseCase @Inject constructor() {

    operator fun invoke(
        function: GraphFunction,
        rangeStart: Double,
        rangeEnd: Double,
        params: Map<String, Double> = emptyMap()
    ): List<KeyPoint> {
        if (rangeEnd <= rangeStart) return emptyList()
        val step = (rangeEnd - rangeStart) / SEARCH_STEPS
        val f = { x: Double -> function.evaluate(x, params) }
        // 기울기 계산용 아주 작은 간격
        val h = step / 100
        val slope = { x: Double -> (f(x + h) - f(x - h)) / (2 * h) }

        // 화면에 보이는 y 범위 크기를 대략 추정 (근/극값 판정 허용 오차 기준)
        val samples = (0..SEARCH_STEPS).map { rangeStart + it * step }
        val values = samples.map(f)
        val yScale = values.filter { it.isFinite() }.maxOfOrNull { abs(it) }?.let { max(it, 1.0) } ?: 1.0
        val tolerance = yScale * 1e-6 + step

        val points = mutableListOf<KeyPoint>()
        fun add(point: KeyPoint) {
            // 거의 같은 위치의 같은 종류 점은 하나만 남김
            val duplicate = points.any {
                it.type == point.type && abs(it.x - point.x) < step * 2
            }
            if (!duplicate) points.add(point)
        }

        // y절편
        if (0.0 in rangeStart..rangeEnd) {
            val y0 = f(0.0)
            if (y0.isFinite()) add(KeyPoint(0.0, y0, KeyPoint.Type.Y_INTERCEPT))
        }

        for (i in 0 until SEARCH_STEPS) {
            val a = samples[i]
            val b = samples[i + 1]
            val fa = values[i]
            val fb = values[i + 1]
            if (!fa.isFinite() || !fb.isFinite()) continue

            // 근: 부호 변화
            if (fa == 0.0) {
                add(KeyPoint(a, 0.0, KeyPoint.Type.ROOT))
            } else if (fa * fb < 0) {
                val root = bisect(a, b, f)
                if (abs(f(root)) < tolerance) add(KeyPoint(root, 0.0, KeyPoint.Type.ROOT))
            }

            // 극값: 기울기 부호 변화
            // 샘플이 극값 위치에 정확히 떨어져 기울기가 0 이 되는 경우도 잡도록, 끝점의 0 은 이 구간에서 처리
            val sa = slope(a)
            val sb = slope(b)
            val slopeChanges = (sa > 0 && sb <= 0) || (sa < 0 && sb >= 0)
            if (sa.isFinite() && sb.isFinite() && slopeChanges) {
                val x = bisect(a, b, slope)
                val y = f(x)
                // 극값 양옆이 모두 정의되고, 점근선이 아닌(값이 급변하지 않는) 경우만
                if (y.isFinite() && abs(y - fa) < yScale && abs(y - fb) < yScale) {
                    add(KeyPoint(x, y, if (sa > 0) KeyPoint.Type.MAXIMUM else KeyPoint.Type.MINIMUM))
                    if (abs(y) < tolerance) add(KeyPoint(x, 0.0, KeyPoint.Type.ROOT))
                }
            }
        }
        return points.sortedBy { it.x }
    }

    /** [a, b] 에서 g 의 부호가 바뀌는 지점을 이분 탐색으로 찾습니다. */
    private fun bisect(a: Double, b: Double, g: (Double) -> Double): Double {
        var low = a
        var high = b
        val gLow = g(low)
        repeat(60) {
            val mid = (low + high) / 2
            val gMid = g(mid)
            if (gMid == 0.0) return mid
            if ((gMid > 0) == (gLow > 0)) low = mid else high = mid
        }
        return (low + high) / 2
    }

    companion object {
        /** 범위를 나누는 구간 수 */
        const val SEARCH_STEPS = 2000
    }
}
