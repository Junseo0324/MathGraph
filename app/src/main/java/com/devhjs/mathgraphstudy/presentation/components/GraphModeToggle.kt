package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors

@Composable
fun GraphModeToggle(
    isBeginnerMode: Boolean,
    onModeChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 배경 컨테이너
    BoxWithConstraints(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceCard.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 2

        val indicatorOffset by animateDpAsState(
            targetValue = if (isBeginnerMode) tabWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.TextSecondary)
                .zIndex(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        ) {
            // 고급 모드 탭
            ToggleTabItem(
                text = "고급 모드",
                isSelected = !isBeginnerMode,
                modifier = Modifier.width(tabWidth),
                onClick = { if (isBeginnerMode) onModeChange() }
            )
            // 초보자 모드 탭
            ToggleTabItem(
                text = "초보자 모드",
                isSelected = isBeginnerMode,
                modifier = Modifier.width(tabWidth),
                onClick = { if (!isBeginnerMode) onModeChange() }
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun GraphModeTogglePreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        GraphModeToggle(
            isBeginnerMode = false,
            onModeChange = {},
            modifier = Modifier.width(300.dp)
        )
    }
}
