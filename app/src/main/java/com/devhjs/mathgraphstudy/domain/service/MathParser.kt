package com.devhjs.mathgraphstudy.domain.service

import com.devhjs.mathgraphstudy.domain.model.math.ExpressionNode

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 문자열 형태의 수학 수식(예: "x^2 + 2x")을 파싱하여 계산 가능한 함수로 변환하는 클래스입니다.
 *
 * 주요 기능:
 * 1. 암시적 곱셈 처리 (예: 2x -> 2*x)
 * 2. Shunting-yard 알고리즘을 통한 후위 표기법 변환
 * 3. AST(Abstract Syntax Tree) 구성 및 계산
 */
import javax.inject.Inject

class MathParser @Inject constructor() {

    /**
     * 수식 문자열을 입력받아, 실수(Double) 값을 넣어 결과를 얻을 수 있는 함수((Double) -> Double)를 반환합니다.
     * 그래프 그리기 등에서 반복적으로 호출될 때 유용합니다.
     */
    fun parse(expression: String): (Double) -> Double {
        return try {
            val node = parseToNode(expression)
            val lambda: (Double) -> Double = { x -> node.evaluate(x) }
            lambda
        } catch (e: Exception) {
            { Double.NaN }
        }
    }

    /**
     * 수식 문자열을 계산 트리(ExpressionNode) 구조로 변환합니다.
     * 내부적으로 토큰화 -> 전처리 -> 후위 표기법 변환 -> 트리 생성 과정을 거칩니다.
     */
    fun parseToNode(expression: String): ExpressionNode {
        val tokens = tokenize(expression)
        val processedTokens = insertImplicitMultiplication(tokens)
        val rpn = shuntingYard(processedTokens)
        return buildAST(rpn)
    }

    fun evaluate(node: ExpressionNode): (Double) -> Double {
        return { x -> node.evaluate(x) }
    }



    private fun buildAST(rpnTokens: List<String>): ExpressionNode {
        val stack = mutableListOf<ExpressionNode>()

        for (token in rpnTokens) {
            when {
                isNumber(token) -> stack.add(ExpressionNode.Constant(token.toDouble()))
                token == "x" || token == "e" || token == "pi" -> stack.add(ExpressionNode.Variable(token))
                isFunction(token) -> {
                    val operand = stack.removeAt(stack.lastIndex)
                    val op: (Double) -> Double = when (token) {
                        "sin" -> ::sin
                        "cos" -> ::cos
                        "tan" -> ::tan
                        "log" -> ::log10
                        "ln" -> ::ln
                        "exp" -> ::exp
                        "sqrt" -> ::sqrt
                        "abs" -> ::abs
                        else -> { _ -> 0.0 }
                    }
                    stack.add(ExpressionNode.UnaryOp(operand, op, token))
                }
                isOperator(token) -> {
                    val right = stack.removeAt(stack.lastIndex)
                    val left = stack.removeAt(stack.lastIndex)
                    val op: (Double, Double) -> Double = when (token) {
                        "+" -> Double::plus
                        "-" -> Double::minus
                        "*" -> Double::times
                        "/" -> Double::div
                        "^" -> Double::pow
                        else -> { _, _ -> 0.0 }
                    }
                    stack.add(ExpressionNode.BinaryOp(left, right, op, token))
                }
            }
        }
        return stack.last()
    }

    /**
     * Shunting-yard 알고리즘을 사용하여 중위 표기법(Infix)을 후위 표기법(RPN/Postfix)으로 변환합니다.
     * 예: "3 + 4" -> "3 4 +"
     * 연산자 우선순위를 고려하여 괄호 없이도 올바른 계산 순서를 만듭니다.
     */
    private fun shuntingYard(tokens: List<String>): List<String> {
        val outputQueue = mutableListOf<String>()
        val operatorStack = mutableListOf<String>()

        for (token in tokens) {
            when {
                isNumber(token) || token == "x" || token == "e" || token == "pi" -> outputQueue.add(token)
                isFunction(token) -> operatorStack.add(token)
                token == "(" -> operatorStack.add(token)
                token == ")" -> {
                    while (operatorStack.isNotEmpty() && operatorStack.last() != "(") {
                        outputQueue.add(operatorStack.removeAt(operatorStack.lastIndex))
                    }
                    if (operatorStack.isNotEmpty()) operatorStack.removeAt(operatorStack.lastIndex) // Remove "("
                    if (operatorStack.isNotEmpty() && isFunction(operatorStack.last())) {
                        outputQueue.add(operatorStack.removeAt(operatorStack.lastIndex))
                    }
                }
                isOperator(token) -> {
                    while (operatorStack.isNotEmpty() && hasPrecedence(token, operatorStack.last())) {
                        outputQueue.add(operatorStack.removeAt(operatorStack.lastIndex))
                    }
                    operatorStack.add(token)
                }
            }
        }
        while (operatorStack.isNotEmpty()) {
            outputQueue.add(operatorStack.removeAt(operatorStack.lastIndex))
        }
        return outputQueue
    }

