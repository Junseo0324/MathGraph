package com.devhjs.mathgraphstudy.data.datasource.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 수식 트리(VisualMathNode)를 DB 에 저장하기 위한 직렬화 모델입니다.
 * 연산자/함수는 enum 이름으로 저장합니다. ([SerialName] 은 저장 포맷이므로 바꾸지 않습니다)
 */
@Serializable
sealed interface MathNodeDto {
    @Serializable
    @SerialName("num")
    data class Number(val value: String) : MathNodeDto

    @Serializable
    @SerialName("var")
    data class Variable(val name: String) : MathNodeDto

    @Serializable
    @SerialName("bin")
    data class Binary(val left: MathNodeDto, val op: String, val right: MathNodeDto) : MathNodeDto

    @Serializable
    @SerialName("fn")
    data class Function(val func: String, val arg: MathNodeDto) : MathNodeDto

    @Serializable
    @SerialName("pow")
    data class Power(val base: MathNodeDto, val exponent: MathNodeDto) : MathNodeDto

    @Serializable
    @SerialName("paren")
    data class Paren(val inner: MathNodeDto) : MathNodeDto

    @Serializable
    @SerialName("neg")
    data class Negate(val operand: MathNodeDto) : MathNodeDto

    @Serializable
    @SerialName("empty")
    data object Placeholder : MathNodeDto
}
