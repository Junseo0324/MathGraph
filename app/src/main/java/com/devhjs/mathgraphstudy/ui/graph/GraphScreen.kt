package com.devhjs.mathgraphstudy.ui.graph

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import com.devhjs.mathgraphstudy.ui.components.GraphCanvas

@Composable
fun GraphScreen(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    
    DisposableEffect(configuration.orientation) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
             // Ensure bars are shown when leaving screen or composition if needed, 
             // though rotation usually handles it by recomposing or Activity recreation.
             // But valid to reset safely if orientation changes back.
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

@Composable
fun GraphContentPortrait(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top: Graph Area
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth()
        ) {
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
        }

        // Bottom: Controls
        LazyColumn(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Input Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputExpression,
                        onValueChange = { onAction(GraphAction.OnExpressionChanged(it)) },
                        label = { Text("수식 입력 (예: sin(x) + x^2)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAction(GraphAction.OnAddFunction) },
                        enabled = state.inputExpression.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick Input Chips
            item {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val inputs = listOf(
                        "sin" to "sin(", "cos" to "cos(", "tan" to "tan(",
                        "ln" to "ln(", "log" to "log(",
                        "√" to "sqrt(", "x²" to "^2", "^" to "^",
                        "(" to "(", ")" to ")",
                        "x" to "x", "+" to "+", "-" to "-", "*" to "*", "/" to "/",
                        "π" to "pi", "e" to "e"
                    )
                    inputs.forEach { (label, value) ->
                        SuggestionChip(
                            onClick = {
                                onAction(GraphAction.OnExpressionChanged(state.inputExpression + value))
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Function List
            item {
                Text(
                    text = "함수 목록",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
        }
    }
}

@Composable
fun FunctionItem(
    function: GraphFunction,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(function.color, shape = MaterialTheme.shapes.small)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "y = ${function.expression}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (function.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GraphScreenPreview() {
    val sampleFunctions = listOf(
        GraphFunction("1", "x^2", Color.Red),
        GraphFunction("2", "sin(x)", Color.Blue)
    )
    val sampleState = GraphState(
        functions = sampleFunctions,
        inputExpression = "cos(x)"
    )
    
    MaterialTheme {
        GraphScreen(
            state = sampleState,
            onAction = {}
        )
    }
}
