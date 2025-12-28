package com.devhjs.mathgraphstudy.ui.graph

import com.devhjs.mathgraphstudy.domain.model.GraphFunction

data class GraphState(
    val functions: List<GraphFunction> = emptyList(),
    val inputExpression: String = "",
    val viewportScale: Float = 40f, // Pixels per unit
    val viewportOffsetX: Float = 0f,
    val viewportOffsetY: Float = 0f
)
