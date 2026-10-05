package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.ExpressionError
import com.devhjs.mathgraphstudy.domain.model.FunctionInput
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import com.devhjs.mathgraphstudy.domain.model.math.toExpressionNode
import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildFunctionNodeUseCaseTest {

    private val buildFunctionNode = BuildFunctionNodeUseCase(MathParser())

    @Test
    fun testExpressionTextBuildsNode() {
        // When: "2 + x"
        val result = buildFunctionNode(FunctionInput.Expression("2 + x"))

        // Then: 2 + x 트리
        assertEquals(Result.Success(BinaryOpNode(NumberNode("2"), MathOperator.PLUS, VariableNode("x"))), result)
    }

    @Test
    fun testEmptyOrIncompleteExpressionFails() {
        // Given & When & Then: 빈 수식, 연산자 뒤가 빈 수식, 빈 함수 괄호
        assertEquals(Result.Error(ExpressionError.EMPTY), buildFunctionNode(FunctionInput.Expression("  ")))
        assertEquals(Result.Error(ExpressionError.INVALID_EXPRESSION), buildFunctionNode(FunctionInput.Expression("2 +")))
        assertEquals(Result.Error(ExpressionError.INVALID_EXPRESSION), buildFunctionNode(FunctionInput.Expression("sin( )")))
    }

    @Test
    fun testTemplateBuildsNode() {
        // Given: 이차함수 y = x^2 - 3
        val input = FunctionInput.Template(BeginnerFunctionType.QUADRATIC, mapOf("c" to "-3"))

        // When
        val result = buildFunctionNode(input)

        // Then: x=2 에서 1
        assertTrue(result is Result.Success)
        val node = (result as Result.Success).data
        assertEquals(1.0, node.toExpressionNode().evaluate(2.0), 0.001)
    }

    @Test
    fun testTemplateWithInvalidCoefficientFails() {
        // Given: 숫자가 아닌 계수
        val input = FunctionInput.Template(BeginnerFunctionType.LINEAR, mapOf("a" to "abc"))

        // When & Then
        assertEquals(Result.Error(ExpressionError.INVALID_COEFFICIENT), buildFunctionNode(input))
    }
}
