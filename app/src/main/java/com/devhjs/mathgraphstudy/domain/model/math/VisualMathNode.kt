package com.devhjs.mathgraphstudy.domain.model.math

sealed interface VisualMathNode

data class NumberNode(val value: String) : VisualMathNode
data class VariableNode(val name: String) : VisualMathNode // x, e, pi

data class BinaryOpNode(
    val left: VisualMathNode,
    val op: MathOperator,
    val right: VisualMathNode
) : VisualMathNode

data class FunctionNode(
    val func: MathFunction,
    val arg: VisualMathNode
) : VisualMathNode

data class PowerNode(
    val base: VisualMathNode,
    val exponent: VisualMathNode
) : VisualMathNode

data object PlaceholderNode : VisualMathNode

enum class MathOperator(val symbol: String, val precedence: Int) {
    PLUS("+", 1), MINUS("-", 1), 
    MULTIPLY("*", 2), DIVIDE("/", 2),
    POWER("^", 3)
}

enum class MathFunction(val symbol: String) {
    SQRT("√"), SIN("sin"), COS("cos"), TAN("tan"),
    LOG("log"), LN("ln"), ABS("abs")
}

fun VisualMathNode.toDisplayString(): String {
    return when (this) {
        is NumberNode -> value
        is VariableNode -> name
        is BinaryOpNode -> {
            val isImplicit = op == MathOperator.MULTIPLY && left is NumberNode && right is VariableNode
            if (isImplicit) {
                "${left.toDisplayString()}${right.toDisplayString()}"
            } else {
                "${left.toDisplayString()} ${op.symbol} ${right.toDisplayString()}"
            }
        }
        is FunctionNode -> {
            if (func == MathFunction.SQRT) {
                "${func.symbol}(${arg.toDisplayString()})"
            } else {
                "${func.symbol} ${arg.toDisplayString()}"
            }
        }
        is PowerNode -> {
            "${base.toDisplayString()}^${exponent.toDisplayString()}"
        }
        PlaceholderNode -> "?"
    }
}
