package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.ExpressionError
import com.devhjs.mathgraphstudy.domain.model.FunctionInput
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.toExpressionNode
import com.devhjs.mathgraphstudy.domain.model.math.toVisualNode
import com.devhjs.mathgraphstudy.domain.service.MathParser
import javax.inject.Inject

/**
 * 사용자 입력(직접 입력 수식 또는 템플릿 계수)을 검증하고,
 * 그래프 함수로 저장할 수식 트리를 만들어 반환합니다.
 */
class BuildFunctionNodeUseCase @Inject constructor(
    private val mathParser: MathParser
) {
    operator fun invoke(input: FunctionInput): Result<VisualMathNode, ExpressionError> {
        return when (input) {
            is FunctionInput.Expression -> validate(input.node)
            is FunctionInput.Template -> fromTemplate(input)
        }
    }

    private fun validate(node: VisualMathNode): Result<VisualMathNode, ExpressionError> {
        if (node is PlaceholderNode) return Result.Error(ExpressionError.EMPTY_SLOT)
        return try {
            node.toExpressionNode() // 빈 칸이 남아 있으면 IllegalStateException
            Result.Success(node)
        } catch (e: IllegalStateException) {
            Result.Error(ExpressionError.EMPTY_SLOT)
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
