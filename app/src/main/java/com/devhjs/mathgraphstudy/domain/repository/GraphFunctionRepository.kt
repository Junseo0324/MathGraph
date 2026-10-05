package com.devhjs.mathgraphstudy.domain.repository

import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import kotlinx.coroutines.flow.Flow

interface GraphFunctionRepository {
    /** 저장된 함수 목록 (생성 순). 변경될 때마다 새 목록을 방출합니다. */
    fun observeFunctions(): Flow<List<GraphFunction>>

    /** 같은 id 가 있으면 교체, 없으면 추가합니다. */
    suspend fun upsertFunction(function: GraphFunction)

    suspend fun deleteFunction(id: String)
}
