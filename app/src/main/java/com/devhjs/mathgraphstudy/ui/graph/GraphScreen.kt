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
import androidx.compose.ui.draw.clip
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
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
        Box(
            modifier = Modifier
                .weight(0.5f)
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

        LazyColumn(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Mode Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.TabRow(
                        selectedTabIndex = if (state.isBeginnerMode) 1 else 0,
                        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    ) {
                        androidx.compose.material3.Tab(
                            selected = !state.isBeginnerMode,
                            onClick = { if (state.isBeginnerMode) onAction(GraphAction.OnToggleMode) },
                            text = { Text("고급 모드") }
                        )
                        androidx.compose.material3.Tab(
                            selected = state.isBeginnerMode,
                            onClick = { if (!state.isBeginnerMode) onAction(GraphAction.OnToggleMode) },
                            text = { Text("초보자 모드") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (state.isBeginnerMode) {
                // Beginner Mode UI
                item {
                    BeginnerModeInput(state, onAction)
                }
            } else {
                // Advanced Mode UI
                item {
                    AdvancedModeInput(state, onAction)
                }
            }
            
            item {
                 Spacer(modifier = Modifier.height(16.dp))
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
fun AdvancedModeInput(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    Column {
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
                enabled = state.inputExpression.text.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val inputs = listOf(
                Triple("sin", "sin()", 4),
                Triple("cos", "cos()", 4),
                Triple("tan", "tan()", 4),
                Triple("ln", "ln()", 3),
                Triple("log", "log()", 4),
                Triple("√", "sqrt()", 5),
                Triple("x²", "^2", 2),
                Triple("^", "^", 1),
                Triple("(", "(", 1),
                Triple(")", ")", 1),
                Triple("x", "x", 1),
                Triple("+", "+", 1),
                Triple("-", "-", 1),
                Triple("*", "*", 1),
                Triple("/", "/", 1),
                Triple("π", "pi", 2),
                Triple("e", "e", 1)
            )
            inputs.forEach { (label, value, offset) ->
                SuggestionChip(
                    onClick = {
                        onAction(GraphAction.OnInsertSymbol(value, offset))
                    },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun BeginnerModeInput(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    Column {
        Text("함수 타입 선택", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BeginnerFunctionType.values().forEach { type ->
                val isSelected = state.beginnerFunctionType == type
                SuggestionChip(
                    onClick = { onAction(GraphAction.OnBeginnerTypeChanged(type)) },
                    label = { 
                        Text(
                            text = type.displayName,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        ) 
                    },
                    colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        labelColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("계수 입력", style = MaterialTheme.typography.labelLarge)
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

@Composable
fun CoefficientForm(state: GraphState, onAction: (GraphAction) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("y =", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(8.dp))

        when (state.beginnerFunctionType) {
            BeginnerFunctionType.LINEAR -> {
                // y = ax + b
                CoefficientInput(state, "a", onAction)
                Text("x +", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "b", onAction)
            }
            BeginnerFunctionType.QUADRATIC -> {
                // y = ax^2 + bx + c
                CoefficientInput(state, "a", onAction)
                Text("x² +", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "b", onAction)
                Text("x +", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "c", onAction)
            }
            BeginnerFunctionType.IRRATIONAL -> {
                 // y = a*sqrt(x+b) + c
                CoefficientInput(state, "a", onAction)
                Text("√", style = MaterialTheme.typography.titleLarge)
                Text("( x +", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "b", onAction)
                Text(") +", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "c", onAction)
            }
             BeginnerFunctionType.RATIONAL -> {
                // y = a/(x+b) + c
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     CoefficientInput(state, "a", onAction)
                     androidx.compose.material3.Divider(modifier = Modifier.width(40.dp), thickness = 2.dp)
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text("x +", style = MaterialTheme.typography.bodyLarge)
                         CoefficientInput(state, "b", onAction)
                     }
                }
                Text(" + ", style = MaterialTheme.typography.titleLarge)
                CoefficientInput(state, "c", onAction)
             }
        }
    }
}

@Composable
fun CoefficientInput(state: GraphState, key: String, onAction: (GraphAction) -> Unit) {
    OutlinedTextField(
        value = state.beginnerCoefficients[key] ?: "",
        onValueChange = { onAction(GraphAction.OnCoefficientChanged(key, it)) },
        label = { Text(key) },
        modifier = Modifier
            .width(60.dp)
            .padding(horizontal = 4.dp),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        )
    )
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
        inputExpression = androidx.compose.ui.text.input.TextFieldValue("cos(x)")
    )

    GraphScreen(
        state = sampleState,
        onAction = {}
    )
}
