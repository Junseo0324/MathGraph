package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState
import com.devhjs.mathgraphstudy.presentation.math.MathInputManager
import com.devhjs.mathgraphstudy.presentation.math.MathNodeView

/**
 * 직접 입력 모드에서 작성 중인 수식을 보여주는 입력창입니다.
 * 빈 영역을 누르면 수식 전체로 포커스가 이동합니다.
 */
@Composable
fun AdvancedModeEquationBox(
    state: GraphState = GraphState(),
    onAction: (GraphAction) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(AppColors.Background, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { onAction(GraphAction.OnFocusChange(emptyList())) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("y =", style = AppTextStyles.normalTextRegular, color = AppColors.TextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MathNodeView(
                node = state.mathInput.rootNode,
                currentPath = emptyList(),
                focusPath = state.mathInput.focusPath,
                onFocusRequest = { onAction(GraphAction.OnFocusChange(it)) }
            )
        }
    }
}

/** 키의 역할. 역할마다 색을 달리해 숫자, 연산자, 함수가 한눈에 구분되도록 합니다. */
private enum class KeyType { NUMBER, VARIABLE, OPERATOR, FUNCTION, ACTION }

/** 키패드 버튼 하나. [label]은 화면에 표시할 글자, [input]은 [GraphAction.OnInput]으로 전달할 값입니다. */
private data class KeypadKey(val label: String, val type: KeyType, val input: String = label)

// 맨 윗줄: 함수 키 (다른 줄보다 낮게 표시)
private val functionKeys = listOf("sin", "cos", "tan", "log", "ln").map { KeypadKey(it, KeyType.FUNCTION) } +
    KeypadKey("e", KeyType.VARIABLE)

private val keypadRows = listOf(
    listOf(
        KeypadKey("x", KeyType.VARIABLE),
        KeypadKey("xⁿ", KeyType.OPERATOR, "^"),
        KeypadKey("7", KeyType.NUMBER),
        KeypadKey("8", KeyType.NUMBER),
        KeypadKey("9", KeyType.NUMBER),
        KeypadKey("÷", KeyType.OPERATOR, "/")
    ),
    listOf(
        KeypadKey("( )", KeyType.OPERATOR, MathInputManager.INPUT_PAREN),
        KeypadKey("√", KeyType.FUNCTION),
        KeypadKey("4", KeyType.NUMBER),
        KeypadKey("5", KeyType.NUMBER),
        KeypadKey("6", KeyType.NUMBER),
        KeypadKey("×", KeyType.OPERATOR, "*")
    ),
    listOf(
        KeypadKey("|x|", KeyType.FUNCTION, "abs"),
        KeypadKey("π", KeyType.VARIABLE, "pi"),
        KeypadKey("1", KeyType.NUMBER),
        KeypadKey("2", KeyType.NUMBER),
        KeypadKey("3", KeyType.NUMBER),
        KeypadKey("−", KeyType.OPERATOR, "-")
    ),
    listOf(
        KeypadKey("←", KeyType.ACTION, MathInputManager.INPUT_LEFT),
        KeypadKey("→", KeyType.ACTION, MathInputManager.INPUT_RIGHT),
        KeypadKey("0", KeyType.NUMBER),
        KeypadKey(".", KeyType.NUMBER),
        KeypadKey("⌫", KeyType.ACTION, MathInputManager.INPUT_DELETE),
        KeypadKey("+", KeyType.OPERATOR)
    )
)

/**
 * 수식 입력용 키패드입니다. 스크롤 없이 한 화면에 모든 키가 보이도록 고정 격자로 배치합니다.
 * 맨 윗줄은 함수, 아래 4줄은 변수/숫자/연산자/이동 키입니다.
 */
@Composable
fun AdvancedModeKeypad(
    onAction: (GraphAction) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            functionKeys.forEach { key ->
                KeypadButton(
                    key = key,
                    onClick = { onAction(GraphAction.OnInput(key.input)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
            }
        }
        keypadRows.forEach { keys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keys.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onAction(GraphAction.OnInput(key.input)) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: KeypadKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = when (key.type) {
        KeyType.NUMBER -> AppColors.KeyNumber to AppColors.TextPrimary
        KeyType.VARIABLE -> AppColors.KeyOperator to AppColors.PrimaryGold
        KeyType.OPERATOR -> AppColors.KeyOperator to AppColors.BlueAccent
        KeyType.FUNCTION -> AppColors.KeyFunction to AppColors.TextPrimary
        KeyType.ACTION -> AppColors.KeyFunction to AppColors.TextSecondary
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = key.label,
                style = if (key.type == KeyType.NUMBER) AppTextStyles.largeTextBold else AppTextStyles.normalTextRegular,
                fontStyle = if (key.input == "x") FontStyle.Italic else FontStyle.Normal
            )
        }
    }
}

@Preview
@Composable
private fun AdvancedModeInputPreview() {
    Column(
        modifier = Modifier
            .background(AppColors.Panel)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdvancedModeEquationBox()
        AdvancedModeKeypad()
    }
}
