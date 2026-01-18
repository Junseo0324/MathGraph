package com.devhjs.mathgraphstudy.domain.model.math.enums

enum class BeginnerFunctionType(val displayName: String, val inputLabels: List<String>) {
    LINEAR("일차", listOf("a", "b")),       // y = ax + b
    QUADRATIC("이차", listOf("a", "b", "c")), // y = ax^2 + bx + c
    CUBIC("삼차", listOf("a", "b", "c", "d")), // y = ax^3 + bx^2 + cx + d
    RATIONAL("유리", listOf("a", "b", "c"))    // y = a/(x+b) + c
}