package com.devhjs.mathgraphstudy.presentation.graph

sealed interface GraphEvent {
    data class ShowError(val message: String) : GraphEvent
    data object ShowInterstitialAd : GraphEvent
}
