package com.devhjs.mathgraphstudy.domain.model

/**
 * 수식에 쓰이는 매개변수(예: y = a·sin(bx) 의 a, b) 하나입니다.
 * 슬라이더로 [min] ~ [max] 사이에서 값을 바꿀 수 있습니다.
 */
data class Parameter(
    val name: String,
    val value: Double = DEFAULT_VALUE,
    val min: Double = DEFAULT_MIN,
    val max: Double = DEFAULT_MAX
) {
    companion object {
        /** 매개변수로 쓸 수 있는 이름. (x 는 변수, e 는 상수라 제외) */
        val NAMES = listOf("a", "b", "c", "d", "k", "m")

        const val DEFAULT_VALUE = 1.0
        const val DEFAULT_MIN = -10.0
        const val DEFAULT_MAX = 10.0
    }
}
