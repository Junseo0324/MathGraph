package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.KeyPoint
import com.devhjs.mathgraphstudy.domain.model.math.toVisualNode
import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class FindKeyPointsUseCaseTest {

    private val findKeyPoints = FindKeyPointsUseCase()
    private val parser = MathParser()

    private fun function(expression: String) = GraphFunction(
        id = expression,
        node = parser.parseToNode(expression).toVisualNode(),
        color = 0xFF000000
    )

    private fun List<KeyPoint>.ofType(type: KeyPoint.Type) = filter { it.type == type }

    @Test
    fun testQuadraticRootsAndMinimum() {
        // When: y = x^2 - 4 를 -5 ~ 5 에서
        val points = findKeyPoints(function("x^2 - 4"), -5.0, 5.0)

        // Then: 근 ±2, 극소 (0, -4), y절편 -4
        val roots = points.ofType(KeyPoint.Type.ROOT).map { it.x }
        assertEquals(2, roots.size)
        assertEquals(-2.0, roots[0], 1e-6)
        assertEquals(2.0, roots[1], 1e-6)
        val minimum = points.ofType(KeyPoint.Type.MINIMUM).single()
        assertEquals(0.0, minimum.x, 1e-4)
        assertEquals(-4.0, minimum.y, 1e-6)
        assertEquals(-4.0, points.ofType(KeyPoint.Type.Y_INTERCEPT).single().y, 1e-9)
    }

    @Test
    fun testTouchingRootIsFound() {
        // When: y = x^2 (부호가 바뀌지 않고 0 에 닿는 근)
        val points = findKeyPoints(function("x^2"), -3.0, 3.0)

        // Then: 극소이자 근
        assertEquals(1, points.ofType(KeyPoint.Type.ROOT).size)
        assertEquals(1, points.ofType(KeyPoint.Type.MINIMUM).size)
    }

    @Test
    fun testSineMaximumWithParameters() {
        // When: y = a·sin(x), a = 3 을 0 ~ π 에서
        val points = findKeyPoints(function("a*sin(x)"), 0.1, PI - 0.1, mapOf("a" to 3.0))

        // Then: 극대 (π/2, 3)
        val maximum = points.ofType(KeyPoint.Type.MAXIMUM).single()
        assertEquals(PI / 2, maximum.x, 1e-4)
        assertEquals(3.0, maximum.y, 1e-6)
    }

    @Test
    fun testTanAsymptoteIsNotRoot() {
        // When: y = tan x 를 1 ~ 2 에서 (π/2 의 점근선에서 부호가 바뀜)
        val points = findKeyPoints(function("tan(x)"), 1.0, 2.0)

        // Then: 근으로 잡지 않음
        assertTrue(points.ofType(KeyPoint.Type.ROOT).isEmpty())
    }
}
