package com.devhjs.mathgraphstudy.domain.model.math

import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeginnerFunctionTypeTest {

    private val parser = MathParser()

    private fun evaluate(type: BeginnerFunctionType, coefficients: Map<String, String>, x: Double): Double {
        val expression = type.buildExpression(coefficients)!!
        return parser.parseToNode(expression).evaluate(x)
    }

    @Test
    fun testRationalIsReciprocal() {
        // Given: y = 2/(x+1) + 3
        val coefficients = mapOf("a" to "2", "b" to "1", "c" to "3")

        // When & Then: x=1 이면 2/2 + 3 = 4, x=3 이면 2/4 + 3 = 3.5
        assertEquals(4.0, evaluate(BeginnerFunctionType.RATIONAL, coefficients, 1.0), 0.001)
        assertEquals(3.5, evaluate(BeginnerFunctionType.RATIONAL, coefficients, 3.0), 0.001)
    }

    @Test
    fun testNegativeCoefficients() {
        // Given: y = -2x^2 - 3x - 1
        val coefficients = mapOf("a" to "-2", "b" to "-3", "c" to "-1")

        // When & Then: x=2 이면 -8 - 6 - 1 = -15
        assertEquals(-15.0, evaluate(BeginnerFunctionType.QUADRATIC, coefficients, 2.0), 0.001)
    }

    @Test
    fun testBlankCoefficientsUseDefaults() {
        // Given: 아무 계수도 입력하지 않은 경우 (a=1, 나머지=0)
        val coefficients = mapOf("b" to " ")

        // When & Then: y = x^3
        assertEquals(8.0, evaluate(BeginnerFunctionType.CUBIC, coefficients, 2.0), 0.001)
    }

    @Test
    fun testInvalidCoefficientReturnsNull() {
        // Given: 숫자가 아닌 계수
        val coefficients = mapOf("a" to "-", "b" to "1")

        // When & Then: 수식을 만들지 않음
        assertNull(BeginnerFunctionType.LINEAR.buildExpression(coefficients))
    }
}
