package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GraphControls(
    modifier: Modifier = Modifier,
    state: GraphState = GraphState(),
    onAction: (GraphAction) -> Unit= {},
) {
    LazyColumn(
        modifier = modifier
            .background(AppColors.DarkSurface),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                GraphModeToggle(
                    isBeginnerMode = state.isBeginnerMode,
                    onModeChange = { onAction(GraphAction.OnToggleMode) },
                    modifier = Modifier.fillMaxWidth(0.9f) // 전체 너비의 90% 정도 차지하게
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.isBeginnerMode) {
            item {
                BeginnerModeInput(state, onAction)
            }
        } else {
            stickyHeader {
                AdvancedModeEquationBox(state, onAction)
            }
            item {
                AdvancedModeKeypad(state, onAction)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "함수 목록",
                style = AppTextStyles.normalTextBold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(state.functions) { function ->
            FunctionItem(
                function = function,
                onToggleVisibility = { onAction(GraphAction.OnToggleVisibility(function.id)) },
                onDelete = { onAction(GraphAction.OnRemoveFunction(function.id)) }
            )
        }


        item {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { onAction(GraphAction.OnOpenLicenses) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "오픈소스 라이선스",
                    color = AppColors.TextSecondary,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
private fun GraphControlsPreview() {
    GraphControls()
}