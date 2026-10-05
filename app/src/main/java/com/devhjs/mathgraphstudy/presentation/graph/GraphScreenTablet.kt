package com.devhjs.mathgraphstudy.presentation.graph

import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.presentation.components.GraphArea
import com.devhjs.mathgraphstudy.presentation.components.GraphControls

@Composable
fun GraphScreenTablet(
    viewport: GraphViewportState = rememberGraphViewportState(),
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    var isPanelVisible by rememberSaveable { mutableStateOf(true) }

    // 그래프와 패널을 겹치지 않게 좌우로 배치 (패널을 접으면 그래프가 넓어짐)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            GraphArea(
                state = state,
                viewport = viewport,
                onAction = onAction,
                controlsAlignment = Alignment.TopStart // 오른쪽 패널 손잡이와 겹치지 않도록
            )

            // 패널 열기/닫기 손잡이
            Surface(
                onClick = { isPanelVisible = !isPanelVisible },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(32.dp)
                    .height(64.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                color = AppColors.PrimaryGold,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPanelVisible) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = if (isPanelVisible) "패널 닫기" else "패널 열기",
                        tint = AppColors.OnPrimary
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isPanelVisible,
            enter = expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            GraphControls(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
            )
        }
    }
}

@Preview(widthDp = 1200, heightDp = 800, showBackground = true)
@Composable
fun GraphScreenTabletPreview() {
    val sampleFunctions = listOf(
        GraphFunction(id = "1", node = PowerNode(VariableNode("x"), NumberNode("2")), color = 0xFFFF0000),
        GraphFunction(id = "2", node = FunctionNode(MathFunction.SIN, VariableNode("x")), color = 0xFF0000FF)
    )
    val sampleState = GraphState(
        functions = sampleFunctions
    )

    GraphScreenTablet(
        state = sampleState,
        onAction = {}
    )
}
