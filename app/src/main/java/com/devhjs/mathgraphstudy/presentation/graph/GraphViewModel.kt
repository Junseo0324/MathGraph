package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devhjs.mathgraphstudy.core.di.DefaultDispatcher
import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.FunctionInput
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.KeyPoint
import com.devhjs.mathgraphstudy.domain.model.Parameter
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.usecase.BuildFunctionNodeUseCase
import com.devhjs.mathgraphstudy.domain.usecase.CalculateIntersectionsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.DeleteGraphFunctionUseCase
import com.devhjs.mathgraphstudy.domain.usecase.FindKeyPointsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.ObserveGraphFunctionsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.ObserveParametersUseCase
import com.devhjs.mathgraphstudy.domain.usecase.SaveGraphFunctionUseCase
import com.devhjs.mathgraphstudy.domain.usecase.SaveParameterUseCase
import com.devhjs.mathgraphstudy.presentation.math.MathInputManager
import com.devhjs.mathgraphstudy.presentation.math.MathInputState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val findKeyPointsUseCase: FindKeyPointsUseCase,
    private val observeParametersUseCase: ObserveParametersUseCase,
    private val saveParameterUseCase: SaveParameterUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _state = MutableStateFlow(GraphState())
    val state: StateFlow<GraphState> = _state.asStateFlow()

    private val _events = Channel<GraphEvent>()
    val events = _events.receiveAsFlow()

    private var analysisJob: Job? = null
    private var animationJob: Job? = null
    private var functionAddedCount = 0

    // 저장된 모든 매개변수 (화면에는 함수에 쓰인 것만 표시)
    private var allParameters: List<Parameter> = emptyList()

    init {
        observeFunctions()
        observeParameters()
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
                        selectedPoint = null // 뷰포트 변경 시 선택 해제
                    )
                }
                triggerAnalysis()
            }
            is GraphAction.OnSelectFunction -> {
                _state.update {
                    it.copy(
                        selectedFunctionId = action.id,
                        selectedPoint = null,
                        keyPoints = if (action.id == null) emptyList() else it.keyPoints
                    )
                }
                triggerAnalysis()
            }
            is GraphAction.OnSelectPoint -> {
                _state.update { it.copy(selectedPoint = action.point) }
            }
            GraphAction.OnDismissPointInfo -> {
                _state.update { it.copy(selectedPoint = null) }
            }
            is GraphAction.OnParameterChange -> {
                // 재생 중인 매개변수를 직접 움직이면 재생을 멈춤
                if (_state.value.animatingParameter == action.name) stopAnimation(save = false)
                updateParameterValue(action.name, action.value)
            }
            is GraphAction.OnParameterChangeFinished -> saveParameter(action.name)
            is GraphAction.OnToggleParameterAnimation -> {
                val wasAnimating = _state.value.animatingParameter == action.name
                stopAnimation(save = true)
                if (!wasAnimating) startAnimation(action.name)
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
                        val functions = result.data
                        _state.update {
                            it.copy(
                                functions = functions,
                                parameters = usedParameters(functions, allParameters),
                                // 선택했던 함수가 삭제되면 선택 해제
                                selectedFunctionId = it.selectedFunctionId?.takeIf { id -> functions.any { f -> f.id == id } }
                            )
                        }
                        triggerAnalysis()
                    }
                    is Result.Error -> sendError(result.error.message())
                }
            }
        }
    }

    /**
     * 저장된 매개변수를 구독합니다.
     * 자동 재생 중인 매개변수는 화면의 값이 더 최신이므로 덮어쓰지 않습니다.
     */
    private fun observeParameters() {
        viewModelScope.launch {
            observeParametersUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        allParameters = result.data
                        _state.update { state ->
                            val stored = usedParameters(state.functions, result.data)
                            state.copy(
                                parameters = stored.map { parameter ->
                                    if (parameter.name == state.animatingParameter) {
                                        state.parameters.find { it.name == parameter.name } ?: parameter
                                    } else {
                                        parameter
                                    }
                                }
                            )
                        }
                        triggerAnalysis()
                    }
                    is Result.Error -> sendError(result.error.message())
                }
            }
        }
    }

    /** 함수들에 실제로 쓰인 매개변수만 이름 순으로 고릅니다. */
    private fun usedParameters(functions: List<GraphFunction>, parameters: List<Parameter>): List<Parameter> {
        val names = functions.flatMap { it.parameterNames }.toSet()
        return parameters.filter { it.name in names }.sortedBy { it.name }
    }

    /** 화면의 매개변수 값만 바꿉니다. (저장은 슬라이더를 놓을 때) */
    private fun updateParameterValue(name: String, value: Double) {
        _state.update { state ->
            state.copy(
                parameters = state.parameters.map {
                    if (it.name == name) it.copy(value = value.coerceIn(it.min, it.max)) else it
                },
                selectedPoint = null
            )
        }
        triggerAnalysis()
    }

    private fun saveParameter(name: String) {
        val parameter = _state.value.parameters.find { it.name == name } ?: return
        viewModelScope.launch {
            when (val result = saveParameterUseCase(parameter)) {
                is Result.Success -> Unit
                is Result.Error -> sendError(result.error.message())
            }
        }
    }

    /**
     * 매개변수를 최소 ~ 최대 사이에서 왕복하며 자동으로 움직입니다. (한 방향 약 4초)
     * 값이 바뀔 때 그래프가 어떻게 변하는지 관찰하는 학습용 기능입니다.
     */
    private fun startAnimation(name: String) {
        _state.update { it.copy(animatingParameter = name) }
        animationJob = viewModelScope.launch {
            var direction = 1
            while (true) {
                val parameter = _state.value.parameters.find { it.name == name } ?: break
                val delta = (parameter.max - parameter.min) / ANIMATION_DURATION_FRAMES
                var next = parameter.value + direction * delta
                if (next >= parameter.max) { next = parameter.max; direction = -1 }
                if (next <= parameter.min) { next = parameter.min; direction = 1 }
                updateParameterValue(name, next)
                delay(ANIMATION_FRAME_MS)
            }
        }
    }

    private fun stopAnimation(save: Boolean) {
        val name = _state.value.animatingParameter ?: return
        animationJob?.cancel()
        animationJob = null
        _state.update { it.copy(animatingParameter = null) }
        if (save) saveParameter(name)
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
     * 함수, 매개변수, 보이는 범위, 선택한 함수가 바뀔 때 교점과 특징점 계산을 요청합니다.
     * 진행 중인 계산이 있으면 취소하고 최신 상태로 다시 계산합니다.
     */
    private fun triggerAnalysis() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            val state = _state.value
            val (intersections, keyPoints) = withContext(defaultDispatcher) {
                calculateIntersections(state) to findKeyPoints(state)
            }
            _state.update { it.copy(intersections = intersections, keyPoints = keyPoints) }
        }
    }

    /** 현재 보이는 범위(양옆 여유 포함) 내에서 보이는 함수들 간의 교차점을 계산합니다. */
    private fun calculateIntersections(state: GraphState): List<Offset> {
        val buffer = (state.visibleEndX - state.visibleStartX) * 0.1
        val intersections = calculateIntersectionsUseCase(
            functions = state.functions,
            rangeStart = state.visibleStartX - buffer,
            rangeEnd = state.visibleEndX + buffer,
            params = state.parameterValues
        )

        return intersections.map { (x, y) ->
            Offset(x.toFloat(), y.toFloat())
        }
    }

    /** 선택한 함수의 특징점(근, y절편, 극대, 극소)을 보이는 범위에서 찾습니다. */
    private fun findKeyPoints(state: GraphState): List<KeyPoint> {
        val function = state.functions.find { it.id == state.selectedFunctionId && it.isVisible }
            ?: return emptyList()
        return findKeyPointsUseCase(function, state.visibleStartX, state.visibleEndX, state.parameterValues)
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
        const val ANIMATION_FRAME_MS = 16L
        const val ANIMATION_DURATION_FRAMES = 240.0 // 최소 -> 최대 한 번 가는 데 걸리는 프레임 수 (약 4초)

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
