package com.devhjs.mathgraphstudy.presentation.math

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.presentation.designsystem.AppColors
import com.devhjs.mathgraphstudy.presentation.designsystem.AppTextStyles

/**
 * 한 줄 수식 입력기의 화면입니다.
 *
 * - 토큰을 한 줄로 보여주고, 커서 위치에 깜빡이는 막대를 그립니다.
 * - 토큰을 탭하면 그 토큰의 왼쪽/오른쪽 중 가까운 쪽으로 커서가 이동합니다.
 * - `^` 뒤의 지수(숫자/문자/괄호 묶음)는 위첨자로 작게 보여주고 `^` 기호 자체는 숨깁니다.
 */
@Composable
fun ExpressionEditorView(
    state: EditorState,
    onCursorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    // 끝에서 입력 중이면 수식이 길어져도 커서가 보이도록 끝으로 스크롤
    LaunchedEffect(state.tokens.size) {
        if (state.cursor == state.tokens.size) scrollState.animateScrollTo(scrollState.maxValue)
    }

    val levels = remember(state.tokens) { superscriptLevels(state.tokens) }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 맨 앞 터치 영역 (커서를 처음으로)
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(40.dp)
                .pointerInput(Unit) { detectTapGestures { onCursorChange(0) } }
        )
        if (state.tokens.isEmpty()) {
            Cursor(level = 0)
            Text(
                text = "수식을 입력하세요",
                style = AppTextStyles.normalTextRegular,
                color = AppColors.TextSecondary
            )
            return@Row
        }
        state.tokens.forEachIndexed { index, token ->
            if (state.cursor == index) Cursor(levels[index])
            val previous = state.tokens.getOrNull(index - 1)
            TokenText(
                token = token,
                // 함수 이름 앞에 값이 오면 살짝 띄움 (a sin(x) 가 "asin" 처럼 붙어 보이지 않도록)
                spaceBefore = token != "(" && ExpressionEditor.isOpener(token) &&
                    previous != null && !ExpressionEditor.isOpener(previous) && previous !in setOf("+", "-", "*", "/", "^"),
                level = levels[index],
                onTap = { isRightHalf -> onCursorChange(if (isRightHalf) index + 1 else index) }
            )
        }
        if (state.cursor == state.tokens.size) Cursor(level = 0)
    }
}

@Composable
private fun TokenText(token: String, spaceBefore: Boolean, level: Int, onTap: (Boolean) -> Unit) {
    val isVariable = token == "x" || token == "e" || token in Parameter.NAMES
    val text = when (token) {
        "^" -> "" // 지수는 위첨자로 표현하므로 기호 숨김
        "*" -> "×"
        "/" -> "÷"
        "-" -> "−"
        "pi" -> "π"
        "sqrt(" -> "√("
        else -> token
    }
    val isOperator = token in setOf("+", "-", "*", "/")
    val fontSize = when (level) {
        0 -> 22.sp
        1 -> 14.sp
        else -> 11.sp
    }
    Box(
        modifier = Modifier
            // 위첨자는 위로 올려 표시
            .padding(bottom = if (level > 0) (12 + (level - 1) * 6).dp else 0.dp)
            .widthIn(min = if (token == "^") 4.dp else 10.dp)
            .pointerInput(token) {
                detectTapGestures { offset -> onTap(offset.x > size.width / 2) }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTextStyles.normalTextRegular.copy(fontSize = fontSize, lineHeight = fontSize * 1.3f),
            fontStyle = if (isVariable) FontStyle.Italic else FontStyle.Normal,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(
                start = if (spaceBefore) 6.dp else if (isOperator && level == 0) 5.dp else 1.dp,
                end = if (isOperator && level == 0) 5.dp else 1.dp
            )
        )
    }
}

/** 깜빡이는 커서 막대 */
@Composable
private fun Cursor(level: Int) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 530), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    Box(
        modifier = Modifier
            .padding(bottom = if (level > 0) 12.dp else 0.dp)
            .width(2.dp)
            .height(if (level > 0) 18.dp else 28.dp)
            .alpha(alpha)
            .background(AppColors.PrimaryGold)
    )
}

/**
 * 각 토큰의 위첨자 단계(0 = 기본, 1 = 지수, 2 = 지수의 지수)를 계산합니다.
 * `^` 바로 뒤의 숫자 묶음, 문자 하나, 또는 괄호/함수 묶음이 지수가 됩니다.
 */
internal fun superscriptLevels(tokens: List<String>): IntArray {
    val levels = IntArray(tokens.size)
    tokens.forEachIndexed { i, token ->
        if (token != "^" || i + 1 >= tokens.size) return@forEachIndexed
        val start = i + 1
        val end = when {
            ExpressionEditor.isOpener(tokens[start]) ->
                ExpressionEditor.matchingClose(tokens, start) ?: (tokens.size - 1)
            ExpressionEditor.isDigit(tokens[start]) -> {
                var j = start
                while (j + 1 < tokens.size && ExpressionEditor.isDigit(tokens[j + 1])) j++
                j
            }
            else -> start
        }
        for (k in start..end) levels[k] = levels[i] + 1
    }
    return levels
}
