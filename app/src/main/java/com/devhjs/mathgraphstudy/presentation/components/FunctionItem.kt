package com.devhjs.mathgraphstudy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles
import com.devhjs.mathgraphstudy.presentation.math.MathNodeView

/**
 * 함수 목록의 한 줄입니다.
 *
 * - 왼쪽 색 점: 탭하면 그래프 보이기/숨기기 (숨기면 빈 원)
 * - 수식: 탭하면 편집
 * - 오른쪽: 삭제
 * - 그래프에서 곡선을 탭해 선택한 함수는 함수 색 테두리로 강조
 */
@Composable
fun FunctionItem(
    function: GraphFunction,
    isSelected: Boolean = false,
    onEdit: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val functionColor = Color(function.color)
    val shape = RoundedCornerShape(14.dp)

    Surface(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) functionColor else AppColors.BorderColor,
                shape = shape
            ),
        shape = shape,
        color = AppColors.SurfaceCard
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisibilityDot(
                color = functionColor,
                isVisible = function.isVisible,
                onClick = onToggleVisibility
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    // 숨긴 함수는 흐리게
                    .alpha(if (function.isVisible) 1f else 0.4f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "y =",
                    style = AppTextStyles.normalTextRegular,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                CompositionLocalProvider(LocalContentColor provides AppColors.TextPrimary) {
                    ProvideTextStyle(AppTextStyles.largeTextRegular.copy(color = AppColors.TextPrimary)) {
                        MathNodeView(
                            node = function.node,
                            currentPath = emptyList(),
                            focusPath = listOf(-1),
                            // 목록에서는 편집 포커스 대신 항목 전체 클릭(onEdit)으로 처리
                            onFocusRequest = { onEdit() },
                            editable = false
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "함수 삭제",
                    tint = AppColors.TextSecondary
                )
            }
        }
    }
}

/** 함수 색 점. 보이면 채워진 원, 숨기면 테두리만 있는 원 */
@Composable
private fun VisibilityDot(color: Color, isVisible: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .then(
                    if (isVisible) Modifier.background(color, CircleShape)
                    else Modifier.border(2.dp, color, CircleShape)
                )
        )
    }
}

@Preview
@Composable
private fun FunctionItemPreview() {
    FunctionItem(
        function = GraphFunction(id = "1", node = VariableNode("x"), color = 0xFF42A5F5)
    )
}
