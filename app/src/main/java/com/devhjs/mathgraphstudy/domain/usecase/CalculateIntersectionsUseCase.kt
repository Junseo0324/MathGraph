package com.devhjs.mathgraphstudy.domain.usecase

/**
 * 화면에 표시된 함수들의 교차점(Intersections)을 계산하는 유즈케이스입니다.
 *
 * 두 함수의 차이(f1(x) - f2(x))를 구하고, 부호가 바뀌는 구간을 찾은 뒤
 * 이분 탐색(Bisection method)을 통해 정밀한 교차점을 찾아냅니다.
 */
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import javax.inject.Inject
import kotlin.math.abs

class CalculateIntersectionsUseCase @Inject constructor() {

    /**
     * 주어진 범위 내에서 활성화된 함수들의 모든 교차점을 찾아 반환합니다.
     *
     * @param functions 검사할 함수 목록 (isVisible이 true인 것만 계산)
     * @param rangeStart 검사할 x축 시작 범위 (Viewport minX)
     * @param rangeEnd 검사할 x축 끝 범위 (Viewport maxX)
     * @param step 구간을 나눌 간격 (기본값 0.1). 작을수록 정밀하지만 성능이 떨어질 수 있습니다.
     * @return 교차점의 (x, y) 좌표 리스트
     */
    operator fun invoke(
        functions: List<GraphFunction>,
        rangeStart: Double,
        rangeEnd: Double,
        step: Double = 0.1
    ): List<Pair<Double, Double>> {
        val visibleFunctions = functions.filter { it.isVisible }
        if (visibleFunctions.size < 2) return emptyList()

        val intersections = mutableListOf<Pair<Double, Double>>()

        for (i in visibleFunctions.indices) {
            for (j in i + 1 until visibleFunctions.size) {
                val f1 = visibleFunctions[i]
                val f2 = visibleFunctions[j]

                var x = rangeStart
                while (x < rangeEnd) {
                    val y1_a = f1.calculate(x) // 함수 1의 높이
                    val y2_a = f2.calculate(x) // 함수 2의 높이
                    val diff_a = y1_a - y2_a // 차이 (a)

                    val nextX = x + step
                    val y1_b = f1.calculate(nextX) // 함수 1의 높이
                    val y2_b = f2.calculate(nextX) // 함수 2의 높이
                    val diff_b = y1_b - y2_b // 차이 (b)

                    // 그 전 step 과 비교해서 부호가 다르면 교차했는지 확인
                    if (diff_a * diff_b <= 0.0) {
                        // 교차했는지 확인
                        val rootX = bisection(f1, f2, x, nextX) // 이분 탐색으로 교차점 x 찾기
                        val rootY = f1.calculate(rootX) // 그때의 y 값

                        // 실제 교차점이 있는지 체크하기
                        if (abs(f1.calculate(rootX) - f2.calculate(rootX)) < 1e-3) {
                            // 리스트에 이미 가까운 값이 있다면 등록하지 않음. (중복 방지)
                            val existing = intersections.find {
                                abs(it.first - rootX) < 0.2 && abs(it.second - rootY) < 0.2
                            }
                            if (existing == null) {
                                intersections.add(rootX to rootY)
                            }
                        }
                    }
                    x = nextX
                }
            }
        }
        return intersections
    }

    /**
     * 이분 탐색법(Bisection Method)을 사용하여 두 함수가 만나는 x좌표(해)를 정밀하게 근사합니다.
     *
     * 두 함수의 차이 `diff(x) = f1(x) - f2(x)`가 0이 되는 지점을 찾습니다.
     * 부호가 다른 두 지점 a, b 사이에서 중간값(mid)을 계속 좁혀가며 해를 찾습니다.
     *
     * @param a 구간 시작
     * @param b 구간 끝
     * @param tol 허용 오차 (Tolerance)
     */
    private fun bisection(f1: GraphFunction, f2: GraphFunction, a: Double, b: Double, tol: Double = 1e-5): Double {
        var low = a
        var high = b
        var mid = (low + high) / 2.0

        repeat(20) { // Max iterations
            val diffMid = f1.calculate(mid) - f2.calculate(mid)

            if (abs(diffMid) < tol) return mid

            val diffLow = f1.calculate(low) - f2.calculate(low)
            if (diffLow * diffMid < 0) {
                high = mid
            } else {
                low = mid
            }
            mid = (low + high) / 2.0
        }
        return mid
    }
}
