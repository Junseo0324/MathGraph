package com.devhjs.mathgraphstudy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.devhjs.mathgraphstudy.presentation.graph.GraphScreenRoot
import com.devhjs.mathgraphstudy.presentation.designsystem.MathGraphStudyTheme
import com.devhjs.mathgraphstudy.util.AdManager

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            MathGraphStudyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        GraphScreenRoot()
                    }
                }
            }
        }
    }
}