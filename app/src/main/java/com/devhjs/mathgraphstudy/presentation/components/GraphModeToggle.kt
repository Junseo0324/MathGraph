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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun GraphModeToggle(
    isBeginnerMode: Boolean,
    onModeChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 배경 컨테이너
    BoxWithConstraints(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 2

        // 움직이는 인디케이터 (슬라이딩 효과)
        // 초보자 모드(isBeginnerMode=true)일 때 오른쪽(1), 고급 모드(isBeginnerMode=false)일 때 왼쪽(0)
        // 기존 코드 로직상: 0=고급, 1=초보자
        val indicatorOffset by animateDpAsState(
            targetValue = if (isBeginnerMode) tabWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "indicatorOffset"
        )

        // 인디케이터
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
                .zIndex(1f) 
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f) // Row 자체를 인디케이터 위로 올림
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



@Preview
@Composable
private fun GraphModeTogglePreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GraphModeToggle(
                isBeginnerMode = false,
                onModeChange = {},
                modifier = Modifier.width(300.dp)
            )
        }
    }
}
