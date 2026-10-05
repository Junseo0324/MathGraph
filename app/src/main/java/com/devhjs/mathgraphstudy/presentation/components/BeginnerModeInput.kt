package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

@Composable
fun BeginnerModeInput(
    state: GraphState = GraphState(),
    onAction: (GraphAction) -> Unit= {}
) {
    Column {
        Text("함수 타입 선택", style = AppTextStyles.smallTextBold, color = AppColors.TextPrimary)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BeginnerFunctionType.entries.forEach { type ->
                val isSelected = state.beginnerFunctionType == type
                SuggestionChip(
                    onClick = { onAction(GraphAction.OnBeginnerTypeChanged(type)) },
                    label = { 
                        Text(
                            text = type.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isSelected) AppColors.PrimaryGold else Color.Transparent,
                        labelColor = if (isSelected) AppColors.OnPrimary else AppColors.TextPrimary
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) AppColors.PrimaryGold else AppColors.BorderColor
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("계수 입력", style = AppTextStyles.smallTextBold, color = AppColors.TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        CoefficientForm(state, onAction)

        Spacer(modifier = Modifier.height(12.dp))

        CoefficientGuide(state.beginnerFunctionType)

    }
}

@Preview
@Composable
private fun BeginnerModeInputPreview() {
    BeginnerModeInput()
}

/** 계수마다 그래프에서 어떤 역할을 하는지 설명합니다. */
@Composable
private fun CoefficientGuide(type: BeginnerFunctionType) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        coefficientGuides(type).forEach { (label, description) ->
            Row {
                Text(
                    text = label,
                    style = AppTextStyles.smallTextBold,
                    fontStyle = FontStyle.Italic,
                    color = AppColors.PrimaryGold,
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    text = description,
                    style = AppTextStyles.smallTextRegular,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

private fun coefficientGuides(type: BeginnerFunctionType): List<Pair<String, String>> = when (type) {
    BeginnerFunctionType.LINEAR -> listOf(
        "a" to "기울기. 클수록 가파르고, 음수면 오른쪽 아래로 내려가요.",
        "b" to "y절편. 그래프가 y축과 만나는 높이예요."
    )
    BeginnerFunctionType.QUADRATIC -> listOf(
        "a" to "양수면 아래로 볼록, 음수면 위로 볼록. 절댓값이 클수록 폭이 좁아져요.",
        "b" to "축의 위치를 옮겨요. 꼭짓점의 x좌표는 -b/2a 예요.",
        "c" to "y절편. 그래프가 y축과 만나는 높이예요."
    )
    BeginnerFunctionType.CUBIC -> listOf(
        "a" to "양수면 오른쪽 위로, 음수면 오른쪽 아래로 뻗어요.",
        "b" to "b와 c는 극대·극소가 생기는 위치와 모양을 바꿔요.",
        "c" to "x = 0 에서의 기울기예요.",
        "d" to "y절편. 그래프가 y축과 만나는 높이예요."
    )
    BeginnerFunctionType.RATIONAL -> listOf(
        "a" to "점근선에서 멀어지는 정도. 음수면 반대쪽 사분면에 그려져요.",
        "b" to "세로 점근선 x = -b 의 위치를 정해요.",
        "c" to "가로 점근선 y = c 의 위치를 정해요."
    )
    BeginnerFunctionType.EXPONENTIAL -> listOf(
        "a" to "세로 배율. 음수면 x축에 대해 뒤집혀요.",
        "b" to "밑 (b > 0). 1보다 크면 증가, 0과 1 사이면 감소해요.",
        "c" to "가로 점근선 y = c 의 위치를 정해요."
    )
    BeginnerFunctionType.LOGARITHM -> listOf(
        "a" to "세로 배율. 음수면 x축에 대해 뒤집혀요.",
        "b" to "세로 점근선 x = -b 의 위치를 정해요.",
        "c" to "그래프를 위아래로 평행이동해요."
    )
    BeginnerFunctionType.SINE -> listOf(
        "a" to "진폭. 최댓값과 최솟값이 ±|a| 가 돼요.",
        "b" to "주기는 2π/|b|. 클수록 물결이 촘촘해져요.",
        "c" to "그래프를 왼쪽으로 c/b 만큼 이동해요. (위상)",
        "d" to "중심선 y = d. 그래프를 위아래로 이동해요."
    )
}
