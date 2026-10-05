package com.devhjs.mathgraphstudy.domain.repository

import com.devhjs.mathgraphstudy.domain.model.Parameter
import kotlinx.coroutines.flow.Flow

interface ParameterRepository {
    /** 저장된 매개변수 목록 (이름 순). 변경될 때마다 새 목록을 방출합니다. */
    fun observeParameters(): Flow<List<Parameter>>

    suspend fun getParameters(): List<Parameter>

    /** 같은 이름이 있으면 교체, 없으면 추가합니다. */
    suspend fun upsertParameter(parameter: Parameter)
}
