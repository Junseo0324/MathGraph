package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles

/**
 * 매개변수 하나를 조절하는 슬라이더 줄입니다. (예: "a = 1.50  ───●───  ▶")
 * 재생 버튼을 누르면 값이 최소 ~ 최대 사이를 자동으로 오갑니다.
 */
@Composable
fun ParameterSlider(
    parameter: Parameter,
    isAnimating: Boolean,
    onValueChange: (Double) -> Unit,
    onValueChangeFinished: () -> Unit,
    onToggleAnimation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = parameter.name,
            style = AppTextStyles.normalTextBold,
            fontStyle = FontStyle.Italic,
            color = AppColors.PrimaryGold
        )
        Text(
            text = " = %.2f".format(parameter.value),
            style = AppTextStyles.normalTextRegular,
            color = AppColors.TextPrimary,
            modifier = Modifier.width(64.dp)
        )
        Slider(
            value = parameter.value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.PrimaryGold,
                activeTrackColor = AppColors.PrimaryGold,
                inactiveTrackColor = AppColors.BorderColor
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        IconButton(onClick = onToggleAnimation) {
            Icon(
                imageVector = if (isAnimating) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isAnimating) "${parameter.name} 정지" else "${parameter.name} 자동 재생",
                tint = AppColors.TextSecondary
            )
        }
    }
}

@Preview
@Composable
private fun ParameterSliderPreview() {
    ParameterSlider(
        parameter = Parameter("a", 1.5),
        isAnimating = false,
        onValueChange = {},
        onValueChangeFinished = {},
        onToggleAnimation = {}
    )
}
