package com.devhjs.mathgraphstudy.domain.model.math

import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator

/**
 * 수식의 시각적 표현(UI)과 편집을 위한 트리 구조(AST: Abstract Syntax Tree)의 상위 인터페이스입니다.
 *
 * 이 구조는 계산을 위한 `MathParser.ExpressionNode`와 달리, 사용자가 수식을 입력하고
 * 화면에 표시하는 과정(렌더링, 커서 이동, 편집)에 최적화되어 있습니다.
 */
sealed interface VisualMathNode

/** 숫자 노드 (예: "3", "3.14"). 소수점 입력 상태 등을 유지하기 위해 문자열로 저장합니다. */
data class NumberNode(val value: String) : VisualMathNode

/** 변수 노드 (예: "x", "e", "pi"). */
data class VariableNode(val name: String) : VisualMathNode

/** 이항 연산 노드 (예: 3 + x). 왼쪽(left)과 오른쪽(right) 자식 노드를 가집니다. */
data class BinaryOpNode(
    val left: VisualMathNode,
    val op: MathOperator,
    val right: VisualMathNode
) : VisualMathNode

/** 함수 노드 (예: sin(x)). 함수 종류(func)와 인자(arg)를 가집니다. */
data class FunctionNode(
    val func: MathFunction,
    val arg: VisualMathNode
) : VisualMathNode

/** 지수/거듭제곱 노드 (예: x^2). 밑(base)과 지수(exponent)를 가집니다. */
data class PowerNode(
    val base: VisualMathNode,
    val exponent: VisualMathNode
) : VisualMathNode

/** 아직 입력되지 않은 빈 칸 (예: sin(?)). 사용자의 입력을 기다리는 상태를 나타냅니다. */
data object PlaceholderNode : VisualMathNode
