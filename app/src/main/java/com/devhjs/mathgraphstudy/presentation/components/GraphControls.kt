package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.presentation.graph.AdvancedModeInput
import com.devhjs.mathgraphstudy.presentation.graph.BeginnerModeInput
import com.devhjs.mathgraphstudy.presentation.graph.FunctionItem
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

@Composable
fun GraphControls(
    state: GraphState,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background), // Use background (#121212) so Cards (#1E1E1E) stand out
        contentPadding = PaddingValues(16.dp)
    ) {
        // Mode Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TabRow(
                    selectedTabIndex = if (state.isBeginnerMode) 1 else 0,
                    modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = !state.isBeginnerMode,
                        onClick = { if (state.isBeginnerMode) onAction(GraphAction.OnToggleMode) },
                        text = { Text("고급 모드") }
                    )
                    Tab(
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