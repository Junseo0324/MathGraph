package com.devhjs.mathgraphstudy.data.mapper

import com.devhjs.mathgraphstudy.data.datasource.local.ParameterEntity
import com.devhjs.mathgraphstudy.domain.model.Parameter

fun ParameterEntity.toDomain(): Parameter = Parameter(name = name, value = value, min = min, max = max)

fun Parameter.toEntity(): ParameterEntity = ParameterEntity(name = name, value = value, min = min, max = max)
