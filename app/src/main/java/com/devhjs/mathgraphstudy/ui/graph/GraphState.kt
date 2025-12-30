package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.ui.text.input.TextFieldValue
import com.devhjs.mathgraphstudy.domain.model.GraphFunction

enum class BeginnerFunctionType(val displayName: String, val inputLabels: List<String>) {
    LINEAR("일차", listOf("a", "b")),       // y = ax + b
    QUADRATIC("이차", listOf("a", "b", "c")), // y = ax^2 + bx + c
    IRRATIONAL("무리", listOf("a", "b", "c")), // y = a*sqrt(x+b) + c
    RATIONAL("유리", listOf("a", "b", "c"))    // y = a/(x+b) + c
}

data class GraphState(
    val functions: List<GraphFunction> = emptyList(),
    val inputExpression: TextFieldValue = TextFieldValue(""),
    val isBeginnerMode: Boolean = false,
    val beginnerFunctionType: BeginnerFunctionType = BeginnerFunctionType.LINEAR,
    val beginnerCoefficients: Map<String, String> = emptyMap(),
    val viewportScale: Float = 40f, // Pixels per unit
    val viewportOffsetX: Float = 0f,
    val viewportOffsetY: Float = 0f,
    val intersections: List<androidx.compose.ui.geometry.Offset> = emptyList()
)
