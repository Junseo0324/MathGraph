package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState
import com.devhjs.mathgraphstudy.presentation.math.MathNodeView

@Composable
fun AdvancedModeInput(
    state: GraphState,
    onAction: (GraphAction) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MathNodeView(
                    node = state.mathInput.rootNode,
                    currentPath = emptyList(),
                    focusPath = state.mathInput.focusPath,
                    onFocusRequest = { onAction(GraphAction.OnFocusChange(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = { onAction(GraphAction.OnAddFunction) },
                enabled = state.mathInput.rootNode !is PlaceholderNode
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("추가")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            @Composable
            fun buttonRow(items: List<String>) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { label ->
                        SuggestionChip(
                            onClick = {
                                val input = when (label) {
                                    "×" -> "*"
                                    "÷" -> "/"
                                    "𝑥" -> "x"
                                    else -> label
                                }
                                onAction(GraphAction.OnInput(input))
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Row 1: Functions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("sin", "cos", "tan", "log", "ln").forEach { label ->
                    SuggestionChip(
                        onClick = { onAction(GraphAction.OnInput(label)) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("7", "8", "9", "÷", "√").forEach { label ->
                    SuggestionChip(
                        onClick = {
                            val input = if (label == "÷") "/" else label
                            onAction(GraphAction.OnInput(input))
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("4", "5", "6", "×", "^").forEach { label ->
                    SuggestionChip(
                        onClick = {
                            val input = if (label == "×") "*" else label
                            onAction(GraphAction.OnInput(input))
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 4
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("1", "2", "3", "-", "⌫").forEach { label ->
                    SuggestionChip(
                        onClick = {
                            val input = if (label == "⌫") "DEL" else label
                            onAction(GraphAction.OnInput(input))
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("0", ".", "x", "+", "→").forEach { label ->
                    SuggestionChip(
                        onClick = { onAction(GraphAction.OnInput(label)) },
                        label = {
                            Text(
                                if (label == "x") "𝑥" else label, // Italic x
                                fontStyle = if (label == "x") FontStyle.Italic else FontStyle.Normal
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}