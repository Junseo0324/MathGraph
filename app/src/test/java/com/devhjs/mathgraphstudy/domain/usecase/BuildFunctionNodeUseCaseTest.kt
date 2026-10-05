package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.ExpressionError
import com.devhjs.mathgraphstudy.domain.model.FunctionInput
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import com.devhjs.mathgraphstudy.domain.model.math.toExpressionNode
import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildFunctionNodeUseCaseTest {

    private val buildFunctionNode = BuildFunctionNodeUseCase(MathParser())

    @Test
    fun testCompleteExpressionSucceeds() {
        // Given: 2 + x
        val node = BinaryOpNode(NumberNode("2"), MathOperator.PLUS, VariableNode("x"))

        // When
        val result = buildFunctionNode(FunctionInput.Expression(node))

        // Then: 입력한 트리를 그대로 반환
        assertEquals(Result.Success(node), result)
    }

    @Test
    fun testEmptyOrIncompleteExpressionFails() {
        // Given: 빈 수식, sin(?) 처럼 빈 칸이 남은 수식
        val empty = FunctionInput.Expression(PlaceholderNode)
        val incomplete = FunctionInput.Expression(FunctionNode(MathFunction.SIN, PlaceholderNode))

        // When & Then
        assertEquals(Result.Error(ExpressionError.EMPTY_SLOT), buildFunctionNode(empty))
        assertEquals(Result.Error(ExpressionError.EMPTY_SLOT), buildFunctionNode(incomplete))
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
