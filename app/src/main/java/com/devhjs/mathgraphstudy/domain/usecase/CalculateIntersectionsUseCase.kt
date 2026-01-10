package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import kotlin.math.abs

class CalculateIntersectionsUseCase {

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
                    val y1_a = f1.calculate(x)
                    val y2_a = f2.calculate(x)
                    val diff_a = y1_a - y2_a

                    val nextX = x + step
                    val y1_b = f1.calculate(nextX)
                    val y2_b = f2.calculate(nextX)
                    val diff_b = y1_b - y2_b

                    // Check if signs are different, OR if one of them is effectively zero
                    if (diff_a * diff_b <= 0.0) {
                        // Likely intersection
                        val rootX = bisection(f1, f2, x, nextX)
                        val rootY = f1.calculate(rootX)

                        // Validation: Is it really a root?
                        if (abs(f1.calculate(rootX) - f2.calculate(rootX)) < 1e-3) {
                            // Check if we already added a close point to avoid duplicates
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
