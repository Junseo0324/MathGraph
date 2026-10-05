package com.devhjs.mathgraphstudy.presentation.math

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.devhjs.mathgraphstudy.domain.model.math.startsWithDigit
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles

/**
 * [VisualMathNode] 트리를 재귀적으로 순회하며 화면에 그리는 Jetpack Compose 컴포넌트입니다.
 *
 * 주요 기능:
 * 1. 노드 타입(숫자, 변수, 이항 연산, 함수, 거듭제곱)에 따라 적절한 UI 레이아웃을 구성합니다.
 * 2. 분수(나눗셈)는 세로로 배치하고, 거듭제곱은 위첨자로 배치합니다.
 * 3. 루트(sqrt) 기호는 [drawBehind]를 사용하여 직접 캔버스에 그립니다.
 * 4. 현재 포커스 된 노드([focusPath])에 테두리를 그려 커서 위치를 표시합니다.
 * 5. [PlaceholderNode]는 빈 박스로 표시하여 사용자가 입력을 유도합니다.
 */

@Composable
fun MathNodeView(
    node: VisualMathNode,
    currentPath: List<Int>,
    focusPath: List<Int>,
    onFocusRequest: (List<Int>) -> Unit,
    editable: Boolean = true
) {
    val isFocused = editable && currentPath == focusPath
    // 편집 중일 때만 각 노드를 터치해 포커스를 옮길 수 있음 (목록 표시용은 터치/여백 없음)
    val modifier = when {
        !editable -> Modifier
        isFocused -> Modifier
            .clickable { onFocusRequest(currentPath) }
            .border(2.dp, AppColors.PrimaryGold, RoundedCornerShape(4.dp))
            .padding(2.dp)
        else -> Modifier
            .clickable { onFocusRequest(currentPath) }
            .padding(2.dp)
    }

    // 자식 노드에도 같은 편집 여부를 전달
    @Composable
    fun Child(child: VisualMathNode, index: Int) =
        MathNodeView(child, currentPath + index, focusPath, onFocusRequest, editable)

    Box(modifier = modifier) {
        when (node) {
            is NumberNode -> Text(text = node.value)
            is VariableNode -> Text(
                // 수학 표기 관례: 문자는 기울임, 원주율은 π
                text = if (node.name == "pi") "π" else node.name,
                fontStyle = if (node.name == "pi") FontStyle.Normal else FontStyle.Italic
            )
            is BinaryOpNode -> {
                if (node.op == MathOperator.DIVIDE) {
                    // Vertical Fraction Layout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(IntrinsicSize.Max)
                    ) {
                        Child(node.left, 0)
                        // Fraction Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppColors.TextPrimary)
                        )
                        Child(node.right, 1)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Child(node.left, 0)
                        if (node.op == MathOperator.MULTIPLY && !node.right.startsWithDigit()) {
                            // 암시적 곱셈 (예: 2x, 3sin x) 은 기호 생략. 함수 이름 앞은 조금 더 띄움 (a sin x)
                            Spacer(modifier = Modifier.width(if (node.right is FunctionNode) 5.dp else 2.dp))
                        } else {
                            val symbol = when (node.op) {
                                MathOperator.MULTIPLY -> "×"
                                MathOperator.MINUS -> "−"
                                else -> node.op.symbol
                            }
                            Text(
                                text = symbol,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        Child(node.right, 1)
                    }
                }
            }

            is FunctionNode -> {
                if (node.func == MathFunction.SQRT) {
                    val color = AppColors.TextPrimary
                    Row(
                        modifier = Modifier.drawBehind {
                            val strokeWidth = 1.5.dp.toPx()
                            val path = Path().apply {
                                // Coordinates for the root symbol
                                // 1. Start (small tick left) - approx (2dp, 65% height)
                                moveTo(2.dp.toPx(), size.height * 0.65f)
                                // 2. Valley (bottom point) - approx (6dp, height - 2dp)
                                lineTo(6.dp.toPx(), size.height - 2.dp.toPx())
                                // 3. Beak (top point near text start) - (12dp, line_top)
                                // The horizontal line is drawn at y = strokeWidth/2 derived from top padding
                                val lineY = 4.dp.toPx() / 2 // Centered in the 4dp top spacing
                                lineTo(12.dp.toPx(), lineY)
                                // 4. Horizontal Line (Vinculum)
                                lineTo(size.width, lineY)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    ) {
                        // Reserve space for the root symbol on the left
                        Spacer(modifier = Modifier.padding(start = 14.dp))

                        // Content with top padding to make room for the horizontal line
                        Box(
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            ProvideTextStyle(
                                value = AppTextStyles.smallTextRegular
                            ) {
                                Child(node.arg, 0)
                            }
                        }
                    }
                } else if (node.func == MathFunction.ABS) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "|")
                        Child(node.arg, 0)
                        Text(text = "|")
                    }
                } else {
                    // 인자가 한 글자(숫자/변수/빈 칸)가 아니면 sin(bx) 처럼 괄호로 감싸 범위를 분명히 함
                    val needsParens = node.arg !is NumberNode && node.arg !is VariableNode &&
                        node.arg !is PlaceholderNode && node.arg !is ParenNode
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = node.func.symbol)
                        if (needsParens) Text(text = "(") else Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Child(node.arg, 0)
                        if (needsParens) Text(text = ")")
                    }
                }
            }

            is PowerNode -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Child(node.base, 0)
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .offset(y = (-8).dp)
                    ) {
                        ProvideTextStyle(
                            value = AppTextStyles.smallTextRegular.copy(fontSize = 12.sp)
                        ) {
                            Child(node.exponent, 1)
                        }
                    }
                }
            }

            is ParenNode -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "(")
                    Child(node.inner, 0)
                    Text(text = ")")
                }
            }

            is NegateNode -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "−")
                    Child(node.operand, 0)
                }
            }

            PlaceholderNode -> {
                Box(
                    modifier = Modifier
                        .border(1.dp, AppColors.TextSecondary, shape = RoundedCornerShape(4.dp))
                        // PlaceholderNode is already wrapped by the baseModifier in the outer Box,
                        // but we keep its padding. No need for an additional clickable here.
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(" ") // Empty space to give size
                }
            }
        }
    }
}
