package com.devhjs.mathgraphstudy.domain.error

/** 사용자가 입력한 수식을 그래프 함수로 만들 수 없는 경우 */
enum class ExpressionError {
    EMPTY,
    INVALID_COEFFICIENT,
    INVALID_EXPRESSION;

    fun toMessage(): String = when (this) {
        EMPTY -> "수식을 입력해주세요."
        INVALID_COEFFICIENT -> "계수는 숫자로 입력해주세요."
        INVALID_EXPRESSION -> "수식을 완성해주세요. (연산자 뒤나 괄호 안이 비어 있지 않은지 확인)"
    }
}
