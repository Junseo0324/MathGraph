package com.devhjs.mathgraphstudy.presentation.components

import com.devhjs.mathgraphstudy.presentation.graph.GraphViewportState
import com.devhjs.mathgraphstudy.presentation.graph.rememberGraphViewportState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

@Composable
fun GraphContentPortrait(
    viewport: GraphViewportState = rememberGraphViewportState(),
    state: GraphState = GraphState(),
    onAction: (GraphAction) -> Unit= {}
) {
    // 평소에는 그래프를 크게(60%), 입력 중에는 키패드가 필요한 만큼만 차지하고 나머지를 그래프에 할당
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .weight(if (state.isEditorOpen) 1f else 0.6f)
                .fillMaxWidth()
        ) {
            GraphArea(
                state = state,
                viewport = viewport,
                onAction = onAction
            )
        }

        GraphControls(
            state = state,
            onAction = onAction,
            modifier = if (state.isEditorOpen) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
            }
        )
    }
}

@Preview()
@Composable
private fun GraphContentPortraitPreview() {
    GraphContentPortrait()
}