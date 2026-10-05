package com.devhjs.mathgraphstudy.presentation.graph

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devhjs.mathgraphstudy.presentation.license.OpenSourceLicenseScreen
import com.devhjs.mathgraphstudy.util.AdManager

@Composable
fun GraphScreenRoot(
    viewModel: GraphViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GraphEvent.ShowError -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
                GraphEvent.ShowInterstitialAd -> {
                    if (context is Activity) {
                        AdManager.showInterstitial(context)
                    }
                }
            }
        }
    }
    
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 확대/이동 상태는 화면 구성(세로/가로/태블릿)이 바뀌어도 유지되도록 여기서 한 번만 생성
    val viewport = rememberGraphViewportState()

    // 라이센스 화면을 표시할지 여부
    var showLicenses by remember { mutableStateOf(false) }

    // Tablet 확인
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    Box(modifier = Modifier.fillMaxSize()) {
        if (showLicenses) {
            OpenSourceLicenseScreen(
                onAction = { action ->
                    if (action is GraphAction.OnCloseLicenses) {
                        showLicenses = false
                    } else {
                        viewModel.onAction(action)
                    }
                }
            )
        } else if (isTablet) {
            GraphScreenTablet(
                viewport = viewport,
                state = state,
                onAction = { action ->
                    if (action is GraphAction.OnOpenLicenses) {
                        showLicenses = true
                    } else {
                        viewModel.onAction(action)
                    }
                }
            )
        } else {
            GraphScreen(
                viewport = viewport,
                state = state,
                onAction = { action ->
                    if (action is GraphAction.OnOpenLicenses) {
                        showLicenses = true
                    } else {
                        viewModel.onAction(action)
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
