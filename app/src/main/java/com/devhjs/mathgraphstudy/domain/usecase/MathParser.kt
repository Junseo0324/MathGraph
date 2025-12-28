package com.devhjs.mathgraphstudy.domain.usecase

import kotlin.math.*

class MathParser {

    fun parse(expression: String): (Double) -> Double {
        val tokens = tokenize(expression)
        val processedTokens = insertImplicitMultiplication(tokens)
        return { x ->
            try {
                evaluate(processedTokens, x)
            } catch (e: Exception) {
                Double.NaN
            }
        }
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
        // Cases to insert *:
        // 1. Number (or x/e/pi) followed by Variable (x) or Function or '('
        // 2. ')' followed by Number or Variable or Function or '('
        
        val isPrevNumber = isNumber(prev) || listOf("x", "e", "pi").contains(prev)
        val isPrevRightParen = prev == ")"
        
        val isCurrNumber = isNumber(curr) || listOf("x", "e", "pi").contains(curr) // usually don't have )2 but supported.
        val isCurrFunction = isFunction(curr)
        val isCurrVariable = curr == "x" || curr == "e" || curr == "pi"
        val isCurrLeftParen = curr == "("
        
        if (isPrevNumber) {
            if (isCurrFunction || isCurrLeftParen || (isCurrVariable && !isNumber(prev))) { 
                // 2sin, 2(x), 2x. 
                // But wait, if prev is "sin", it's a function, not number. 
                // isNumber check handles digits. 
                // But what about "xsin"? x is variable.
                // My logic: isPrevNumber includes x/e/pi.
                // If prev is "2", curr is "x" -> 2*x.
                // If prev is "x", curr is "sin" -> x*sin.
                // If prev is "2", curr is "(" -> 2*(.
                return true
            }
            // Special case: Number followed by Number is handled by tokenizer merging them? 
            // Yes tokenizer merges digits.
            // But what about "x" followed by "2"? x2 -> x*2.
            if (!isNumber(prev) && isNumber(curr)) {
                return true
            }
        }
        
        if (isPrevRightParen) {
            // )2, )x, )sin, )(
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

    private fun evaluate(tokens: List<String>, x: Double): Double {
        val values = mutableListOf<Double>()
        val ops = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            when {
                token == "(" -> ops.add(token)
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.last() != "(") {
                        applyOp(ops.removeAt(ops.lastIndex), values)
                    }
                    if (ops.isNotEmpty()) ops.removeAt(ops.lastIndex) // Remove "("
                    
                    // Handle function calls like sin(...)
                    if (ops.isNotEmpty() && isFunction(ops.last())) {
                        applyOp(ops.removeAt(ops.lastIndex), values)
                    }
                }
                isNumber(token) -> values.add(token.toDouble())
                token == "x" -> values.add(x)
                token == "e" -> values.add(E)
                token == "pi" -> values.add(PI)
                isOperator(token) -> {
                    while (ops.isNotEmpty() && hasPrecedence(token, ops.last())) {
                        applyOp(ops.removeAt(ops.lastIndex), values)
                    }
                    ops.add(token)
                }
                isFunction(token) -> ops.add(token)
                 // Implicit multiplication handling (e.g. 2x, xsin)
                 // This logic is tricky in this simple loop, simplified for now:
                 // basic implementation assumes explicit operators or handles specific cases if needed.
                 // For "2x", tokenizer gives "2", "x".
                 // We need to insert "*" if we have Number followed by Variable/Function/LeftParen.
                 // But for now, let's Stick to standard Shunting-yard or similar logic.
                 // To support implicit multiplication properly, we should pre-process tokens.
            }
            i++
        }

        while (ops.isNotEmpty()) {
            applyOp(ops.removeAt(ops.lastIndex), values)
        }

        return values.lastOrNull() ?: 0.0
    }
    
    // Improved evaluate with Shunting Yard Algorithm logic simplified for recursion or stacks
    // Re-writing evaluate to be more robust for standard precedence

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

    private fun applyOp(op: String, values: MutableList<Double>) {
        if (isFunction(op)) {
            val a = values.removeAt(values.lastIndex)
            val res = when (op) {
                "sin" -> sin(a)
                "cos" -> cos(a)
                "tan" -> tan(a)
                "log" -> log10(a)
                "ln" -> ln(a)
                "exp" -> exp(a)
                "sqrt" -> sqrt(a)
                "abs" -> abs(a)
                else -> 0.0
            }
            values.add(res)
            return
        }

        val b = values.removeAt(values.lastIndex)
        val a = if (values.isNotEmpty()) values.removeAt(values.lastIndex) else 0.0 // Unary handling might need check

        val res = when (op) {
            "+" -> a + b
            "-" -> a - b // This handles binary minus, for unary minus we need cleaner tokenization
            "*" -> a * b
            "/" -> a / b
            "^" -> a.pow(b)
            else -> 0.0
        }
        values.add(res)
    }
}
