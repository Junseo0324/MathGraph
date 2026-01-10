package com.devhjs.mathgraphstudy.domain.model.math

import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator

/**
 * VisualMathNode 트리를 사용자가 읽기 쉬운 문자열 형태(예: "2x + 1")로 변환합니다.
 *
 * 주로 디버깅용 로그나, 간단한 텍스트 표시가 필요할 때 사용됩니다.
 * 복잡한 수식 렌더링은 `MathLayout` 컴포저블에서 별도로 처리합니다.
 */
fun VisualMathNode.toDisplayString(): String {
    return when (this) {
        is NumberNode -> value
        is VariableNode -> name
        is BinaryOpNode -> {
            val isImplicit = op == MathOperator.MULTIPLY && left is NumberNode && right is VariableNode
            if (isImplicit) {
                "${left.toDisplayString()}${right.toDisplayString()}"
            } else {
                "${left.toDisplayString()}${op.symbol}${right.toDisplayString()}"
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
