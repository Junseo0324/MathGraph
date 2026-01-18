package com.devhjs.mathgraphstudy.domain.model


import androidx.compose.runtime.Immutable
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode

@Immutable
data class GraphFunction(
    val id: String,
    val expression: String,
    val visualNode: VisualMathNode? = null,
    val color: Long = 0xFF000000,
    val isVisible: Boolean = true,
    val calculate: (Double) -> Double = { Double.NaN }
)
