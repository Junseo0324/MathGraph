package com.devhjs.mathgraphstudy.ui.graph

sealed interface GraphEvent {
    data class ShowError(val message: String) : GraphEvent
}
