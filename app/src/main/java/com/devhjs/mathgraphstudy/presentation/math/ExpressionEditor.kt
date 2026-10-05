package com.devhjs.mathgraphstudy.presentation.math

import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NegateNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.ParenNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator

/**
 * 수식 입력기의 상태입니다. 수식은 토큰의 한 줄 목록이고, 커서는 토큰 사이 위치입니다.
 *
 * 예: "2x^2 + sin(x)" -> ["2", "x", "^", "2", "+", "sin(", "x", ")"]
 *
 * @property cursor 커서 위치. 0 이면 맨 앞, tokens.size 면 맨 끝
 */
data class EditorState(
    val tokens: List<String> = emptyList(),
    val cursor: Int = 0
)

/**
 * 계산기처럼 한 줄로 수식을 입력하는 편집기 로직입니다.
 *
 * - 키를 누르면 커서 위치에 토큰을 넣습니다. (빈 칸을 채우는 방식이 아님)
 * - 함수(sin 등)와 괄호는 닫는 괄호를 함께 넣고 커서를 안쪽에 둡니다.
 * - 지수(^)는 바로 다음 숫자/문자/괄호 묶음에 적용됩니다. ("x^2+1" 을 그대로 입력)
 * - 완성할 때 닫히지 않은 괄호는 자동으로 닫습니다.
 */
object ExpressionEditor {

    const val INPUT_PAREN = "()"
    const val INPUT_LEFT = "←"
    const val INPUT_RIGHT = "→"
    const val INPUT_DELETE = "DEL"

    /** 키패드 입력 -> 함수 토큰 (여는 괄호 포함) */
    private val FUNCTION_TOKENS = mapOf(
        "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
        "log" to "log(", "ln" to "ln(", "√" to "sqrt(", "abs" to "abs("
    )

    private val OPERATORS = setOf("+", "-", "*", "/", "^")
    private val VARIABLES = setOf("x", "e", "pi") + Parameter.NAMES

    /** 여는 괄호 역할을 하는 토큰인지 ("(" 또는 "sin(" 같은 함수) */
    fun isOpener(token: String): Boolean = token.endsWith("(")

    fun isDigit(token: String): Boolean = token.length == 1 && (token[0].isDigit() || token[0] == '.')

    /** 키패드 입력 하나를 처리합니다. */
    fun input(state: EditorState, key: String): EditorState {
        val functionToken = FUNCTION_TOKENS[key]
        return when {
            key == INPUT_LEFT -> state.copy(cursor = (state.cursor - 1).coerceAtLeast(0))
            key == INPUT_RIGHT -> state.copy(cursor = (state.cursor + 1).coerceAtMost(state.tokens.size))
            key == INPUT_DELETE || key == "⌫" -> delete(state)
            functionToken != null -> insertPair(state, functionToken)
            key == INPUT_PAREN || key == "(" -> insertPair(state, "(")
            key == ")" -> {
                // 바로 뒤가 닫는 괄호면 새로 넣지 않고 넘어감 (코드 편집기와 같은 동작)
                if (state.tokens.getOrNull(state.cursor) == ")") state.copy(cursor = state.cursor + 1)
                else insert(state, ")")
            }
            key.length == 1 && (key[0].isDigit() || key == ".") -> insert(state, key)
            key in OPERATORS || key in VARIABLES -> insert(state, key)
            else -> state
        }
    }

    /** 수식을 탭한 위치로 커서를 옮깁니다. */
    fun moveCursor(state: EditorState, index: Int): EditorState =
        state.copy(cursor = index.coerceIn(0, state.tokens.size))

    private fun insert(state: EditorState, token: String): EditorState {
        val tokens = state.tokens.toMutableList().apply { add(state.cursor, token) }
        return EditorState(tokens, state.cursor + 1)
    }

    /** 여는 토큰과 닫는 괄호를 함께 넣고 커서를 그 사이에 둡니다. */
    private fun insertPair(state: EditorState, opener: String): EditorState {
        val tokens = state.tokens.toMutableList().apply {
            add(state.cursor, opener)
            add(state.cursor + 1, ")")
        }
        return EditorState(tokens, state.cursor + 1)
    }

    /**
     * 커서 앞 토큰을 지웁니다.
     * - 여는 괄호/함수를 지우면 짝이 되는 닫는 괄호도 함께 지움 (안쪽 내용은 남김)
     * - 닫는 괄호 앞에서 지우면 괄호를 지우지 않고 안쪽으로 커서만 이동
     */
    private fun delete(state: EditorState): EditorState {
        if (state.cursor == 0) return state
        val index = state.cursor - 1
        val token = state.tokens[index]
        if (token == ")") return state.copy(cursor = index)

        val tokens = state.tokens.toMutableList()
        if (isOpener(token)) {
            matchingClose(tokens, index)?.let { tokens.removeAt(it) }
        }
        tokens.removeAt(index)
        return EditorState(tokens, index)
    }

