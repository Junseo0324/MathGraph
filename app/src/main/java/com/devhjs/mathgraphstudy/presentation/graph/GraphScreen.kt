package com.devhjs.mathgraphstudy.presentation.graph

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.presentation.components.GraphCanvas
import com.devhjs.mathgraphstudy.presentation.components.GraphContentPortrait
import com.devhjs.mathgraphstudy.presentation.math.MathNodeView

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
            val windowDispose = (view.context as? android.app.Activity)?.window
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
        // mathInput is default, no inputExpression
    )

    GraphScreen(
        state = sampleState,
        onAction = {}
    )
}
