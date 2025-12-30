package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.ui.text.input.TextFieldValue

sealed interface GraphAction {
    data class OnExpressionChanged(val expression: TextFieldValue) : GraphAction
    data class OnInsertSymbol(val symbol: String, val moveCursor: Int) : GraphAction
    object OnToggleMode : GraphAction
    data class OnBeginnerTypeChanged(val type: BeginnerFunctionType) : GraphAction
    data class OnCoefficientChanged(val key: String, val value: String) : GraphAction
    object OnAddFunction : GraphAction
    data class OnRemoveFunction(val id: String) : GraphAction
    data class OnToggleVisibility(val id: String) : GraphAction
    data class OnViewportChange(val scale: Float, val offsetX: Float, val offsetY: Float) : GraphAction
}
