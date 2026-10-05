package com.devhjs.mathgraphstudy.domain.model.math

import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import com.devhjs.mathgraphstudy.domain.service.MathParser
import kotlin.math.pow

/**
 * UI 표현을 위한 노드 트리(VisualMathNode)를
 * 실제 수학 계산을 위한 도메인 노드 트리(ExpressionNode)로 변환합니다.
 */
fun VisualMathNode.toExpressionNode(): ExpressionNode {
    return when (this) {
        is NumberNode -> ExpressionNode.Constant(this.value.toDoubleOrNull() ?: 0.0)
        is VariableNode -> ExpressionNode.Variable(this.name)
        is BinaryOpNode -> {
            val leftNode = this.left.toExpressionNode()
            val rightNode = this.right.toExpressionNode()
            val (opFunc, symbol) = when (this.op) {
                MathOperator.PLUS -> ({ a: Double, b: Double -> a + b } to "+")
                MathOperator.MINUS -> ({ a: Double, b: Double -> a - b } to "-")
                MathOperator.MULTIPLY -> ({ a: Double, b: Double -> a * b } to "*")
                MathOperator.DIVIDE -> ({ a: Double, b: Double -> a / b } to "/")
                MathOperator.POWER -> ({ a: Double, b: Double -> a.pow(b) } to "^")
            }
            ExpressionNode.BinaryOp(leftNode, rightNode, opFunc, symbol)
        }
        is FunctionNode -> {
            val argNode = this.arg.toExpressionNode()
            val (funcOp, symbol) = when (this.func) {
                MathFunction.SQRT -> ({ x: Double -> kotlin.math.sqrt(x) } to "sqrt")
                MathFunction.SIN -> ({ x: Double -> kotlin.math.sin(x) } to "sin")
                MathFunction.COS -> ({ x: Double -> kotlin.math.cos(x) } to "cos")
                MathFunction.TAN -> ({ x: Double -> kotlin.math.tan(x) } to "tan")
                MathFunction.LOG -> ({ x: Double -> kotlin.math.log10(x) } to "log")
                MathFunction.LN -> ({ x: Double -> kotlin.math.ln(x) } to "ln")
                MathFunction.ABS -> ({ x: Double -> kotlin.math.abs(x) } to "abs")
            }
            ExpressionNode.UnaryOp(argNode, funcOp, symbol)
        }
        is PowerNode -> {
             val baseNode = this.base.toExpressionNode()
             val exponentNode = this.exponent.toExpressionNode()
             ExpressionNode.BinaryOp(baseNode, exponentNode, { a, b -> a.pow(b) }, "^")
        }
        is ParenNode -> this.inner.toExpressionNode()
        is NegateNode -> ExpressionNode.UnaryOp(this.operand.toExpressionNode(), { x: Double -> -x }, MathParser.NEGATE)
        PlaceholderNode -> throw IllegalStateException("Placeholder in expression")
    }
}

/**
 * 계산용 도메인 노드 트리(ExpressionNode)를 UI 표현용 노드 트리(VisualMathNode)로 역변환합니다.
 * 이 과정에서 0 더하기, 1 곱하기 등의 기본적인 식 간소화 로직이 적용됩니다.
 */
fun ExpressionNode.toVisualNode(): VisualMathNode {
    return when (this) {
        is ExpressionNode.Constant -> {
            val v = this.value
            val text = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
            NumberNode(text)
        }
        is ExpressionNode.Variable -> VariableNode(this.name)
        is ExpressionNode.BinaryOp -> {
            val leftViz = this.left.toVisualNode()
            val rightViz = this.right.toVisualNode()

            val isLeftZero = leftViz is NumberNode && (leftViz.value == "0" || leftViz.value == "0.0")
            val isLeftOne = leftViz is NumberNode && (leftViz.value == "1" || leftViz.value == "1.0")
            val isRightZero = rightViz is NumberNode && (rightViz.value == "0" || rightViz.value == "0.0")

            if (this.symbol == "^") {
                // x^1 -> x
                val isRightOne = rightViz is NumberNode && (rightViz.value == "1" || rightViz.value == "1.0")
                if (isRightOne) return leftViz
                PowerNode(base = leftViz, exponent = rightViz)
            } else {
                val op = when (this.symbol) {
                    "+" -> MathOperator.PLUS
                    "-" -> MathOperator.MINUS
                    "*" -> MathOperator.MULTIPLY
                    "/" -> MathOperator.DIVIDE
                    else -> MathOperator.PLUS
                }

                if (op == MathOperator.PLUS && isRightZero) return leftViz
                if (op == MathOperator.PLUS && isLeftZero) return rightViz

                // x + (-3) -> x - 3
                if (op == MathOperator.PLUS) {
                    rightViz.withoutLeadingNegation()?.let {
                        return BinaryOpNode(left = leftViz, op = MathOperator.MINUS, right = it)
                    }
                }

                if (op == MathOperator.MINUS && isRightZero) return leftViz

                if (op == MathOperator.MULTIPLY && isLeftOne) return rightViz
                if (op == MathOperator.MULTIPLY && leftViz is NumberNode && (leftViz.value == "1" || leftViz.value == "1.0")) return rightViz

                if (op == MathOperator.MULTIPLY && isLeftZero) return NumberNode("0")
                if (op == MathOperator.MULTIPLY && isRightZero) return NumberNode("0")

                BinaryOpNode(
                    left = leftViz,
                    op = op,
                    right = rightViz
                )
            }
        }
        is ExpressionNode.UnaryOp -> {
            if (this.symbol == MathParser.NEGATE) {
                return NegateNode(this.operand.toVisualNode())
            }
            val func = when (this.symbol) {
                "sqrt" -> MathFunction.SQRT
                "sin" -> MathFunction.SIN
                "cos" -> MathFunction.COS
                "tan" -> MathFunction.TAN
                "log" -> MathFunction.LOG
                "ln" -> MathFunction.LN
                "abs" -> MathFunction.ABS
                else -> MathFunction.SIN
            }
            FunctionNode(
                func = func,
                arg = this.operand.toVisualNode()
            )
        }
    }
}

/**
 * 음수 부호로 시작하는 노드라면 부호를 뗀 노드를 반환합니다. (예: -3 -> 3, (-3)x -> 3x)
 * 부호로 시작하지 않으면 null을 반환합니다.
 */
private fun VisualMathNode.withoutLeadingNegation(): VisualMathNode? {
    return when {
        this is NegateNode -> operand
        this is BinaryOpNode && (op == MathOperator.MULTIPLY || op == MathOperator.DIVIDE) ->
            left.withoutLeadingNegation()?.let { copy(left = it) }
        else -> null
    }
}
