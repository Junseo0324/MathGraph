package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.presentation.math.MathInputState


@Immutable
data class GraphState(
    val functions: List<GraphFunction> = emptyList(),
    val mathInput: MathInputState = MathInputState(),
    val isEditorOpen: Boolean = false, // 함수 입력/편집 패널이 열려 있는지
    val editingFunctionId: String? = null, // 편집 중인 기존 함수 id (null 이면 새 함수 추가)
    val isTemplateMode: Boolean = false, // 직접 입력 대신 템플릿(일차/이차...) 계수 입력을 사용하는지
    val beginnerFunctionType: BeginnerFunctionType = BeginnerFunctionType.LINEAR,
    val beginnerCoefficients: Map<String, String> = emptyMap(),
    val viewportScale: Float = 40f, // Pixels per unit
    val viewportOffsetX: Float = 0f,
    val viewportOffsetY: Float = 0f,
    val canvasWidth: Float = 1080f, // 교점 탐색 범위 계산용 캔버스 가로 크기 (px)
    val intersections: List<Offset> = emptyList(),
    val selectedIntersection: Offset? = null // 선택된 교점 (클릭 시 좌표 표시용)
)
