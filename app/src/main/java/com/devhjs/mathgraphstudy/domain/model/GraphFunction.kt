package com.devhjs.mathgraphstudy.domain.model

import com.devhjs.mathgraphstudy.domain.model.math.ExpressionNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.toDisplayString
import com.devhjs.mathgraphstudy.domain.model.math.toExpressionNode

/**
 * 그래프에 그려지는 함수 하나입니다.
 * 수식 트리([node])만 보관하고, 계산용 트리는 처음 계산할 때 한 번 만들어 재사용합니다.
 *
 * @property node 편집과 화면 표시에 쓰는 수식 트리 (빈 칸이 없는 완성된 수식)
 * @property color 그래프 선 색상 (ARGB)
 * @property createdAt 목록 정렬용 생성 시각
 */
data class GraphFunction(
    val id: String,
    val node: VisualMathNode,
    val color: Long,
    val isVisible: Boolean = true,
    val createdAt: Long = 0L
) {
    private val expressionNode: ExpressionNode by lazy { node.toExpressionNode() }

    /** 사람이 읽기 쉬운 수식 문자열 (예: "2x+1") */
    val expression: String get() = node.toDisplayString()

    /** x 에서의 함수값. 정의되지 않으면 NaN 또는 무한대 */
    fun evaluate(x: Double): Double = expressionNode.evaluate(x)
}
