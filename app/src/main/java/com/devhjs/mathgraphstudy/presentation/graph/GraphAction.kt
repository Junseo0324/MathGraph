package com.devhjs.mathgraphstudy.presentation.graph

import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType

sealed interface GraphAction {
    data class OnInput(val input: String) : GraphAction
    data class OnCursorChange(val index: Int) : GraphAction // 수식을 탭해 커서 이동
    object OnToggleMode : GraphAction
    data class OnOpenEditor(val functionId: String?) : GraphAction // null 이면 새 함수 추가
    object OnCloseEditor : GraphAction
    data class OnBeginnerTypeChanged(val type: BeginnerFunctionType) : GraphAction
    data class OnCoefficientChanged(val key: String, val value: String) : GraphAction
    object OnSubmitFunction : GraphAction // 입력한 함수를 추가하거나, 편집 중인 함수를 교체
    data class OnRemoveFunction(val id: String) : GraphAction
    data class OnToggleVisibility(val id: String) : GraphAction
    data class OnVisibleRangeChange(val startX: Double, val endX: Double) : GraphAction // 확대/이동이 멈춘 뒤 보이는 x 범위
    data class OnSelectFunction(val id: String?) : GraphAction // 곡선 탭으로 함수 선택 (null 이면 해제)
    data class OnSelectPoint(val point: GraphPoint) : GraphAction // 교점/특징점 선택
    object OnDismissPointInfo : GraphAction // 좌표 정보 닫기
    data class OnParameterChange(val name: String, val value: Double) : GraphAction // 슬라이더 드래그 중
    data class OnParameterChangeFinished(val name: String) : GraphAction // 슬라이더 놓음 (저장)
    data class OnToggleParameterAnimation(val name: String) : GraphAction // 매개변수 자동 재생/정지
    object OnOpenLicenses : GraphAction
    object OnCloseLicenses : GraphAction
}
