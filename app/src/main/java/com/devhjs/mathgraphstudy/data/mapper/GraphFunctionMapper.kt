package com.devhjs.mathgraphstudy.data.mapper

import com.devhjs.mathgraphstudy.data.datasource.local.GraphFunctionEntity
import com.devhjs.mathgraphstudy.data.datasource.local.MathNodeDto
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NegateNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.ParenNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

// Domain Model -> Room Entity
fun GraphFunction.toEntity(): GraphFunctionEntity = GraphFunctionEntity(
    id = id,
    nodeJson = json.encodeToString(MathNodeDto.serializer(), node.toDto()),
    color = color,
    isVisible = isVisible,
    createdAt = createdAt
)

/**
 * Room Entity -> Domain Model.
 * 저장된 수식을 읽을 수 없으면(포맷 손상 등) null 을 반환해 목록에서 제외합니다.
 */
fun GraphFunctionEntity.toDomainOrNull(): GraphFunction? {
    val node = runCatching { json.decodeFromString(MathNodeDto.serializer(), nodeJson).toDomain() }
        .getOrNull() ?: return null
    return GraphFunction(
        id = id,
        node = node,
        color = color,
        isVisible = isVisible,
        createdAt = createdAt
    )
}

fun VisualMathNode.toDto(): MathNodeDto = when (this) {
    is NumberNode -> MathNodeDto.Number(value)
    is VariableNode -> MathNodeDto.Variable(name)
    is BinaryOpNode -> MathNodeDto.Binary(left.toDto(), op.name, right.toDto())
    is FunctionNode -> MathNodeDto.Function(func.name, arg.toDto())
    is PowerNode -> MathNodeDto.Power(base.toDto(), exponent.toDto())
    is ParenNode -> MathNodeDto.Paren(inner.toDto())
    is NegateNode -> MathNodeDto.Negate(operand.toDto())
    PlaceholderNode -> MathNodeDto.Placeholder
}

fun MathNodeDto.toDomain(): VisualMathNode = when (this) {
    is MathNodeDto.Number -> NumberNode(value)
    is MathNodeDto.Variable -> VariableNode(name)
    is MathNodeDto.Binary -> BinaryOpNode(left.toDomain(), MathOperator.valueOf(op), right.toDomain())
    is MathNodeDto.Function -> FunctionNode(MathFunction.valueOf(func), arg.toDomain())
    is MathNodeDto.Power -> PowerNode(base.toDomain(), exponent.toDomain())
    is MathNodeDto.Paren -> ParenNode(inner.toDomain())
    is MathNodeDto.Negate -> NegateNode(operand.toDomain())
    MathNodeDto.Placeholder -> PlaceholderNode
}
