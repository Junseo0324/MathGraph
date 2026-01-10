package com.devhjs.mathgraphstudy.domain.model.math.enums

/**
 * 수식에서 사용되는 이항 연산자(+, -, *, /, ^)를 정의합니다.
 *
 * @property symbol 연산자의 기호 (예: "+")
 * @property precedence 연산 우선순위 (높을수록 먼저 계산됨)
 */
enum class MathOperator(val symbol: String, val precedence: Int) {
    PLUS("+", 1), MINUS("-", 1),
    MULTIPLY("*", 2), DIVIDE("/", 2),
    POWER("^", 3)
}
