package com.devhjs.mathgraphstudy.data.repository

import com.devhjs.mathgraphstudy.data.datasource.local.GraphFunctionDao
import com.devhjs.mathgraphstudy.data.mapper.toDomainOrNull
import com.devhjs.mathgraphstudy.data.mapper.toEntity
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GraphFunctionRepositoryImpl @Inject constructor(
    private val dao: GraphFunctionDao
) : GraphFunctionRepository {

    override fun observeFunctions(): Flow<List<GraphFunction>> =
        dao.observeAll().map { entities -> entities.mapNotNull { it.toDomainOrNull() } }

    override suspend fun upsertFunction(function: GraphFunction) =
        dao.upsert(function.toEntity())

    override suspend fun deleteFunction(id: String) =
        dao.deleteById(id)
}
