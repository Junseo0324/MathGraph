package com.devhjs.mathgraphstudy.domain.model

/** 함수 그래프의 특징점 (근, y절편, 극대, 극소) */
data class KeyPoint(
    val x: Double,
    val y: Double,
    val type: Type
) {
    enum class Type { ROOT, Y_INTERCEPT, MAXIMUM, MINIMUM }
}
