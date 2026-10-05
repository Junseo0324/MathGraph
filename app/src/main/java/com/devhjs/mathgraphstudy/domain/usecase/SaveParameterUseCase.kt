package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.ParameterRepository
import javax.inject.Inject

/** 매개변수 값(슬라이더 위치)을 저장합니다. 값은 [Parameter.min] ~ [Parameter.max] 로 제한합니다. */
class SaveParameterUseCase @Inject constructor(
    private val repository: ParameterRepository
) {
    suspend operator fun invoke(parameter: Parameter): Result<Unit, DataError> {
        return try {
            repository.upsertParameter(parameter.copy(value = parameter.value.coerceIn(parameter.min, parameter.max)))
            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
