package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.ExpressionError
import com.devhjs.mathgraphstudy.domain.model.FunctionInput
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.toVisualNode
import com.devhjs.mathgraphstudy.domain.service.MathParser
import javax.inject.Inject

/**
 * 사용자 입력(직접 입력한 수식 문자열 또는 템플릿 계수)을 해석하고,
 * 그래프 함수로 저장할 수식 트리를 만들어 반환합니다.
 */
class BuildFunctionNodeUseCase @Inject constructor(
    private val mathParser: MathParser
) {
    operator fun invoke(input: FunctionInput): Result<VisualMathNode, ExpressionError> {
        return when (input) {
            is FunctionInput.Expression -> fromText(input.text)
            is FunctionInput.Template -> fromTemplate(input)
        }
    }

    private fun fromText(text: String): Result<VisualMathNode, ExpressionError> {
        if (text.isBlank()) return Result.Error(ExpressionError.EMPTY)
        return try {
            Result.Success(mathParser.parseToNode(text).toVisualNode())
        } catch (e: Exception) {
            Result.Error(ExpressionError.INVALID_EXPRESSION)
        }
    }

    private fun fromTemplate(input: FunctionInput.Template): Result<VisualMathNode, ExpressionError> {
        val expression = input.type.buildExpression(input.coefficients)
            ?: return Result.Error(ExpressionError.INVALID_COEFFICIENT)
        return try {
            Result.Success(mathParser.parseToNode(expression).toVisualNode())
        } catch (e: Exception) {
            Result.Error(ExpressionError.INVALID_COEFFICIENT)
        }
    }
}
