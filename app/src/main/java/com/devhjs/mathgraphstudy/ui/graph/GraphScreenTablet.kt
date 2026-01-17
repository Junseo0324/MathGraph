package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.devhjs.mathgraphstudy.ui.components.GraphCanvas
import androidx.compose.ui.graphics.Color
import com.devhjs.mathgraphstudy.domain.model.GraphFunction

@Composable
fun GraphScreenTablet(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    val isPanelVisible = remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Graph Canvas (Full Screen)
            GraphCanvas(
                functions = state.functions,
                viewportScale = state.viewportScale,
                viewportOffsetX = state.viewportOffsetX,
                viewportOffsetY = state.viewportOffsetY,
                intersections = state.intersections,
                onViewportChange = { scale, offsetX, offsetY ->
                    onAction(GraphAction.OnViewportChange(scale, offsetX, offsetY))
                }
            )

            // Layer 2: Sliding Panel with Handle
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(2f), // Higher zIndex than canvas
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Handle
                Surface(
                    onClick = { isPanelVisible.value = !isPanelVisible.value },
                    modifier = Modifier
                        .width(32.dp)
                        .height(64.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPanelVisible.value) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                            contentDescription = if (isPanelVisible.value) "Close Panel" else "Open Panel",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Panel Content
                AnimatedVisibility(
                    visible = isPanelVisible.value,
                    enter = expandHorizontally(expandFrom = Alignment.Start),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(360.dp) // Fixed width for panel
                            .shadow(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        GraphControls(
                            state = state,
                            onAction = onAction,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 1200, heightDp = 800, showBackground = true)
@Composable
fun GraphScreenTabletPreview() {
    val sampleFunctions = listOf(
        GraphFunction(id = "1", expression = "x^2", visualNode = null, color = 0xFFFF0000),
        GraphFunction(id = "2", expression = "sin(x)", visualNode = null, color = 0xFF0000FF)
    )
    val sampleState = GraphState(
        functions = sampleFunctions
    )

    GraphScreenTablet(
        state = sampleState,
        onAction = {}
    )
}
