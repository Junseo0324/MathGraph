package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.presentation.graph.GraphAction
import com.devhjs.mathgraphstudy.presentation.graph.GraphState

import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun CoefficientInput(state: GraphState, key: String, onAction: (GraphAction) -> Unit) {
    OutlinedTextField(
        value = state.beginnerCoefficients[key] ?: "",
        onValueChange = { onAction(GraphAction.OnCoefficientChanged(key, it)) },
        label = { Text(key) },
        modifier = Modifier
            .width(60.dp)
            .padding(horizontal = 4.dp),
        singleLine = true,
        textStyle = AppTextStyles.normalTextRegular.copy(color = AppColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.PrimaryGold,
            unfocusedBorderColor = AppColors.BorderColor,
            focusedLabelColor = AppColors.PrimaryGold,
            unfocusedLabelColor = AppColors.TextSecondary,
            cursorColor = AppColors.PrimaryGold
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}