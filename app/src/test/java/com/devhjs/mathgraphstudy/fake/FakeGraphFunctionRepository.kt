package com.devhjs.mathgraphstudy.fake

import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** 메모리에 함수 목록을 보관하는 테스트용 저장소. [shouldFail] 이면 쓰기/읽기에서 예외를 던집니다. */
class FakeGraphFunctionRepository : GraphFunctionRepository {
    private val functions = MutableStateFlow<List<GraphFunction>>(emptyList())
    var shouldFail = false

    override fun observeFunctions(): Flow<List<GraphFunction>> = functions.map {
        if (shouldFail) throw IllegalStateException("observe failed")
        it.sortedBy { function -> function.createdAt }
    }

    override suspend fun upsertFunction(function: GraphFunction) {
        if (shouldFail) throw IllegalStateException("upsert failed")
        functions.update { list -> list.filterNot { it.id == function.id } + function }
    }

    override suspend fun deleteFunction(id: String) {
        if (shouldFail) throw IllegalStateException("delete failed")
        functions.update { list -> list.filterNot { it.id == id } }
    }
}
