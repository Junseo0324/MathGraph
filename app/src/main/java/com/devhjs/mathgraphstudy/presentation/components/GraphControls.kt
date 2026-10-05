package com.devhjs.mathgraphstudy.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

/**
 * 그래프 아래(태블릿은 옆)에 표시되는 조작 패널입니다.
 * 평소에는 함수 목록을, 함수를 추가하거나 편집할 때는 입력 패널을 보여줍니다.
 */
@Composable
fun GraphControls(
    modifier: Modifier = Modifier,
    state: GraphState = GraphState(),
    onAction: (GraphAction) -> Unit = {},
) {
    if (state.isEditorOpen) {
        FunctionEditorPanel(state, onAction, modifier)
    } else {
        FunctionListPanel(state, onAction, modifier)
    }
}

/**
 * 추가한 함수 목록입니다. 항목을 누르면 해당 함수를 편집합니다.
 */
@Composable
private fun FunctionListPanel(
    state: GraphState,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.background(AppColors.Panel),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.functions.isEmpty()) {
            item {
                Text(
                    text = "함수를 추가해 그래프를 그려보세요",
                    style = AppTextStyles.smallTextRegular,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }

        if (state.functions.isNotEmpty()) {
            item { SectionTitle("함수") }
        }

        items(state.functions, key = { it.id }) { function ->
            FunctionItem(
                function = function,
                isSelected = function.id == state.selectedFunctionId,
                onEdit = { onAction(GraphAction.OnOpenEditor(function.id)) },
                onToggleVisibility = { onAction(GraphAction.OnToggleVisibility(function.id)) },
                onDelete = { onAction(GraphAction.OnRemoveFunction(function.id)) }
            )
        }

        if (state.parameters.isNotEmpty()) {
            item { SectionTitle("매개변수", modifier = Modifier.padding(top = 8.dp)) }
            items(state.parameters, key = { "param-${it.name}" }) { parameter ->
                ParameterSlider(
                    parameter = parameter,
                    isAnimating = state.animatingParameter == parameter.name,
                    onValueChange = { onAction(GraphAction.OnParameterChange(parameter.name, it)) },
                    onValueChangeFinished = { onAction(GraphAction.OnParameterChangeFinished(parameter.name)) },
                    onToggleAnimation = { onAction(GraphAction.OnToggleParameterAnimation(parameter.name)) }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { onAction(GraphAction.OnOpenEditor(null)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.PrimaryGold)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("함수 추가", style = AppTextStyles.normalTextBold)
            }
        }

        item {
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
        }
    }
}

/** 목록 구역 제목 (예: "함수", "매개변수") */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTextStyles.smallTextBold,
        color = AppColors.TextSecondary,
        modifier = modifier.padding(start = 4.dp)
    )
}

/**
 * 함수 입력/편집 패널입니다.
 * 상단에 입력 방식(직접 입력/템플릿) 선택과 닫기, 그 아래에 입력창과 고정 키패드를 배치합니다.
 */
@Composable
private fun FunctionEditorPanel(
    state: GraphState,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onAction(GraphAction.OnCloseEditor) }

    // 가로 모드처럼 높이가 낮으면 키패드가 잘리지 않도록 패널 전체를 스크롤
    Column(
        modifier = modifier
            .background(AppColors.Panel)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAction(GraphAction.OnCloseEditor) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = AppColors.TextSecondary
                )
            }
            if (state.editingFunctionId == null) {
                GraphModeToggle(
                    isTemplateMode = state.isTemplateMode,
                    onModeChange = { onAction(GraphAction.OnToggleMode) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "함수 편집",
                    style = AppTextStyles.normalTextBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.isTemplateMode) {
            BeginnerModeInput(state, onAction)
            Button(
                onClick = { onAction(GraphAction.OnSubmitFunction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.PrimaryGold,
                    contentColor = AppColors.OnPrimary
                )
            ) {
                Text("그래프에 추가", style = AppTextStyles.normalTextBold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AdvancedModeEquationBox(state, onAction)
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { onAction(GraphAction.OnSubmitFunction) },
                    modifier = Modifier.height(64.dp).width(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AppColors.PrimaryGold,
                        contentColor = AppColors.OnPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (state.editingFunctionId == null) "추가" else "저장"
                    )
                }
            }
            AdvancedModeKeypad(onAction)
        }
    }
}

@Preview
@Composable
private fun GraphControlsListPreview() {
    GraphControls()
}

@Preview(heightDp = 480)
@Composable
private fun GraphControlsEditorPreview() {
    GraphControls(state = GraphState(isEditorOpen = true))
}
