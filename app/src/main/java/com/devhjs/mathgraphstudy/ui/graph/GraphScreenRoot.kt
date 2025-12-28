package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GraphScreenRoot(
    viewModel: GraphViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    GraphScreen(
        state = state,
        onAction = viewModel::onAction
    )
}
