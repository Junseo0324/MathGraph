package com.devhjs.mathgraphstudy.fake

import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.repository.ParameterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 메모리에 매개변수를 보관하는 테스트용 저장소. [shouldFail] 이면 예외를 던집니다. */
class FakeParameterRepository : ParameterRepository {
    private val parameters = MutableStateFlow<List<Parameter>>(emptyList())
    var shouldFail = false

    /** 테스트 검증용 현재 저장값 */
    val current: List<Parameter> get() = parameters.value

    override fun observeParameters(): Flow<List<Parameter>> = parameters.map {
        if (shouldFail) throw IllegalStateException("observe failed")
        it.sortedBy { parameter -> parameter.name }
    }

    override suspend fun getParameters(): List<Parameter> {
        if (shouldFail) throw IllegalStateException("get failed")
        return parameters.value
    }

    override suspend fun upsertParameter(parameter: Parameter) {
        if (shouldFail) throw IllegalStateException("upsert failed")
        parameters.update { list -> list.filterNot { it.name == parameter.name } + parameter }
    }
}
