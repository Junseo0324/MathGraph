package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.model.UsageStats
import com.devhjs.mathgraphstudy.fake.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordFunctionAddedUseCaseTest {

    private val repository = FakeUsageRepository()
    private val record = RecordFunctionAddedUseCase(repository)

    private val minute = 60 * 1000L

    private suspend fun outcome(now: Long) =
        (record(now) as Result.Success).data

    @Test
    fun testReviewRequestedOnceAfterThirdFunction() = runTest {
        // When: 함수 1~4개째 추가
        val outcomes = (1..4).map { outcome(now = it * minute) }

        // Then: 3번째에만 리뷰 요청, 이후에는 다시 요청하지 않음
        assertEquals(listOf(false, false, true, false), outcomes.map { it.requestReview })
        assertEquals(4, repository.stats.functionsAdded)
    }

    @Test
    fun testInterstitialEveryFifthFunctionWithMinimumInterval() = runTest {
        // Given: 리뷰는 이미 요청함, 4개 추가된 상태
        repository.stats = UsageStats(functionsAdded = 4, reviewRequested = true)

        // When & Then: 5번째 -> 광고
        assertEquals(true, outcome(now = 10 * minute).showInterstitial)

        // Given: 9개까지 추가 (1분 뒤)
        repeat(4) { outcome(now = 11 * minute) }
        // When & Then: 10번째지만 직전 광고로부터 3분이 안 지나서 광고 없음
        assertEquals(false, outcome(now = 11 * minute).showInterstitial)

        // Given: 14개까지 추가
        repeat(4) { outcome(now = 20 * minute) }
        // When & Then: 15번째, 충분히 지났으므로 광고
        assertEquals(true, outcome(now = 20 * minute).showInterstitial)
    }

    @Test
    fun testNoInterstitialWhenRequestingReview() = runTest {
        // Given: 리뷰 기준을 5개로 착각하지 않도록, 리뷰를 아직 요청하지 않은 4개 상태
        repository.stats = UsageStats(functionsAdded = 4)

        // When: 5번째 (광고 차례이면서 리뷰 요청 조건도 만족)
        val result = outcome(now = 10 * minute)

        // Then: 리뷰만 요청하고 광고는 띄우지 않음
        assertEquals(RecordFunctionAddedUseCase.Outcome(showInterstitial = false, requestReview = true), result)
    }

    @Test
    fun testFailureBecomesLocalError() = runTest {
        repository.shouldFail = true
        assertEquals(Result.Error(DataError.Local.UNKNOWN), record(0L))
    }
}
