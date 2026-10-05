package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import javax.inject.Inject

class DeleteGraphFunctionUseCase @Inject constructor(
    private val repository: GraphFunctionRepository
) {
    suspend operator fun invoke(id: String): Result<Unit, DataError> {
        return try {
            repository.deleteFunction(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
