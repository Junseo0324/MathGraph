package com.devhjs.mathgraphstudy.presentation.math

import com.devhjs.mathgraphstudy.domain.model.math.toVisualNode
import com.devhjs.mathgraphstudy.domain.service.MathParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressionEditorTest {

    private val parser = MathParser()

    /** 빈 입력기에서 키를 차례로 누릅니다. */
    private fun type(vararg keys: String, from: EditorState = EditorState()): EditorState =
        keys.fold(from) { state, key -> ExpressionEditor.input(state, key) }

    /** 입력기 내용을 파서로 계산합니다. */
    private fun EditorState.evaluate(x: Double, params: Map<String, Double> = emptyMap()): Double =
        parser.parseToNode(ExpressionEditor.toParserText(tokens)).evaluate(x, params)

    @Test
    fun testTypeNaturallyWithoutSlots() {
        // When: x ^ 2 + 1 을 그대로 입력 (지수 칸을 빠져나올 필요 없음)
        val state = type("x", "^", "2", "+", "1")

        // Then: x^2 + 1
        assertEquals(listOf("x", "^", "2", "+", "1"), state.tokens)
        assertEquals(10.0, state.evaluate(3.0), 1e-9)
    }

    @Test
    fun testFunctionInsertsClosingParen() {
        // When: 3 sin x
        val state = type("3", "sin", "x")

        // Then: 닫는 괄호가 자동으로 들어가고 커서는 괄호 안
        assertEquals(listOf("3", "sin(", "x", ")"), state.tokens)
        assertEquals(3, state.cursor)

        // When: → 로 괄호를 빠져나와 + 1
        val done = type(ExpressionEditor.INPUT_RIGHT, "+", "1", from = state)
        assertEquals(listOf("3", "sin(", "x", ")", "+", "1"), done.tokens)
    }

    @Test
    fun testUnclosedParenIsClosedAutomatically() {
        // Given: 사용자가 직접 연 괄호를 닫지 않음
        val tokens = listOf("2", "(", "x", "+", "1")

        // When & Then: 끝에 자동으로 닫음
        assertEquals("2 ( x + 1 )", ExpressionEditor.toParserText(tokens))
    }

    @Test
    fun testTapToMoveCursorAndInsertInMiddle() {
        // Given: x + 1
        val state = type("x", "+", "1")

        // When: x 뒤(인덱스 1)를 탭하고 ^2 입력
        val moved = ExpressionEditor.moveCursor(state, 1)
        val edited = type("^", "2", from = moved)

        // Then: x^2 + 1
        assertEquals(listOf("x", "^", "2", "+", "1"), edited.tokens)
        assertEquals(3, edited.cursor)
    }

    @Test
    fun testDeleteFunctionRemovesItsClosingParen() {
        // Given: sin(x) 에서 커서를 sin( 바로 뒤로
        val state = ExpressionEditor.moveCursor(type("sin", "x"), 1)

        // When: 지움
        val deleted = type(ExpressionEditor.INPUT_DELETE, from = state)

        // Then: 안쪽 x 만 남음
        assertEquals(listOf("x"), deleted.tokens)
        assertEquals(0, deleted.cursor)
    }

    @Test
    fun testDeleteBeforeClosingParenMovesInside() {
        // Given: (x) 에서 커서가 맨 끝
        val state = ExpressionEditor.moveCursor(type(ExpressionEditor.INPUT_PAREN, "x"), 3)

        // When: 지움 -> 괄호를 지우지 않고 안쪽으로 이동
        val result = type(ExpressionEditor.INPUT_DELETE, from = state)
        assertEquals(listOf("(", "x", ")"), result.tokens)
        assertEquals(2, result.cursor)
    }

    @Test
    fun testParameterAndConstantsAreSeparated() {
        // When: a π x (문자들이 붙어 "apix" 로 읽히지 않아야 함)
        val state = type("a", "pi", "x")

        // Then: a·π·x
        assertEquals(2 * Math.PI * 3, state.evaluate(3.0, mapOf("a" to 2.0)), 1e-9)
    }

    @Test
    fun testRoundTripFromStoredNode() {
        // 저장된 트리 -> 토큰 -> 다시 해석했을 때 같은 함수여야 함
        val expressions = listOf(
            "x^2 - 3x + 1",
            "(x + 1)^2",
            "1 / (x - 2) + 1",
            "-x^2",
            "2 * (-x)",
            "a sin(b x + c)",
            "x - (x - 1)",
            "sqrt(x) + abs(x)",
            "2^(x + 1)"
        )
        val params = mapOf("a" to 2.0, "b" to 3.0, "c" to 0.5)
        expressions.forEach { expression ->
            val node = parser.parseToNode(expression).toVisualNode()
            val state = ExpressionEditor.fromNode(node)
            listOf(-1.5, 0.7, 2.3).forEach { x ->
                val expected = parser.parseToNode(expression).evaluate(x, params)
                assertEquals("$expression -> ${state.tokens}", expected, state.evaluate(x, params), 1e-9)
            }
        }
    }

    @Test
    fun testSuperscriptLevels() {
        // x^(2+1) + x^23 : 괄호 묶음과 여러 자리 숫자가 위첨자
        val tokens = listOf("x", "^", "(", "2", "+", "1", ")", "+", "x", "^", "2", "3")
        val levels = superscriptLevels(tokens).toList()
        assertEquals(listOf(0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1), levels)
    }
}