    /** [openIndex] 의 여는 토큰과 짝이 되는 닫는 괄호 위치. 없으면 null */
    fun matchingClose(tokens: List<String>, openIndex: Int): Int? {
        var depth = 0
        for (i in openIndex until tokens.size) {
            if (isOpener(tokens[i])) depth++
            if (tokens[i] == ")") {
                depth--
                if (depth == 0) return i
            }
        }
        return null
    }

    /**
     * 수식 파서([com.devhjs.mathgraphstudy.domain.service.MathParser])가 읽을 문자열로 바꿉니다.
     * 닫히지 않은 괄호는 끝에 자동으로 닫습니다.
     */
    fun toParserText(tokens: List<String>): String {
        val builder = StringBuilder()
        var depth = 0
        tokens.forEachIndexed { i, token ->
            // 숫자끼리는 붙여 쓰고(12.5), 나머지는 띄어 써서 문자 토큰이 섞이지 않게 함 (a pi -> "api" 방지)
            if (i > 0 && !(isDigit(token) && isDigit(tokens[i - 1]))) builder.append(' ')
            builder.append(token)
            if (isOpener(token)) depth++
            if (token == ")") depth--
        }
        repeat(depth.coerceAtLeast(0)) { builder.append(" )") }
        return builder.toString()
    }

    /** 저장된 수식 트리를 편집할 수 있도록 토큰 목록으로 바꿉니다. 커서는 맨 끝에 둡니다. */
    fun fromNode(node: VisualMathNode): EditorState {
        val tokens = toTokens(node)
        return EditorState(tokens, tokens.size)
    }

    // --- 수식 트리 -> 토큰 (필요한 곳에만 괄호) ---

    private const val PREC_ADD = 1
    private const val PREC_MUL = 2
    private const val PREC_NEG = 3
    private const val PREC_POW = 4
    private const val PREC_ATOM = 5

    private fun precedence(node: VisualMathNode): Int = when (node) {
        is BinaryOpNode -> if (node.op == MathOperator.PLUS || node.op == MathOperator.MINUS) PREC_ADD else PREC_MUL
        is NegateNode -> PREC_NEG
        is PowerNode -> PREC_POW
        else -> PREC_ATOM
    }

    private fun wrap(tokens: List<String>): List<String> = listOf("(") + tokens + ")"

    private fun toTokens(node: VisualMathNode): List<String> = when (node) {
        is NumberNode -> node.value.map { it.toString() }
        is VariableNode -> listOf(node.name)
        is ParenNode -> wrap(toTokens(node.inner))
        is FunctionNode -> {
            val opener = when (node.func) {
                MathFunction.SQRT -> "sqrt("
                MathFunction.ABS -> "abs("
                else -> "${node.func.symbol}("
            }
            listOf(opener) + toTokens(node.arg) + ")"
        }
        is NegateNode -> {
            val operand = toTokens(node.operand)
            listOf("-") + if (precedence(node.operand) < PREC_NEG) wrap(operand) else operand
        }
        is PowerNode -> {
            val base = toTokens(node.base)
            val exponent = toTokens(node.exponent)
            val baseTokens = if (precedence(node.base) < PREC_ATOM) wrap(base) else base
            // 지수가 숫자/문자 하나면 괄호 없이 (x^2), 아니면 괄호로 묶음 (x^(n+1))
            val exponentTokens = if (node.exponent is NumberNode || node.exponent is VariableNode) exponent else wrap(exponent)
            baseTokens + "^" + exponentTokens
        }
        is BinaryOpNode -> {
            val prec = precedence(node)
            val left = toTokens(node.left).let { if (precedence(node.left) < prec) wrap(it) else it }
            // 빼기/나누기의 오른쪽은 같은 우선순위도 괄호 필요 (a-(b+c), a/(b*c))
            // 곱하기/나누기 오른쪽의 음수도 괄호 필요 (2·(-x) 를 "2 - x" 로 읽지 않도록)
            val rightNeedsParens = precedence(node.right) < prec ||
                (precedence(node.right) == prec && (node.op == MathOperator.MINUS || node.op == MathOperator.DIVIDE)) ||
                (prec == PREC_MUL && node.right is NegateNode)
            val right = toTokens(node.right).let { if (rightNeedsParens) wrap(it) else it }
            val operator = when (node.op) {
                MathOperator.PLUS -> listOf("+")
                MathOperator.MINUS -> listOf("-")
                MathOperator.DIVIDE -> listOf("/")
                MathOperator.POWER -> listOf("^")
                // 숫자끼리 곱할 때만 × 표시, 나머지는 생략 (2x, a sin(x))
                MathOperator.MULTIPLY -> if (right.firstOrNull()?.let(::isDigit) == true) listOf("*") else emptyList()
            }
            left + operator + right
        }
        PlaceholderNode -> emptyList()
    }
}
