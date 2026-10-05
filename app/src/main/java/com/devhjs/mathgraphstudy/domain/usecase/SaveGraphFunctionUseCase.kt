package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import javax.inject.Inject

/** 함수를 추가하거나, 같은 id 의 기존 함수를 교체(편집, 표시 여부 변경)합니다. */
class SaveGraphFunctionUseCase @Inject constructor(
    private val repository: GraphFunctionRepository
) {
    suspend operator fun invoke(function: GraphFunction): Result<Unit, DataError> {
        return try {
            repository.upsertFunction(function)
            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