    /**
     * "2x", "x(x+1)", "3sin(x)"와 같이 생략된 곱셈 기호를 명시적으로 추가합니다.
     * 파싱 과정에서 "2x"를 "2 * x"로 변환하여 올바르게 계산되도록 합니다.
     */
    private fun insertImplicitMultiplication(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return tokens
        
        val result = mutableListOf<String>()
        result.add(tokens[0])
        
        for (i in 1 until tokens.size) {
            val prev = tokens[i - 1]
            val curr = tokens[i]
            
            if (shouldInsertMultiply(prev, curr)) {
                result.add("*")
            }
            result.add(curr)
        }
        return result
    }

    /**
     * 연속된 두 토큰(prev, curr) 사이에 곱셈 기호(*)를 삽입해야 하는지 판단합니다.
     * 예: "2" "x" -> true, "x" "(" -> true
     */
    private fun shouldInsertMultiply(prev: String, curr: String): Boolean {
        val isPrevNumber = isNumber(prev) || listOf("x", "e", "pi").contains(prev)
        val isPrevRightParen = prev == ")"
        
        val isCurrNumber = isNumber(curr) || listOf("x", "e", "pi").contains(curr)
        val isCurrFunction = isFunction(curr)
        val isCurrVariable = curr == "x" || curr == "e" || curr == "pi"
        val isCurrLeftParen = curr == "("
        
        if (isPrevNumber) {
            if (isCurrFunction || isCurrLeftParen || isCurrVariable) { 
                return true
            }
            if (!isNumber(prev) && isNumber(curr)) {
                return true
            }
        }
        
        if (isPrevRightParen) {
            if (isCurrNumber || isCurrFunction || isCurrVariable || isCurrLeftParen) {
                return true
            }
        }
        
        return false
    }

    /**
     * 입력된 수식 문자열을 최소 단위인 토큰 리스트로 분리합니다.
     * 공백은 무시하며 숫자, 문자(변수/함수), 괄호/연산자 등을 구분합니다.
     * 예: "2x + 1" -> ["2", "x", "+", "1"]
     */
    private fun tokenize(expression: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expression.length) {
            val char = expression[i]
            when {
                char.isWhitespace() -> i++
                char.isDigit() || char == '.' -> {
                    val sb = StringBuilder()
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) {
                        sb.append(expression[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
                char.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expression.length && expression[i].isLetter()) {
                        sb.append(expression[i])
                        i++
                    }
                    tokens.add(sb.toString())
                }
                else -> {
                    tokens.add(char.toString())
                    i++
                }
            }
        }
        return tokens
    }

    /** 해당 토큰이 숫자인지 확인합니다. */
    private fun isNumber(token: String): Boolean = token.toDoubleOrNull() != null
    /** 해당 토큰이 지원되는 수학 함수인지 확인합니다. */
    private fun isFunction(token: String): Boolean = listOf("sin", "cos", "tan", "log", "ln", "exp", "sqrt", "abs").contains(token)
    /** 해당 토큰이 연산자인지 확인합니다. */
    private fun isOperator(token: String): Boolean = listOf("+", "-", "*", "/", "^").contains(token)

    /**
     * 연산자 우선순위를 비교합니다.
     * op2(스택에 있는 연산자)가 op1(현재 토큰)보다 우선순위가 높거나 같으면 true를 반환합니다.
     * 단, 거듭제곱(^)은 우결합(Right-associative)이므로 예외 처리합니다.
     */
    private fun hasPrecedence(op1: String, op2: String): Boolean {
        if (op2 == "(" || op2 == ")") return false
        if ((op1 == "^") && (op2 == "^")) return false // Right associative
        
        return getPrecedence(op2) >= getPrecedence(op1)
    }

    /**
     * 연산자의 우선순위 숫자를 반환합니다. 클수록 우선순위가 높습니다.
     * 1: +, -
     * 2: *, /
     * 3: ^
     */
    private fun getPrecedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            "^" -> 3
            else -> 0
        }
    }
}
