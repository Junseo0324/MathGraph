package com.devhjs.mathgraphstudy.domain.usecase


import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CalculateIntersectionsUseCaseTest {

    private val calculateIntersections = CalculateIntersectionsUseCase()

    @Test
    fun testLinearIntersection() {
        // Given: 교차하는 두 직선 함수 준비 (y = x, y = -x + 2)
        val f1 = GraphFunction(
            id = "1",
            expression = "x",
            calculate = { x -> x },
            color = 0xFF000000,
            isVisible = true
        )
        val f2 = GraphFunction(
            id = "2",
            expression = "-x + 2",
            calculate = { x -> -x + 2.0 },
            color = 0xFF0000FF,
            isVisible = true
        )

        // When: 교차점 계산 실행
        val result = calculateIntersections(listOf(f1, f2), -5.0, 5.0)
        
        // Then: 결과 검증 (1, 1)에서 교차해야 함
        assertEquals(1, result.size)
        assertEquals(1.0, result[0].first, 0.001) // x
        assertEquals(1.0, result[0].second, 0.001) // y
    }

    @Test
    fun testNoIntersection() {
        // Given: 평행한 두 직선 함수 준비 (y = x, y = x + 2)
        val f1 = GraphFunction(
            id = "1",
            expression = "x",
            calculate = { x -> x },
            color = 0xFF000000,
            isVisible = true
        )
        val f2 = GraphFunction(
            id = "2",
            expression = "x + 2",
            calculate = { x -> x + 2.0 },
            color = 0xFF0000FF,
            isVisible = true
        )

        // When: 교차점 계산 실행
        val result = calculateIntersections(listOf(f1, f2), -5.0, 5.0)

        // Then: 교차점이 없어야 함
        assertTrue(result.isEmpty())
    }

    @Test
    fun testQuadraticIntersection() {
        // Given: 이차 함수와 상수 함수 준비 (y = x^2, y = 4)
        val f1 = GraphFunction(
            id = "1",
            expression = "x^2",
            calculate = { x -> x * x },
            color = 0xFF000000,
            isVisible = true
        )
        val f2 = GraphFunction(
            id = "2",
            expression = "4",
            calculate = { 4.0 },
            color = 0xFF0000FF,
            isVisible = true
        )

        // When: 교차점 계산 실행
        val result = calculateIntersections(listOf(f1, f2), -5.0, 5.0)
        
        // Then: 두 지점(-2, 4), (2, 4)에서 교차해야 함
        assertEquals(2, result.size)
        
        val foundNegative2 = result.any { abs(it.first - (-2.0)) < 0.001 && abs(it.second - 4.0) < 0.001 }
        val foundPositive2 = result.any { abs(it.first - 2.0) < 0.001 && abs(it.second - 4.0) < 0.001 }
        
        assertTrue(foundNegative2)
        assertTrue(foundPositive2)
    }

    @Test
    fun testInvisibleFunctionIgnored() {
        // Given: 교차하지만 하나가 보이지 않는 두 함수 준비
        val f1 = GraphFunction(
            id = "1",
            expression = "x",
            calculate = { x -> x },
            color = 0xFF000000,
            isVisible = true
        )
        val f2 = GraphFunction(
            id = "2",
            expression = "-x",
            calculate = { x -> -x },
            color = 0xFF0000FF,
            isVisible = false // 보이지 않음
        )

        // When: 교차점 계산 실행
        val result = calculateIntersections(listOf(f1, f2), -5.0, 5.0)

        // Then: 교차점이 계산되지 않아야 함
        assertTrue(result.isEmpty())
    }
}
