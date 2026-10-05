package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 저장된 함수 목록을 관찰합니다. */
class ObserveGraphFunctionsUseCase @Inject constructor(
    private val repository: GraphFunctionRepository
) {
    operator fun invoke(): Flow<Result<List<GraphFunction>, DataError>> =
        repository.observeFunctions()
            .map { functions -> Result.Success(functions) as Result<List<GraphFunction>, DataError> }
            .catch { e ->
                e.printStackTrace()
                emit(Result.Error(DataError.Local.UNKNOWN))
            }
}
