package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
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
                        containerColor = if (isSelected) AppColors.PrimaryGoldVariant else Color.Transparent,
                        labelColor = if (isSelected) AppColors.BlackCharcoal else AppColors.TextPrimary
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

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { onAction(GraphAction.OnAddFunction) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("함수 추가")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }
}

@Preview
@Composable
private fun BeginnerModeInputPreview() {
    BeginnerModeInput()
}