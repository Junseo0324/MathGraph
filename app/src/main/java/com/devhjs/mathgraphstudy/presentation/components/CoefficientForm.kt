package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

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
            BeginnerFunctionType.CUBIC -> {
                // y = ax^3 + bx^2 + cx + d
                // Using a Column/Flow logic might be better if it doesn't fit, 
                // but let's try to be compact or split into two rows implicitly if we could, 
                // but here we are inside a Row. We'll simplify the text to fit or rely on horizontal scroll if we added it (we didn't).
                // Let's try to fit.
                CoefficientInput(state, "a", onAction)
                Text("x³+", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "b", onAction)
                Text("x²+", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "c", onAction)
                Text("x+", style = MaterialTheme.typography.bodyLarge)
                CoefficientInput(state, "d", onAction)
            }
             BeginnerFunctionType.RATIONAL -> {
                 // y = (a/b)x + c
                 androidx.compose.foundation.layout.Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.padding(end = 4.dp)
                 ) {
                     CoefficientInput(state, "a", onAction)
                     androidx.compose.foundation.layout.Box(
                         modifier = Modifier
                             .width(60.dp)
                             .height(1.dp)
                             .background(MaterialTheme.colorScheme.onSurface)
                     )
                     CoefficientInput(state, "b", onAction)
                 }
                 Text("x +", style = MaterialTheme.typography.bodyLarge)
                 CoefficientInput(state, "c", onAction)
             }
        }
    }
}
