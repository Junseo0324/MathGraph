package com.devhjs.mathgraphstudy.presentation.math

import kotlin.math.ceil

/**
 * 그래프의 그리드(격자) 시스템을 계산하는 유틸리티 클래스입니다.
 *
 * 뷰포트의 줌 레벨(scale)과 위치(offset)에 따라
 * 1. 적절한 그리드 간격(Step)을 결정하고
 * 2. 화면에 보여져야 할 그리드 선들의 좌표 리스트를 계산합니다.
 *
 * UI 레이어에서 복잡한 계산 로직을 분리하기 위해 작성되었습니다.
 */
object GraphGridCalculator {

    // 그리드 선 하나당 최소 픽셀 간격 (너무 촘촘하지 않게 조정)
    private const val MIN_PIXELS_BETWEEN_GRID_LINES = 100f

    /**
     * 현재 줌 레벨에 최적화된 그리드 간격(Step)을 계산합니다.
     * 예: 줌 레벨에 따라 1, 2, 5, 10, 20, 50... 단위로 자동 조정
     *
     * @param viewportScale 현재 화면의 줌 배율 (Pixels per Unit)
     * @return 계산된 그리드 간격 (예: 1, 2, 5, 10 ...)
     */
    fun calculateGridStep(viewportScale: Float): Int {
        // 화면상에서 그리드 선 사이가 최소 100px은 되도록 단위 크기를 역산
        val rawStep = MIN_PIXELS_BETWEEN_GRID_LINES / viewportScale

        var gridStep = 1
        val multipliers = listOf(2, 5, 10)
        var mIdx = 0

        // 계산된 rawStep보다 크거나 같은 정제된 값(1, 2, 5 unit)을 찾을 때까지 증가
        // (원래 while문 로직을 그대로 유지하되 안전장치 포함)
        while (gridStep < rawStep) {
            val multiplier = multipliers[mIdx % multipliers.size]

            // 2, 5, 10 순환 패턴 적용 (10 이후엔 20, 50, 100...)
            if (mIdx < multipliers.size) {
                 gridStep = multiplier
            } else {
                 var p10 = 1
                 var tempIdx = mIdx
                 // 10의 거듭제곱 계산 (안전하게 반복문 사용)
                 while (tempIdx >= 3) {
                     p10 *= 10
                     tempIdx -= 3
                 }
                 gridStep = multipliers[tempIdx] * p10
            }
            mIdx++

            // 안전장치: 비정상적으로 루프가 길어지는 것을 방지 (예: Scale이 극도로 작을 때)
            if (mIdx > 100) break
        }

        return gridStep
    }

    /**
     * 현재 화면 영역(Viewport) 내에 보여져야 할 그리드 선들의 좌표를 계산합니다.
     *
     * @param minGraphVal 화면에 보이는 그래프 좌표의 최소값 (예: X축의 왼쪽 끝, Y축의 아래쪽 끝)
     * @param maxGraphVal 화면에 보이는 그래프 좌표의 최대값
     * @param gridStep calculateGridStep()으로 구한 그리드 간격
     * @return 화면에 그려야 할 좌표 값들의 리스트 (오름차순)
     */
    fun calculateVisibleGridLines(
        minGraphVal: Float,
        maxGraphVal: Float,
        gridStep: Int
    ): List<Float> {
        val lines = mutableListOf<Float>()

        // 시작점 보정: min 값보다 크거나 같은 첫 번째 gridStep 배수 찾기
        // 예: min=3.2, step=2 -> start=4
        val startGridVal = (ceil(minGraphVal / gridStep) * gridStep).toInt()

        var currentVal = startGridVal

        // max 값까지 step만큼 증가하며 리스트에 추가
        // 안전장치: 혹시 모를 무한루프 방지 (최대 1000개 라인 제한)
        var safetyCount = 0
        while (currentVal <= maxGraphVal + (gridStep * 0.1f)) { // 부동소수점 오차 감안하여 약간의 여유 허용
             lines.add(currentVal.toFloat())
             currentVal += gridStep

             if (++safetyCount > 1000) break
        }

        return lines
    }
}