package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState
import com.devhjs.mathgraphstudy.presentation.graph.GraphViewportState

/**
 * 화면 상태([GraphState])를 [GraphCanvas]에 연결하는 그래프 영역입니다.
 * 세로/가로/태블릿 화면이 같은 연결 코드를 공유합니다.
 */
@Composable
fun GraphArea(
    state: GraphState,
    viewport: GraphViewportState,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier,
    controlsAlignment: Alignment = Alignment.TopEnd
) {
    GraphCanvas(
        modifier = modifier,
        viewport = viewport,
        functions = state.functions,
        parameterValues = state.parameterValues,
        intersections = state.intersections,
        selectedFunctionId = state.selectedFunctionId,
        keyPoints = state.keyPoints,
        selectedPoint = state.selectedPoint,
        controlsAlignment = controlsAlignment,
        onVisibleRangeChange = { startX, endX -> onAction(GraphAction.OnVisibleRangeChange(startX, endX)) },
        onFunctionSelected = { id -> onAction(GraphAction.OnSelectFunction(id)) },
        onPointSelected = { point -> onAction(GraphAction.OnSelectPoint(point)) },
        onPointDismiss = { onAction(GraphAction.OnDismissPointInfo) }
    )
}
