package com.devhjs.mathgraphstudy.fake

import com.devhjs.mathgraphstudy.domain.model.UsageStats
import com.devhjs.mathgraphstudy.domain.repository.UsageRepository

/** 메모리에 사용 기록을 보관하는 테스트용 저장소 */
class FakeUsageRepository(var stats: UsageStats = UsageStats()) : UsageRepository {
    var shouldFail = false

    override suspend fun getStats(): UsageStats {
        if (shouldFail) throw IllegalStateException("read failed")
        return stats
    }

    override suspend fun saveStats(stats: UsageStats) {
        if (shouldFail) throw IllegalStateException("write failed")
        this.stats = stats
    }
}
