package com.devhjs.mathgraphstudy.domain.model.math

import kotlin.math.E
import kotlin.math.PI

/**
 * 계산(Evaluation)에 최적화된 노드 인터페이스입니다.
 * 시각적 표현을 위한 `VisualMathNode`와 달리, 오직 값 계산에만 집중.
 */
sealed interface ExpressionNode {
    /**
     * 주어진 x 값에 대해 수식을 계산합니다.
     *
     * @param x 수식에 대입할 변수 x의 값
     * @return 계산된 결과 값 (Double)
     */
    fun evaluate(x: Double): Double

    /**
     * 상수 값을 나타내는 노드입니다.
     * 예: 5, 3.14 등
     */
    data class Constant(val value: Double) : ExpressionNode {
        override fun evaluate(x: Double) = value
    }

    /**
     * 변수(x) 또는 수학 상수(e, pi)를 나타내는 노드입니다.
     * 'x'일 경우 입력된 x 값을 반환하고, 'e'나 'pi'일 경우 해당 상수 값을 반환합니다.
     */
    data class Variable(val name: String) : ExpressionNode {
        override fun evaluate(x: Double) = if (name == "x") x else if (name == "e") E else if (name == "pi") PI else 0.0
    }

    /**
     * 두 개의 피연산자를 가지는 이항 연산(+, -, *, /, ^ 등) 노드입니다.
     *
     * @property left 연산자의 왼쪽에 위치한 수식 노드 (예: 2 + 3 에서 2)
     * @property right 연산자의 오른쪽에 위치한 수식 노드 (예: 2 + 3 에서 3)
     * @property op 실제 계산을 수행하는 람다 함수 ((Double, Double) -> Double)
     * @property symbol 연산자를 나타내는 문자열 기호 (예: "+", "-", "*") - 디버깅이나 문자열 변환용
     */
    data class BinaryOp(
        val left: ExpressionNode, 
        val right: ExpressionNode, 
        val op: (Double, Double) -> Double,
        val symbol: String
    ) : ExpressionNode {
        override fun evaluate(x: Double) = op(left.evaluate(x), right.evaluate(x))
    }

    /**
     * 하나의 피연산자를 가지는 단항 연산(sin, cos, -부호, log 등) 노드입니다.
     *
     * @property operand 연산의 대상이 되는 수식 노드 (예: sin(x) 에서 x)
     * @property op 실제 계산을 수행하는 람다 함수 ((Double) -> Double)
     * @property symbol 연산자를 나타내는 문자열 기호 (예: "sin", "log", "-")
     */
    data class UnaryOp(
        val operand: ExpressionNode, 
        val op: (Double) -> Double,
        val symbol: String
    ) : ExpressionNode {
        override fun evaluate(x: Double) = op(operand.evaluate(x))
    }
}
