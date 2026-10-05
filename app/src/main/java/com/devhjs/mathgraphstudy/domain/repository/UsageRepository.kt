package com.devhjs.mathgraphstudy.domain.repository

import com.devhjs.mathgraphstudy.domain.model.UsageStats

interface UsageRepository {
    suspend fun getStats(): UsageStats

    suspend fun saveStats(stats: UsageStats)
}
