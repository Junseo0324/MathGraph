package com.devhjs.mathgraphstudy.domain.model

import androidx.compose.ui.graphics.Color

import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode

data class GraphFunction(
    val id: String,
    val expression: String,
    val visualNode: VisualMathNode? = null,
    val color: Color = Color.Black,
    val isVisible: Boolean = true,
    val calculate: (Double) -> Double = { Double.NaN }
)
