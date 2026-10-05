package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * 새 함수를 추가했을 때 사용 기록을 갱신하고, 지금 광고나 리뷰 요청을 보여줄지 결정합니다.
 *
 * - 인앱 리뷰: 함수를 [REVIEW_AFTER_FUNCTIONS]개 이상 추가했고 아직 요청한 적이 없으면 한 번만
 * - 전면 광고: [INTERSTITIAL_EVERY_FUNCTIONS]개마다, 단 직전 광고로부터 [INTERSTITIAL_MIN_INTERVAL_MS] 이상 지났을 때만
 * - 리뷰를 요청하는 순간에는 광고를 띄우지 않음
 */
class RecordFunctionAddedUseCase @Inject constructor(
    private val repository: UsageRepository
) {
    data class Outcome(val showInterstitial: Boolean, val requestReview: Boolean)

    /** @param now 현재 시각 (epoch ms) */
    suspend operator fun invoke(now: Long): Result<Outcome, DataError> {
        return try {
            val stats = repository.getStats()
            val added = stats.functionsAdded + 1

            val requestReview = !stats.reviewRequested && added >= REVIEW_AFTER_FUNCTIONS
            val showInterstitial = !requestReview &&
                added % INTERSTITIAL_EVERY_FUNCTIONS == 0 &&
                now - stats.lastInterstitialAt >= INTERSTITIAL_MIN_INTERVAL_MS

            repository.saveStats(
                stats.copy(
                    functionsAdded = added,
                    reviewRequested = stats.reviewRequested || requestReview,
                    lastInterstitialAt = if (showInterstitial) now else stats.lastInterstitialAt
                )
            )
            Result.Success(Outcome(showInterstitial, requestReview))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    companion object {
        const val REVIEW_AFTER_FUNCTIONS = 3
        const val INTERSTITIAL_EVERY_FUNCTIONS = 5
        const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L
    }
}
