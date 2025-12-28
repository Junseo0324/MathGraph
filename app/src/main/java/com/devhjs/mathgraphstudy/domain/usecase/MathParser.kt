package com.devhjs.mathgraphstudy.domain.usecase

import kotlin.math.*

class MathParser {

    fun parse(expression: String): (Double) -> Double {
        return try {
            val tokens = tokenize(expression)
            val processedTokens = insertImplicitMultiplication(tokens)
            val rpn = shuntingYard(processedTokens)
            val ast = buildAST(rpn)
            val lambda: (Double) -> Double = { x -> ast.evaluate(x) }
            lambda
        } catch (e: Exception) {
            { Double.NaN }
        }
    }

    private sealed interface ExpressionNode {
        fun evaluate(x: Double): Double

        data class Constant(val value: Double) : ExpressionNode {
            override fun evaluate(x: Double) = value
        }

        data class Variable(val name: String) : ExpressionNode {
            override fun evaluate(x: Double) = if (name == "x") x else if (name == "e") E else if (name == "pi") PI else 0.0
        }

        data class BinaryOp(val left: ExpressionNode, val right: ExpressionNode, val op: (Double, Double) -> Double) : ExpressionNode {
            override fun evaluate(x: Double) = op(left.evaluate(x), right.evaluate(x))
        }

        data class UnaryOp(val operand: ExpressionNode, val op: (Double) -> Double) : ExpressionNode {
            override fun evaluate(x: Double) = op(operand.evaluate(x))
        }
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
                    stack.add(ExpressionNode.UnaryOp(operand, op))
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
                    stack.add(ExpressionNode.BinaryOp(left, right, op))
                }
            }
        }
        return stack.last()
    }

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

    private fun isNumber(token: String): Boolean = token.toDoubleOrNull() != null
    private fun isFunction(token: String): Boolean = listOf("sin", "cos", "tan", "log", "ln", "exp", "sqrt", "abs").contains(token)
    private fun isOperator(token: String): Boolean = listOf("+", "-", "*", "/", "^").contains(token)

    private fun hasPrecedence(op1: String, op2: String): Boolean {
        if (op2 == "(" || op2 == ")") return false
        if ((op1 == "^") && (op2 == "^")) return false // Right associative
        
        return getPrecedence(op2) >= getPrecedence(op1)
    }

    private fun getPrecedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            "^" -> 3
            else -> 0
        }
    }
}
