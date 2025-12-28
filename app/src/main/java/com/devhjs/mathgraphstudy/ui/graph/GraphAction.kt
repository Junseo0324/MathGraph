package com.devhjs.mathgraphstudy.ui.graph

sealed interface GraphAction {
    data class OnExpressionChanged(val expression: String) : GraphAction
    object OnAddFunction : GraphAction
    data class OnRemoveFunction(val id: String) : GraphAction
    data class OnToggleVisibility(val id: String) : GraphAction
    data class OnViewportChange(val scale: Float, val offsetX: Float, val offsetY: Float) : GraphAction
}
