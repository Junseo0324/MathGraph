package com.devhjs.mathgraphstudy.presentation.math

import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NegateNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.ParenNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import com.devhjs.mathgraphstudy.domain.model.math.toDisplayString
import org.junit.Assert.assertEquals
import org.junit.Test

class MathInputManagerTest {

    /** 빈 상태에서 키 입력을 차례로 적용합니다. */
    private fun type(vararg inputs: String, from: MathInputState = MathInputState()): MathInputState =
        inputs.fold(from) { state, input -> MathInputManager.processInput(state, input) }

    @Test
    fun testMinusOnEmptyCreatesNegation() {
        // When: 빈 칸에서 "-", "2", "x" 입력
        val state = type("-", "2", "x")

        // Then: -(2*x) 구조가 되어야 함
        val expected = NegateNode(BinaryOpNode(NumberNode("2"), MathOperator.MULTIPLY, VariableNode("x")))
        assertEquals(expected, state.rootNode)
    }

    @Test
    fun testOperatorClimbsOutOfNegation() {
        // When: -2x + 1 입력
        val state = type("-", "2", "x", "+", "1")

        // Then: 덧셈이 부호 바깥에 있어야 함 ((-2x) + 1)
        val root = state.rootNode as BinaryOpNode
        assertEquals(MathOperator.PLUS, root.op)
        assertEquals("-2x+1", root.toDisplayString())
    }

    @Test
    fun testParenGroupsExpression() {
        // When: 2 ( x + 1 → * 3
        val state = type("2", MathInputManager.INPUT_PAREN, "x", "+", "1",
            MathInputManager.INPUT_RIGHT, MathInputManager.INPUT_RIGHT, "*", "3")

        // Then: (2 * (x + 1)) * 3
        assertEquals("2(x+1)*3", state.rootNode.toDisplayString())
        val root = state.rootNode as BinaryOpNode
        assertEquals(MathOperator.MULTIPLY, root.op)
        assertEquals(NumberNode("3"), root.right)
    }

    @Test
    fun testFunctionAfterNumberMultipliesInsteadOfReplacing() {
        // When: 3 sin x
        val state = type("3", "sin", "x")

        // Then: 3은 지워지지 않고 3 * sin(x) 가 되어야 함
        val expected = BinaryOpNode(
            NumberNode("3"),
            MathOperator.MULTIPLY,
            FunctionNode(MathFunction.SIN, VariableNode("x"))
        )
        assertEquals(expected, state.rootNode)
        assertEquals(listOf(1, 0), state.focusPath)
    }

    @Test
    fun testDeleteOnPlaceholderRemovesOperator() {
        // Given: "2 + ?" 상태
        val state = type("2", "+")

        // When: 빈 칸에서 삭제
        val result = type(MathInputManager.INPUT_DELETE, from = state)

        // Then: "2"로 되돌아가고 포커스는 2에 위치
        assertEquals(NumberNode("2"), result.rootNode)
        assertEquals(emptyList<Int>(), result.focusPath)
    }

    @Test
    fun testDeleteOnPlaceholderRemovesFunction() {
        // Given: "sin(?)" 상태
        val state = type("sin")

        // When: 빈 칸에서 삭제
        val result = type(MathInputManager.INPUT_DELETE, from = state)

        // Then: 빈 수식으로 되돌아감
        assertEquals(PlaceholderNode, result.rootNode)
    }

    @Test
    fun testMoveFocusLeft() {
        // Given: x ^ 2 (포커스: 지수)
        val state = type("x", "^", "2")
        assertEquals(listOf(1), state.focusPath)

        // When & Then: ← 한 번이면 밑(base), 한 번 더면 전체
        val left1 = type(MathInputManager.INPUT_LEFT, from = state)
        assertEquals(listOf(0), left1.focusPath)
        val left2 = type(MathInputManager.INPUT_LEFT, from = left1)
        assertEquals(emptyList<Int>(), left2.focusPath)
        assertEquals(PowerNode(VariableNode("x"), NumberNode("2")), left2.rootNode)
    }

    @Test
    fun testSingleDecimalPoint() {
        // When: 1 . 5 . 2
        val state = type("1", ".", "5", ".", "2")

        // Then: 두 번째 소수점은 무시
        assertEquals(NumberNode("1.52"), state.rootNode)
    }

    @Test
    fun testParenOnEmpty() {
        // When: ( 입력
        val state = type(MathInputManager.INPUT_PAREN)

        // Then: 괄호 안 빈 칸에 포커스
        assertEquals(ParenNode(PlaceholderNode), state.rootNode)
        assertEquals(listOf(0), state.focusPath)
    }

    @Test
    fun testEndOfExpressionPathSelectsWholeTrailingFunction() {
        // Given: 2x + sin x
        val state = type("2", "x", "+", "sin", "x")

        // When: 편집용 끝 위치에서 "+ 1" 입력
        val path = MathInputManager.endOfExpressionPath(state.rootNode)
        val edited = type("+", "1", from = MathInputState(state.rootNode, path))

        // Then: sin 안이 아니라 수식 끝에 덧붙음
        assertEquals(listOf(1), path)
        assertEquals("2x+sin x+1", edited.rootNode.toDisplayString())
    }
}
