package com.devhjs.mathgraphstudy.presentation.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 앱 전용 색상 팔레트입니다. 다크/라이트 테마마다 하나씩 정의하고,
 * [MathGraphStudyTheme]이 시스템 설정에 맞는 팔레트를 [LocalAppColors]로 제공합니다.
 */
@Immutable
data class AppColorScheme(
    val background: Color, // 화면/그래프 배경
    val panel: Color, // 하단(측면) 조작 패널 배경
    val surfaceCard: Color, // 카드, 목록 항목
    val primaryGold: Color, // 강조색 (선택, 확인 버튼, 커서)
    val primaryGoldVariant: Color,
    val onPrimary: Color, // 강조색 위 글자
    val blueAccent: Color, // 연산자 키 등 보조 강조색
    val textPrimary: Color,
    val textSecondary: Color,
    val borderColor: Color,
    val gridColor: Color, // 그래프 격자
    val axisColor: Color, // 그래프 x/y 축
    val red500: Color,
    val keyNumber: Color, // 키패드 숫자 키 배경
    val keyOperator: Color, // 키패드 변수/연산자 키 배경
    val keyFunction: Color, // 키패드 함수/이동 키 배경
    val tooltipBackground: Color // 그래프 위 좌표 표시 상자
)

val DarkAppColors = AppColorScheme(
    background = Color(0xFF121212),
    panel = Color(0xFF1E1E1E),
    surfaceCard = Color(0xFF262626),
    primaryGold = Color(0xFFFFD700),
    primaryGoldVariant = Color(0xFFFFD54F),
    onPrimary = Color(0xFF121212),
    blueAccent = Color(0xFF42A5F5),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    borderColor = Color(0xFF333333),
    gridColor = Color(0xFF262626),
    axisColor = Color(0xFFE0E0E0),
    red500 = Color(0xFFEF4444),
    keyNumber = Color(0xFF34343A),
    keyOperator = Color(0xFF2A2A30),
    keyFunction = Color(0xFF26262B),
    tooltipBackground = Color(0xE6282828)
)

val LightAppColors = AppColorScheme(
    background = Color(0xFFFFFFFF),
    panel = Color(0xFFF4F4F6),
    surfaceCard = Color(0xFFFFFFFF),
    primaryGold = Color(0xFFC99A00), // 흰 배경에서도 읽히도록 어둡게
    primaryGoldVariant = Color(0xFFFFD54F),
    onPrimary = Color(0xFFFFFFFF),
    blueAccent = Color(0xFF1E6FD9),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6B6B),
    borderColor = Color(0xFFDADADA),
    gridColor = Color(0xFFEAEAEA),
    axisColor = Color(0xFF3A3A3A),
    red500 = Color(0xFFDC2626),
    keyNumber = Color(0xFFFFFFFF),
    keyOperator = Color(0xFFE9EAEE),
    keyFunction = Color(0xFFE2E3E8),
    tooltipBackground = Color(0xF2FFFFFF)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * 현재 테마의 색상에 접근하는 단축 객체입니다. (Composable 안에서만 사용 가능)
 * Canvas 그리기 람다처럼 Composable 이 아닌 곳에서는 바깥에서 값을 미리 읽어 두고 사용합니다.
 */
object AppColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.background
    val Panel: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.panel
    val SurfaceCard: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceCard
    val PrimaryGold: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryGold
    val PrimaryGoldVariant: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryGoldVariant
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onPrimary
    val BlueAccent: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.blueAccent
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary
    val BorderColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.borderColor
    val GridColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.gridColor
    val AxisColor: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.axisColor
    val Red500: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.red500
    val KeyNumber: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.keyNumber
    val KeyOperator: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.keyOperator
    val KeyFunction: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.keyFunction
    val TooltipBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.tooltipBackground
}
