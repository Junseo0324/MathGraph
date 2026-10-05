package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.ui.geometry.Offset
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType

sealed interface GraphAction {
    data class OnInput(val input: String) : GraphAction
    data class OnFocusChange(val path: List<Int>) : GraphAction
    object OnToggleMode : GraphAction
    data class OnOpenEditor(val functionId: String?) : GraphAction // null 이면 새 함수 추가
    object OnCloseEditor : GraphAction
    data class OnBeginnerTypeChanged(val type: BeginnerFunctionType) : GraphAction
    data class OnCoefficientChanged(val key: String, val value: String) : GraphAction
    object OnSubmitFunction : GraphAction // 입력한 함수를 추가하거나, 편집 중인 함수를 교체
    data class OnRemoveFunction(val id: String) : GraphAction
    data class OnToggleVisibility(val id: String) : GraphAction
    data class OnViewportChange(val scale: Float, val offsetX: Float, val offsetY: Float) : GraphAction
    data class OnCanvasSizeChanged(val width: Float) : GraphAction
    data class OnSelectIntersection(val point: Offset) : GraphAction // 교점 선택
    object OnDismissIntersectionInfo : GraphAction // 교점 정보 닫기
    object OnOpenLicenses : GraphAction
    object OnCloseLicenses : GraphAction
}
