package com.devhjs.mathgraphstudy.presentation.graph

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devhjs.mathgraphstudy.util.AdManager

@Composable
fun GraphScreenRoot(
    viewModel: GraphViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GraphEvent.ShowError -> {

                }
                GraphEvent.ShowInterstitialAd -> {
                    if (context is Activity) {
                        AdManager.showInterstitial(context)
                    }
                }
            }
        }
    }
    
    val state by viewModel.state.collectAsState()

    val configuration = LocalConfiguration.current
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
