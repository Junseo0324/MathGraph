package com.devhjs.mathgraphstudy.domain.model.math

import com.devhjs.mathgraphstudy.domain.model.Parameter
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
            val isImplicit = op == MathOperator.MULTIPLY && !right.startsWithDigit()
            if (isImplicit) {
                "${left.toDisplayString()}${right.toDisplayString()}"
            } else {
                "${left.toDisplayString()}${op.symbol}${right.toDisplayString()}"
            }
        }
        is FunctionNode -> {
            when (func) {
                MathFunction.SQRT -> "${func.symbol}(${arg.toDisplayString()})"
                MathFunction.ABS -> "|${arg.toDisplayString()}|"
                else -> if (arg is NumberNode || arg is VariableNode || arg is ParenNode) {
                    "${func.symbol} ${arg.toDisplayString()}"
                } else {
                    "${func.symbol}(${arg.toDisplayString()})"
                }
            }
        }
        is PowerNode -> {
            "${base.toDisplayString()}^${exponent.toDisplayString()}"
        }
        is ParenNode -> "(${inner.toDisplayString()})"
        is NegateNode -> "-${operand.toDisplayString()}"
        PlaceholderNode -> "?"
    }
}

/** 수식 트리에 쓰인 매개변수 이름들을 모읍니다. */
fun VisualMathNode.parameterNames(): Set<String> = when (this) {
    is VariableNode -> if (name in Parameter.NAMES) setOf(name) else emptySet()
    is BinaryOpNode -> left.parameterNames() + right.parameterNames()
    is FunctionNode -> arg.parameterNames()
    is PowerNode -> base.parameterNames() + exponent.parameterNames()
    is ParenNode -> inner.parameterNames()
    is NegateNode -> operand.parameterNames()
    is NumberNode, PlaceholderNode -> emptySet()
}

/**
 * 화면에 표시했을 때 숫자(또는 음수 부호)로 시작하는 노드인지 확인합니다.
 * 곱셈 기호를 생략하면 "2 3"처럼 숫자가 붙어 보이는 경우를 판단하는 데 사용합니다.
 */
fun VisualMathNode.startsWithDigit(): Boolean {
    return when (this) {
        is NumberNode, is NegateNode -> true
        is PowerNode -> base.startsWithDigit()
        is BinaryOpNode -> left.startsWithDigit()
        else -> false
    }
}
