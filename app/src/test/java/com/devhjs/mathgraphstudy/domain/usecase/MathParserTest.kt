package com.devhjs.mathgraphstudy.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.PI

class MathParserTest {

    private val parser = MathParser()

    @Test
    fun testBasicOperations() {
        val fn = parser.parse("2 + 2")
        assertEquals(4.0, fn(0.0), 0.001)
    }

    @Test
    fun testVariable() {
        val fn = parser.parse("x^2")
        assertEquals(4.0, fn(2.0), 0.001)
        assertEquals(9.0, fn(3.0), 0.001)
    }

    @Test
    fun testFunctions() {
        val fn = parser.parse("sin(0)")
        assertEquals(0.0, fn(0.0), 0.001)
        
        val fn2 = parser.parse("cos(0)")
        assertEquals(1.0, fn2(0.0), 0.001)
    }

    @Test
    fun testComplicated() {
        // sin(x) + 1 at x=0 => 1
        val fn = parser.parse("sin(x) + 1")
        assertEquals(1.0, fn(0.0), 0.001)
    }

    @Test
    fun testImplicitMultiplication() {
        // 2x at x=3 => 6
        val fn = parser.parse("2x")
        assertEquals(6.0, fn(3.0), 0.001)

        // 3sin(x) at x=pi/2 => 3
        val fn2 = parser.parse("3sin(x)")
        assertEquals(3.0, fn2(PI / 2), 0.001)
    }
}
