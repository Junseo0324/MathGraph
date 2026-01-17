package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.PI

class MathParserTest {

    private val parser = MathParser()

    @Test
    fun testBasicOperations() {
        // Given: 간단한 덧셈 수식
        val expression = "2 + 2"
        
        // When: 파싱 및 계산
        val node = parser.parseToNode(expression)
        
        // Then: 결과는 4.0이어야 함
        assertEquals(4.0, node.evaluate(0.0), 0.001)
    }

    @Test
    fun testVariable() {
        // Given: 변수가 포함된 수식 (x^2)
        val expression = "x^2"
        
        // When: 파싱 실행
        val node = parser.parseToNode(expression)
        
        // Then: x값에 따라 올바른 제곱값이 나와야 함
        assertEquals(4.0, node.evaluate(2.0), 0.001)
        assertEquals(9.0, node.evaluate(3.0), 0.001)
    }

    @Test
    fun testFunctions() {
        // Given: 삼각함수 수식들
        val expr1 = "sin(0)"
        val expr2 = "cos(0)"
        
        // When: 각각 파싱
        val node1 = parser.parseToNode(expr1)
        val node2 = parser.parseToNode(expr2)
        
        // Then: 수학적으로 올바른 값이어야 함
        assertEquals(0.0, node1.evaluate(0.0), 0.001)
        assertEquals(1.0, node2.evaluate(0.0), 0.001)
    }

    @Test
    fun testComplicated() {
        // Given: 복합 수식 (sin(x) + 1)
        val expression = "sin(x) + 1"
        
        // When: 파싱 실행
        val node = parser.parseToNode(expression)
        
        // Then: x=0일 때 결과는 1.0이어야 함
        assertEquals(1.0, node.evaluate(0.0), 0.001)
    }

    @Test
    fun testImplicitMultiplication() {
        // Given: 곱셈 기호가 생략된 수식들
        val expr1 = "2x"
        val expr2 = "3sin(x)"
        
        // When: 파싱 실행
        val node1 = parser.parseToNode(expr1)
        val node2 = parser.parseToNode(expr2)
        
        // Then: 암시적 곱셈이 잘 처리되어 계산되어야 함
        assertEquals(6.0, node1.evaluate(3.0), 0.001)
        assertEquals(3.0, node2.evaluate(PI / 2), 0.001)
    }

    @Test
    fun testTokenize() {
        // Given: 테스트할 수식 문자열들
        val expr1 = "2x + 1"
        val expr2 = "sin(x)"
        
        // When: 토큰화 수행
        val result1 = parser.tokenize(expr1)
        val result2 = parser.tokenize(expr2)
        
        // Then: 예상되는 토큰 리스트와 일치해야 함
        assertEquals(listOf("2", "x", "+", "1"), result1)
        assertEquals(listOf("sin", "(", "x", ")"), result2)
    }

    @Test
    fun testInsertImplicitMultiplicationStep() {
        // Given: 토큰화된 리스트들
        val tokens1 = listOf("2", "x")
        val tokens2 = listOf("x", "(", "x", "+", "1", ")")
        
        // When: 암시적 곱셈 추가 로직 수행
        val result1 = parser.insertImplicitMultiplication(tokens1)
        val result2 = parser.insertImplicitMultiplication(tokens2)
        
        // Then: 곱셈 기호(*)가 적절한 위치에 추가되어야 함
        assertEquals(listOf("2", "*", "x"), result1)
        assertEquals(listOf("x", "*", "(", "x", "+", "1", ")"), result2)
    }

    @Test
    fun testShuntingYard() {
        // Given: 중위 표기법 토큰 리스트들
        val tokens1 = listOf("3", "+", "4")
        val tokens2 = listOf("3", "+", "4", "*", "2")
        val tokens3 = listOf("3", "*", "(", "4", "+", "2", ")")
        
        // When: Shunting-yard 알고리즘 실행
        val result1 = parser.shuntingYard(tokens1)
        val result2 = parser.shuntingYard(tokens2)
        val result3 = parser.shuntingYard(tokens3)
        
        // Then: 올바른 후위 표기법 순서로 변환되어야 함
        assertEquals(listOf("3", "4", "+"), result1)
        assertEquals(listOf("3", "4", "2", "*", "+"), result2)
        assertEquals(listOf("3", "4", "2", "+", "*"), result3)
    }

    @Test
    fun testBuildAST() {
        // Given: 후위 표기법으로 정리된 토큰 리스트 ("3 4 +")
        val rpn = listOf("3", "4", "+")
        
        // When: AST(계산 트리) 생성
        val node = parser.buildAST(rpn)
        
        // Then: 트리를 계산했을 때 결과가 7.0이어야 함
        assertEquals(7.0, node.evaluate(0.0), 0.001) 
    }
}
