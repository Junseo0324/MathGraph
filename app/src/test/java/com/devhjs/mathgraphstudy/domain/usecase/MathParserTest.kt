package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.PI

class MathParserTest {

    private val parser = MathParser()

    @Test
    fun testBasicOperations() {
        val node = parser.parseToNode("2 + 2")
        assertEquals(4.0, node.evaluate(0.0), 0.001)
    }

    @Test
    fun testVariable() {
        val node = parser.parseToNode("x^2")
        assertEquals(4.0, node.evaluate(2.0), 0.001)
        assertEquals(9.0, node.evaluate(3.0), 0.001)
    }

    @Test
    fun testFunctions() {
        val node1 = parser.parseToNode("sin(0)")
        assertEquals(0.0, node1.evaluate(0.0), 0.001)
        
        val node2 = parser.parseToNode("cos(0)")
        assertEquals(1.0, node2.evaluate(0.0), 0.001)
    }

    @Test
    fun testComplicated() {
        // sin(x) + 1 at x=0 => 1
        val node = parser.parseToNode("sin(x) + 1")
        assertEquals(1.0, node.evaluate(0.0), 0.001)
    }

    @Test
    fun testImplicitMultiplication() {
        // 2x at x=3 => 6
        val node1 = parser.parseToNode("2x")
        assertEquals(6.0, node1.evaluate(3.0), 0.001)

        // 3sin(x) at x=pi/2 => 3
        val node2 = parser.parseToNode("3sin(x)")
        assertEquals(3.0, node2.evaluate(PI / 2), 0.001)
    }

    @Test
    fun testTokenize() {
        // "2x + 1" -> ["2", "x", "+", "1"]
        val result = parser.tokenize("2x + 1")
        assertEquals(listOf("2", "x", "+", "1"), result)

        // "sin(x)" -> ["sin", "(", "x", ")"]
        val result2 = parser.tokenize("sin(x)")
        assertEquals(listOf("sin", "(", "x", ")"), result2)
    }

    @Test
    fun testInsertImplicitMultiplicationStep() {
        // "2x" -> "2", "*", "x"
        val tokens = listOf("2", "x")
        val result = parser.insertImplicitMultiplication(tokens)
        assertEquals(listOf("2", "*", "x"), result)

        // "x(x+1)" -> "x", "*", "(", "x", "+", "1", ")"
        val tokens2 = listOf("x", "(", "x", "+", "1", ")")
        val result2 = parser.insertImplicitMultiplication(tokens2)
        assertEquals(listOf("x", "*", "(", "x", "+", "1", ")"), result2)
    }

    @Test
    fun testShuntingYard() {
        // "3 + 4" -> "3", "4", "+"
        val tokens = listOf("3", "+", "4")
        val result = parser.shuntingYard(tokens)
        assertEquals(listOf("3", "4", "+"), result)

        // "3 + 4 * 2" -> "3", "4", "2", "*", "+"
        val tokens2 = listOf("3", "+", "4", "*", "2")
        val result2 = parser.shuntingYard(tokens2)
        assertEquals(listOf("3", "4", "2", "*", "+"), result2)
        
        // "3 * (4 + 2)" -> "3", "4", "2", "+", "*"
        val tokens3 = listOf("3", "*", "(", "4", "+", "2", ")")
        val result3 = parser.shuntingYard(tokens3)
        assertEquals(listOf("3", "4", "2", "+", "*"), result3)
    }

    @Test
    fun testBuildAST() {
        // "3 4 +" -> BinaryOp(+, CONST(3), CONST(4))
        val rpn = listOf("3", "4", "+")
        val node = parser.buildAST(rpn)
        assertEquals(7.0, node.evaluate(0.0), 0.001) 
    }
}
