package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.repository.GraphFunctionRepository
import com.devhjs.mathgraphstudy.domain.repository.ParameterRepository
import javax.inject.Inject

/**
 * 함수를 추가하거나, 같은 id 의 기존 함수를 교체(편집, 표시 여부 변경)합니다.
 * 수식에 처음 쓰인 매개변수가 있으면 기본값(1)으로 함께 만들어 슬라이더가 바로 나타나게 합니다.
 */
class SaveGraphFunctionUseCase @Inject constructor(
    private val repository: GraphFunctionRepository,
    private val parameterRepository: ParameterRepository
) {
    suspend operator fun invoke(function: GraphFunction): Result<Unit, DataError> {
        return try {
            val existing = parameterRepository.getParameters().map { it.name }.toSet()
            (function.parameterNames - existing).forEach { name ->
                parameterRepository.upsertParameter(Parameter(name))
            }
            repository.upsertFunction(function)
            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
