package com.devhjs.mathgraphstudy.data.repository

import com.devhjs.mathgraphstudy.data.datasource.local.ParameterDao
import com.devhjs.mathgraphstudy.data.mapper.toDomain
import com.devhjs.mathgraphstudy.data.mapper.toEntity
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.repository.ParameterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ParameterRepositoryImpl @Inject constructor(
    private val dao: ParameterDao
) : ParameterRepository {

    override fun observeParameters(): Flow<List<Parameter>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getParameters(): List<Parameter> =
        dao.getAll().map { it.toDomain() }

    override suspend fun upsertParameter(parameter: Parameter) =
        dao.upsert(parameter.toEntity())
}
