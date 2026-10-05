package com.devhjs.mathgraphstudy.data.repository

import com.devhjs.mathgraphstudy.data.datasource.local.UsageDataStore
import com.devhjs.mathgraphstudy.domain.model.UsageStats
import com.devhjs.mathgraphstudy.domain.repository.UsageRepository
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dataStore: UsageDataStore
) : UsageRepository {

    override suspend fun getStats(): UsageStats {
        val prefs = dataStore.read()
        return UsageStats(
            functionsAdded = prefs[UsageDataStore.FUNCTIONS_ADDED] ?: 0,
            reviewRequested = prefs[UsageDataStore.REVIEW_REQUESTED] ?: false,
            lastInterstitialAt = prefs[UsageDataStore.LAST_INTERSTITIAL_AT] ?: 0L
        )
    }

    override suspend fun saveStats(stats: UsageStats) =
        dataStore.write(stats.functionsAdded, stats.reviewRequested, stats.lastInterstitialAt)
}
