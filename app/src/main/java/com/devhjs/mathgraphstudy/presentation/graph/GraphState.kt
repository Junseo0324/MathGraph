package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.KeyPoint
import com.devhjs.mathgraphstudy.domain.model.Parameter
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
    val visibleStartX: Double = -15.0, // 화면에 보이는 x 범위 (교점 탐색 범위)
    val visibleEndX: Double = 15.0,
    val intersections: List<Offset> = emptyList(),
    val parameters: List<Parameter> = emptyList(), // 함수에 쓰인 매개변수 (슬라이더)
    val animatingParameter: String? = null, // 자동으로 움직이는 중인 매개변수 이름
    val selectedFunctionId: String? = null, // 곡선을 탭해 선택한 함수 (특징점 표시)
    val keyPoints: List<KeyPoint> = emptyList(), // 선택한 함수의 근, y절편, 극대, 극소
    val selectedPoint: GraphPoint? = null // 탭한 교점/특징점 (좌표 표시용)
) {
    /** 수식 계산에 넘길 매개변수 값 (이름 -> 값) */
    val parameterValues: Map<String, Double> get() = parameters.associate { it.name to it.value }
}

/** 그래프 위에서 좌표를 보여줄 점 (예: "극대 (1.57, 2.00)") */
@Immutable
data class GraphPoint(val x: Double, val y: Double, val label: String)
