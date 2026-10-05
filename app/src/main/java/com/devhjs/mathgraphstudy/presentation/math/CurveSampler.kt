package com.devhjs.mathgraphstudy.presentation.math

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * 함수 곡선을 화면에 그리기 위한 점들을 적응형으로 샘플링합니다.
 *
 * 1. 화면 가로 [BASE_STEP_PX] 픽셀 간격으로 기본 샘플을 뽑습니다.
 * 2. 이웃한 두 점의 화면상 높이 차가 크면(가파른 구간) 구간을 반으로 나눠 더 촘촘히 계산합니다.
 * 3. 최대 깊이까지 나눠도 크게 튀면 불연속(점근선, 예: tan x, 1/x)으로 보고 선을 끊습니다.
 * 4. 정의역 경계(예: √x 의 x=0, ln x)는 이분 탐색으로 경계 가까이까지 곡선을 이어 그립니다.
 *
 * 결과는 끊긴 구간별 점 목록이며, 각 구간은 하나의 꺾은선으로 그리면 됩니다.
 */
class CurveSampler(
    private val width: Float,
    private val height: Float,
    private val toGraphX: (Float) -> Double,
    private val toScreenY: (Double) -> Float
) {

    private val segments = mutableListOf<List<Offset>>()
    private var current = mutableListOf<Offset>()

    /** 화면 좌표 한 점. 함수값이 정의되지 않으면 [y] 는 NaN */
    private class Sample(val px: Float, val y: Float) {
        val isValid get() = !y.isNaN()
    }

    fun sample(f: (Double) -> Double): List<List<Offset>> {
        segments.clear()
        current = mutableListOf()

        var prev = evaluate(f, 0f)
        var px = BASE_STEP_PX
        while (px <= width + BASE_STEP_PX) {
            val next = evaluate(f, px)
            connect(f, prev, next, depth = 0)
            prev = next
            px += BASE_STEP_PX
        }
        endSegment()
        return segments.toList()
    }

    private fun evaluate(f: (Double) -> Double, px: Float): Sample {
        val y = f(toGraphX(px))
        if (!y.isFinite()) return Sample(px, Float.NaN)
        // 화면 밖으로 크게 벗어난 값은 화면 위아래 한 화면 높이까지로 제한 (float 오버플로 방지)
        return Sample(px, toScreenY(y).coerceIn(-height, 2 * height))
    }

    /** 두 샘플 사이를 필요하면 나눠서 잇거나 끊습니다. */
    private fun connect(f: (Double) -> Double, a: Sample, b: Sample, depth: Int) {
        if (!a.isValid && !b.isValid) return

        if (a.isValid && b.isValid) {
            val jump = abs(b.y - a.y)
            // 두 점이 모두 화면 같은 쪽 바깥이면 보이지 않으므로 더 나누지 않음
            val bothOffscreen = (a.y < 0 && b.y < 0) || (a.y > height && b.y > height)
            if (jump <= SMOOTH_PX || bothOffscreen) {
                lineTo(a, b)
                return
            }
            if (depth >= MAX_DEPTH) {
                // 충분히 나눴는데도 화면 절반 이상 튀면 불연속으로 판단
                if (jump > height / 2) {
                    endSegment()
                } else {
                    lineTo(a, b)
                }
                return
            }
        } else if (depth >= MAX_DEPTH) {
            // 정의된 점과 정의되지 않은 점의 경계: 정의된 쪽까지만 그리고 끊음
            if (a.isValid) {
                lineTo(a, a)
                endSegment()
            }
            return
        }

        val mid = evaluate(f, (a.px + b.px) / 2)
        connect(f, a, mid, depth + 1)
        connect(f, mid, b, depth + 1)
    }

    private fun lineTo(a: Sample, b: Sample) {
        val start = Offset(a.px, a.y)
        if (current.isEmpty() || current.last() != start) {
            // 직전 구간과 이어지지 않으면 새 구간 시작
            if (current.isNotEmpty()) endSegment()
            current.add(start)
        }
        if (b !== a) current.add(Offset(b.px, b.y))
    }

    private fun endSegment() {
        if (current.size >= 2) segments.add(current)
        current = mutableListOf()
    }

    companion object {
        /** 기본 샘플 간격 (px) */
        const val BASE_STEP_PX = 2f

        /** 이 높이 차(px) 이하면 직선으로 이어도 매끄럽게 보임 */
        private const val SMOOTH_PX = 6f

        /** 구간을 반으로 나누는 최대 횟수 (2px / 2^7 ≈ 0.016px) */
        private const val MAX_DEPTH = 7
    }
}
