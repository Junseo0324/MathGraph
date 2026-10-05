package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.ParameterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 저장된 매개변수(슬라이더) 목록을 관찰합니다. */
class ObserveParametersUseCase @Inject constructor(
    private val repository: ParameterRepository
) {
    operator fun invoke(): Flow<Result<List<Parameter>, DataError>> =
        repository.observeParameters()
            .map { parameters -> Result.Success(parameters) as Result<List<Parameter>, DataError> }
            .catch { e ->
                e.printStackTrace()
                emit(Result.Error(DataError.Local.UNKNOWN))
            }
}
