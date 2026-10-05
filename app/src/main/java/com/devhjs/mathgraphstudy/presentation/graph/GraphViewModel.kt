package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devhjs.mathgraphstudy.core.di.DefaultDispatcher
import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.FunctionInput
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.usecase.BuildFunctionNodeUseCase
import com.devhjs.mathgraphstudy.domain.usecase.CalculateIntersectionsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.DeleteGraphFunctionUseCase
import com.devhjs.mathgraphstudy.domain.usecase.ObserveGraphFunctionsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.SaveGraphFunctionUseCase
import com.devhjs.mathgraphstudy.presentation.math.MathInputManager
import com.devhjs.mathgraphstudy.presentation.math.MathInputState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val buildFunctionNodeUseCase: BuildFunctionNodeUseCase,
    private val observeGraphFunctionsUseCase: ObserveGraphFunctionsUseCase,
    private val saveGraphFunctionUseCase: SaveGraphFunctionUseCase,
    private val deleteGraphFunctionUseCase: DeleteGraphFunctionUseCase,
    private val calculateIntersectionsUseCase: CalculateIntersectionsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _state = MutableStateFlow(GraphState())
    val state: StateFlow<GraphState> = _state.asStateFlow()

    private val _events = Channel<GraphEvent>()
    val events = _events.receiveAsFlow()

    private var intersectionJob: Job? = null
    private var functionAddedCount = 0

    init {
        observeFunctions()
    }

    /**
     * 사용자의 UI 액션을 처리하고, 그에 따라 상태(State)를 업데이트합니다.
     * 입력 처리, 입력 패널 열고 닫기, 함수 추가/편집/삭제, 보이는 범위 변경 등을 담당합니다.
     */
    fun onAction(action: GraphAction) {
        when (action) {
            is GraphAction.OnInput -> {
                 _state.update {
                     val newInputState = MathInputManager.processInput(it.mathInput, action.input)
                     it.copy(mathInput = newInputState)
                 }
            }
            is GraphAction.OnFocusChange -> {
                _state.update {
                    val newInputState = MathInputManager.onFocusChange(it.mathInput, action.path)
                    it.copy(mathInput = newInputState)
                }
            }
            GraphAction.OnToggleMode -> {
                _state.update { it.copy(isTemplateMode = !it.isTemplateMode) }
            }
            is GraphAction.OnOpenEditor -> {
                val target = action.functionId?.let { id -> _state.value.functions.find { it.id == id } }
                _state.update {
                    it.copy(
                        isEditorOpen = true,
                        editingFunctionId = target?.id,
                        // 기존 함수는 직접 입력 모드에서 수식을 그대로 불러와 편집
                        isTemplateMode = if (target != null) false else it.isTemplateMode,
                        mathInput = if (target != null) {
                            MathInputState(
                                rootNode = target.node,
                                focusPath = MathInputManager.endOfExpressionPath(target.node)
                            )
                        } else {
                            MathInputState()
                        },
                        beginnerCoefficients = emptyMap()
                    )
                }
            }
            GraphAction.OnCloseEditor -> {
                _state.update { it.copy(isEditorOpen = false, editingFunctionId = null, mathInput = MathInputState()) }
            }
            is GraphAction.OnBeginnerTypeChanged -> {
                _state.update { it.copy(
                    beginnerFunctionType = action.type,
                    beginnerCoefficients = emptyMap()
                ) }
            }
            is GraphAction.OnCoefficientChanged -> {
                _state.update {
                    val newCoefficients = it.beginnerCoefficients.toMutableMap()
                    newCoefficients[action.key] = action.value
                    it.copy(beginnerCoefficients = newCoefficients)
                }
            }
            GraphAction.OnSubmitFunction -> submitFunction()
            is GraphAction.OnRemoveFunction -> {
                viewModelScope.launch {
                    when (val result = deleteGraphFunctionUseCase(action.id)) {
                        is Result.Success -> Unit
                        is Result.Error -> sendError(result.error.message())
                    }
                }
            }
            is GraphAction.OnToggleVisibility -> {
                val function = _state.value.functions.find { it.id == action.id } ?: return
                saveFunction(function.copy(isVisible = !function.isVisible))
            }
            is GraphAction.OnVisibleRangeChange -> {
                _state.update { state: GraphState ->
                    state.copy(
                        visibleStartX = action.startX,
                        visibleEndX = action.endX,
                        selectedIntersection = null // 뷰포트 변경 시 선택 해제
                    )
                }
                triggerIntersectionCalculation()
            }
            is GraphAction.OnSelectIntersection -> {
                _state.update { it.copy(selectedIntersection = action.point) }
            }
            GraphAction.OnDismissIntersectionInfo -> {
                _state.update { it.copy(selectedIntersection = null) }
            }
            GraphAction.OnOpenLicenses,
            GraphAction.OnCloseLicenses -> Unit
        }
    }

    /**
     * 저장된 함수 목록을 구독합니다.
     * 추가/편집/삭제는 저장소에 쓰기만 하고, 화면 목록은 항상 이 흐름으로 갱신됩니다.
     */
    private fun observeFunctions() {
        viewModelScope.launch {
            observeGraphFunctionsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update { it.copy(functions = result.data) }
                        triggerIntersectionCalculation()
                    }
                    is Result.Error -> sendError(result.error.message())
                }
            }
        }
    }

    /**
     * 입력한 수식으로 새 함수를 추가하거나, 편집 중인 함수의 수식을 교체합니다.
     * 수식이 올바르지 않으면 입력 패널을 연 채로 오류를 알립니다.
     */
    private fun submitFunction() {
        val currentState = _state.value
        val input = if (currentState.isTemplateMode) {
            FunctionInput.Template(currentState.beginnerFunctionType, currentState.beginnerCoefficients)
        } else {
            FunctionInput.Expression(currentState.mathInput.rootNode)
        }

        val node = when (val result = buildFunctionNodeUseCase(input)) {
            is Result.Success -> result.data
            is Result.Error -> {
                sendError(result.error.toMessage())
                return
            }
        }

        val editingFunction = currentState.functions.find { it.id == currentState.editingFunctionId }
        // 편집: id, 색상, 표시 여부는 유지하고 수식만 교체
        val function = editingFunction?.copy(node = node) ?: GraphFunction(
            id = UUID.randomUUID().toString(),
            node = node,
            color = nextFunctionColor(currentState.functions),
            isVisible = true,
            createdAt = System.currentTimeMillis()
        )

        _state.update { state: GraphState ->
            state.copy(
                isEditorOpen = false,
                editingFunctionId = null,
                mathInput = MathInputState(),
                beginnerCoefficients = emptyMap()
            )
        }
        saveFunction(function)

        if (editingFunction == null) {
            functionAddedCount++
            if (functionAddedCount % 5 == 0) {
                viewModelScope.launch {
                    _events.send(GraphEvent.ShowInterstitialAd)
                }
            }
        }
    }

    private fun saveFunction(function: GraphFunction) {
        viewModelScope.launch {
            when (val result = saveGraphFunctionUseCase(function)) {
                is Result.Success -> Unit
                is Result.Error -> sendError(result.error.message())
            }
        }
    }

    private fun DataError.message(): String = when (this) {
        is DataError.Local -> toMessage()
    }

    /**
     * 사용자에게 보여줄 오류 메시지를 일회성 이벤트로 전달합니다.
     */
    private fun sendError(message: String) {
        viewModelScope.launch {
            _events.send(GraphEvent.ShowError(message))
        }
    }

    /**
     * 그래프 함수나 보이는 범위가 변경될 때 교차점 계산을 요청합니다.
     * 진행 중인 계산이 있으면 취소하고 최신 상태로 다시 계산합니다.
     */
    private fun triggerIntersectionCalculation() {
        intersectionJob?.cancel()
        intersectionJob = viewModelScope.launch {
            val intersections = calculateIntersections(_state.value)
            _state.update { it.copy(intersections = intersections) }
        }
    }

    /**
     * 현재 보이는 범위(양옆 여유 포함) 내에서 활성화된 함수들 간의 교차점을
     * 백그라운드 스레드에서 비동기로 계산합니다.
     */
    private suspend fun calculateIntersections(state: GraphState): List<Offset> = withContext(defaultDispatcher) {
        val buffer = (state.visibleEndX - state.visibleStartX) * 0.1
        val intersections = calculateIntersectionsUseCase(
            functions = state.functions,
            rangeStart = state.visibleStartX - buffer,
            rangeEnd = state.visibleEndX + buffer
        )

        intersections.map { (x, y) ->
            Offset(x.toFloat(), y.toFloat())
        }
    }

    /**
     * 그래프 선을 그릴 때 사용할 색상을 팔레트에서 고릅니다.
     * 어두운 배경에서 잘 보이는 색 중 아직 쓰이지 않은 색을 우선 사용합니다.
     */
    private fun nextFunctionColor(functions: List<GraphFunction>): Long {
        val usedColors = functions.map { it.color }.toSet()
        return FUNCTION_COLORS.firstOrNull { it !in usedColors }
            ?: FUNCTION_COLORS[functions.size % FUNCTION_COLORS.size]
    }

    private companion object {
        val FUNCTION_COLORS = listOf(
            0xFF42A5F5, // Blue
            0xFFEF5350, // Red
            0xFF66BB6A, // Green
            0xFFFFCA28, // Amber
            0xFFAB47BC, // Purple
            0xFF26C6DA, // Cyan
            0xFFFF7043, // Deep Orange
            0xFFEC407A  // Pink
        )
    }
}
