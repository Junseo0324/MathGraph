package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.runtime.Immutable
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.presentation.math.MathInputState


@Immutable
data class GraphState(
    val functions: List<GraphFunction> = emptyList(),
    val mathInput: MathInputState = MathInputState(),
    val isBeginnerMode: Boolean = false,
    val beginnerFunctionType: BeginnerFunctionType = BeginnerFunctionType.LINEAR,
    val beginnerCoefficients: Map<String, String> = emptyMap(),
    val viewportScale: Float = 40f, // Pixels per unit
    val viewportOffsetX: Float = 0f,
    val viewportOffsetY: Float = 0f,
    val intersections: List<androidx.compose.ui.geometry.Offset> = emptyList()
)
