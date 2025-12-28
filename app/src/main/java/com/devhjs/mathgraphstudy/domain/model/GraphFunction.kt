package com.devhjs.mathgraphstudy.domain.model

import androidx.compose.ui.graphics.Color

data class GraphFunction(
    val id: String,
    val expression: String,
    val color: Color = Color.Black,
    val isVisible: Boolean = true,
    val calculate: (Double) -> Double = { Double.NaN }
)
