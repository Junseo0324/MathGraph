package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GraphScreenRoot(
    viewModel: GraphViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GraphEvent.ShowError -> {
                    // Handle error (e.g. show toast)
                }
                GraphEvent.ShowInterstitialAd -> {
                    if (context is android.app.Activity) {
                        com.devhjs.mathgraphstudy.util.AdManager.showInterstitial(context)
                    }
                }
            }
        }
    }
    
    val state by viewModel.state.collectAsState()

    GraphScreen(
        state = state,
        onAction = viewModel::onAction
    )
}
