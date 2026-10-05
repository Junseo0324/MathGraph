package com.devhjs.mathgraphstudy.domain.error

/** 사용자가 입력한 수식을 그래프 함수로 만들 수 없는 경우 */
enum class ExpressionError {
    EMPTY_SLOT,
    INVALID_COEFFICIENT,
    INVALID_EXPRESSION;

    fun toMessage(): String = when (this) {
        EMPTY_SLOT -> "비어 있는 칸을 모두 채워주세요."
        INVALID_COEFFICIENT -> "계수는 숫자로 입력해주세요."
        INVALID_EXPRESSION -> "올바르지 않은 수식입니다."
    }
}
