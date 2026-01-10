package com.devhjs.mathgraphstudy.domain.model.math

import kotlin.math.E
import kotlin.math.PI

/**
 * 계산(Evaluation)에 최적화된 노드 인터페이스입니다.
 * 시각적 표현을 위한 `VisualMathNode`와 달리, 오직 값 계산에만 집중합니다.
 */
sealed interface ExpressionNode {
    fun evaluate(x: Double): Double

    data class Constant(val value: Double) : ExpressionNode {
        override fun evaluate(x: Double) = value
    }

    data class Variable(val name: String) : ExpressionNode {
        override fun evaluate(x: Double) = if (name == "x") x else if (name == "e") E else if (name == "pi") PI else 0.0
    }

    data class BinaryOp(
        val left: ExpressionNode, 
        val right: ExpressionNode, 
        val op: (Double, Double) -> Double,
        val symbol: String
    ) : ExpressionNode {
        override fun evaluate(x: Double) = op(left.evaluate(x), right.evaluate(x))
    }

    data class UnaryOp(
        val operand: ExpressionNode, 
        val op: (Double) -> Double,
        val symbol: String
    ) : ExpressionNode {
        override fun evaluate(x: Double) = op(operand.evaluate(x))
    }
}
