package com.devhjs.mathgraphstudy.presentation.graph

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.presentation.components.GraphCanvas
import com.devhjs.mathgraphstudy.presentation.components.GraphContentPortrait

@Composable
fun GraphScreen(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    val configuration = LocalConfiguration.current
    val view = LocalView.current

    DisposableEffect(configuration.orientation) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val windowDispose = (view.context as? Activity)?.window
            if (windowDispose != null) {
                val controller = WindowCompat.getInsetsController(windowDispose, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
    } else {
        GraphContentPortrait(state, onAction)
    }
}


@Preview(showBackground = true)
@Composable
fun GraphScreenPreview() {
    val sampleFunctions = listOf(
        GraphFunction(id = "1", expression = "x^2", visualNode = null, color = 0xFFFF0000),
        GraphFunction(id = "2", expression = "sin(x)", visualNode = null, color = 0xFF0000FF)
    )
    val sampleState = GraphState(
        functions = sampleFunctions,
    )

    GraphScreen(
        state = sampleState,
        onAction = {}
    )
}
