package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

@Composable
fun CoefficientForm(
    state: GraphState = GraphState(),
    onAction: (GraphAction) -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("y =", style = AppTextStyles.largeTextBold, color = AppColors.TextPrimary)
        Spacer(modifier = Modifier.width(8.dp))

        when (state.beginnerFunctionType) {
            BeginnerFunctionType.LINEAR -> {
                // y = ax + b
                CoefficientInput(state, "a", onAction)
                Text("x +", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                CoefficientInput(state, "b", onAction)
            }
            BeginnerFunctionType.QUADRATIC -> {
                // y = ax^2 + bx + c
                CoefficientInput(state, "a", onAction)
                Text("x² +", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                CoefficientInput(state, "b", onAction)
                Text("x +", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                CoefficientInput(state, "c", onAction)
            }
            BeginnerFunctionType.CUBIC -> {
                // y = ax^3 + bx^2 + cx + d
                CoefficientInput(state, "a", onAction)
                Text("x³+", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                CoefficientInput(state, "b", onAction)
                Text("x²+", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                CoefficientInput(state, "c", onAction)
                Text("x+", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                CoefficientInput(state, "d", onAction)
            }
             BeginnerFunctionType.RATIONAL -> {
                 // y = (a/b)x + c
                 Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.padding(end = 4.dp)
                 ) {
                     CoefficientInput(state, "a", onAction)
                     androidx.compose.foundation.layout.Box(
                         modifier = Modifier
                             .width(60.dp)
                             .height(1.dp)
                             .background(AppColors.TextPrimary)
                     )
                     CoefficientInput(state, "b", onAction)
                 }
                 Text("x +", style = AppTextStyles.normalTextRegular, color = AppColors.TextPrimary)
                 CoefficientInput(state, "c", onAction)
             }
        }
    }
}

@Preview(name = "Linear")
@Composable
private fun CoefficientFormPreview_Linear() {
    CoefficientForm(
        state = GraphState(beginnerFunctionType = BeginnerFunctionType.LINEAR)
    )
}

@Preview(name = "QUADRATIC")
@Composable
private fun CoefficientFormPreview_Quadratic() {
    CoefficientForm(
        state = GraphState(beginnerFunctionType = BeginnerFunctionType.QUADRATIC)
    )
}


@Preview(name = "CUBIC")
@Composable
private fun CoefficientFormPreview_Cubic() {
    CoefficientForm(
        state = GraphState(beginnerFunctionType = BeginnerFunctionType.CUBIC)
    )
}


@Preview(name = "RATIONAL")
@Composable
private fun CoefficientFormPreview_Rational() {
    CoefficientForm(
        state = GraphState(beginnerFunctionType = BeginnerFunctionType.RATIONAL)
    )
}


