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

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    if (isTablet) {
        GraphScreenTablet(
            state = state,
            onAction = viewModel::onAction
        )
    } else {
        GraphScreen(
            state = state,
            onAction = viewModel::onAction
        )
    }
}
