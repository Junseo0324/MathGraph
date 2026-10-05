package com.devhjs.mathgraphstudy.presentation.graph

sealed interface GraphEvent {
    data class ShowError(val message: String) : GraphEvent
    data object ShowInterstitialAd : GraphEvent
    data object RequestReview : GraphEvent // 인앱 리뷰 요청
}
