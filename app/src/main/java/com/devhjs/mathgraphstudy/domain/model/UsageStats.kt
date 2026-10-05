package com.devhjs.mathgraphstudy.domain.model

/**
 * 광고/리뷰 요청 시점을 정하기 위한 누적 사용 기록입니다.
 *
 * @property functionsAdded 지금까지 새로 추가한 함수 수 (편집은 제외)
 * @property reviewRequested 인앱 리뷰를 요청한 적이 있는지
 * @property lastInterstitialAt 마지막으로 전면 광고를 보여준 시각 (epoch ms, 없으면 0)
 */
data class UsageStats(
    val functionsAdded: Int = 0,
    val reviewRequested: Boolean = false,
    val lastInterstitialAt: Long = 0L
)
