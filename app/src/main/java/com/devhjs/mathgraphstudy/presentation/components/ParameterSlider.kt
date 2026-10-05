package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles

/**
 * 매개변수 하나를 조절하는 슬라이더 카드입니다.
 *
 * ```
 *  [a] 1.50  ━━━━━━━●────────  ( ▶ )
 *            -10            10
 * ```
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
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceCard, RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.BorderColor, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 매개변수 이름 배지
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AppColors.PrimaryGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = parameter.name,
                style = AppTextStyles.normalTextBold,
                fontStyle = FontStyle.Italic,
                color = AppColors.PrimaryGold
            )
        }
        Text(
            text = "%.2f".format(parameter.value),
            style = AppTextStyles.normalTextBold,
            color = AppColors.TextPrimary,
            modifier = Modifier
                .padding(start = 8.dp)
                .width(52.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            GoldSlider(
                value = parameter.value.toFloat(),
                valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
                onValueChange = { onValueChange(it.toDouble()) },
                onValueChangeFinished = onValueChangeFinished
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatBound(parameter.min), style = AppTextStyles.captionRegular, color = AppColors.TextSecondary)
                Text(formatBound(parameter.max), style = AppTextStyles.captionRegular, color = AppColors.TextSecondary)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        PlayButton(isAnimating = isAnimating, name = parameter.name, onClick = onToggleAnimation)
    }
}

/** 재생/정지 원형 버튼. 재생 중이면 강조색으로 채워집니다. */
@Composable
private fun PlayButton(isAnimating: Boolean, name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = if (isAnimating) AppColors.PrimaryGold else AppColors.KeyOperator,
        contentColor = if (isAnimating) AppColors.OnPrimary else AppColors.TextPrimary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isAnimating) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isAnimating) "$name 정지" else "$name 자동 재생",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 얇은 트랙과 둥근 썸으로 그린 슬라이더입니다.
 * 손가락으로 잡고 있는 동안 썸이 조금 커져 조작 중임을 알려줍니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val thumbSize = if (isPressed || isDragged) 24.dp else 20.dp

    val activeColor = AppColors.PrimaryGold
    val inactiveColor = AppColors.BorderColor
    val thumbBorderColor = AppColors.SurfaceCard

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .shadow(3.dp, CircleShape)
                    .background(activeColor, CircleShape)
                    .border(3.dp, thumbBorderColor, CircleShape)
            )
        },
        track = { sliderState ->
            val range = sliderState.valueRange
            val fraction = ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                val y = size.height / 2
                drawLine(inactiveColor, Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
                drawLine(activeColor, Offset(0f, y), Offset(size.width * fraction, y), size.height, StrokeCap.Round)
            }
        }
    )
}

/** 범위 표시용 숫자 (정수면 소수점 생략) */
private fun formatBound(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)

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
