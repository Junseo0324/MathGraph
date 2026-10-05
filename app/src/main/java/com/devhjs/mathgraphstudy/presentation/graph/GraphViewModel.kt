package com.devhjs.mathgraphstudy.presentation.graph

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.ExpressionNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NegateNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.ParenNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import com.devhjs.mathgraphstudy.domain.model.math.toDisplayString
import com.devhjs.mathgraphstudy.domain.service.MathParser
import com.devhjs.mathgraphstudy.domain.usecase.CalculateIntersectionsUseCase
import com.devhjs.mathgraphstudy.presentation.math.MathInputManager
import com.devhjs.mathgraphstudy.presentation.math.MathInputState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val mathParser: MathParser,
    private val calculateIntersectionsUseCase: CalculateIntersectionsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GraphState())
    val state: StateFlow<GraphState> = _state.asStateFlow()

    private val _events = Channel<GraphEvent>()
    val events = _events.receiveAsFlow()

    private var intersectionJob: Job? = null
    private var functionAddedCount = 0

    /**
     * 사용자의 UI 액션을 처리하고, 그에 따라 상태(State)를 업데이트합니다.
     * 입력 처리, 모드 전환, 함수 추가/삭제, 뷰포트 변경 등을 담당합니다.
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
                val targetNode = target?.visualNode
                _state.update {
                    it.copy(
                        isEditorOpen = true,
                        editingFunctionId = target?.id,
                        // 기존 함수는 직접 입력 모드에서 수식을 그대로 불러와 편집
                        isTemplateMode = if (target != null) false else it.isTemplateMode,
                        mathInput = if (targetNode != null) {
                            MathInputState(rootNode = targetNode, focusPath = MathInputManager.lastLeafPath(targetNode))
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
            GraphAction.OnSubmitFunction -> {
                val currentState = _state.value
                val parsed: (Double) -> Double
                val exprDisplay: String

                var visualNode: VisualMathNode? = null

                if (currentState.isTemplateMode) {
                    val expression = currentState.beginnerFunctionType
                        .buildExpression(currentState.beginnerCoefficients)
                    if (expression == null) {
                        sendError("계수는 숫자로 입력해주세요.")
                        return
                    }
                    try {
                        val exprNode = mathParser.parseToNode(expression)
                        parsed = mathParser.evaluate(exprNode)

                        visualNode = exprNode.toVisualNode()
                        exprDisplay = visualNode.toDisplayString()
                    } catch (e: Exception) {
                        sendError("수식을 만들 수 없습니다. 계수를 확인해주세요.")
                        return
                    }
                } else {

                    val root = currentState.mathInput.rootNode
                    try {
                        val exprNode = root.toExpressionNode()
                        parsed = mathParser.evaluate(exprNode)
                        exprDisplay = root.toDisplayString() 
                        visualNode = root
                    } catch (e: IllegalStateException) {
                        sendError("비어 있는 칸을 모두 채워주세요.")
                        return 
                    } catch (e: Exception) {
                        sendError("올바르지 않은 수식입니다.")
                        return
                    }
                }
                
                if (exprDisplay.isBlank()) return

                val editingFunction = currentState.functions.find { it.id == currentState.editingFunctionId }
                val newFunction = editingFunction?.copy(
                    // 편집: id, 색상, 표시 여부는 유지하고 수식만 교체
                    expression = exprDisplay,
                    visualNode = visualNode,
                    calculate = parsed
                ) ?: GraphFunction(
                    id = System.currentTimeMillis().toString(),
                    expression = exprDisplay,
                    visualNode = visualNode,
                    color = nextFunctionColor(currentState.functions),
                    isVisible = true,
                    calculate = parsed
                )

                _state.update { state: GraphState ->
                     state.copy(
                        functions = if (editingFunction != null) {
                            state.functions.map { f -> if (f.id == editingFunction.id) newFunction else f }
                        } else {
                            state.functions + newFunction
                        },
                        isEditorOpen = false,
                        editingFunctionId = null,
                        mathInput = MathInputState(),
                        beginnerCoefficients = emptyMap()
                    )
                }

                if (editingFunction == null) {
                    functionAddedCount++
                    if (functionAddedCount % 5 == 0) {
                        viewModelScope.launch {
                            _events.send(GraphEvent.ShowInterstitialAd)
                        }
                    }
                }

                triggerIntersectionCalculation()
            }
            is GraphAction.OnRemoveFunction -> {
                _state.update { state: GraphState ->
                    state.copy(functions = state.functions.filter { f -> f.id != action.id })
                }
                triggerIntersectionCalculation()
            }
            is GraphAction.OnToggleVisibility -> {
                _state.update { state: GraphState ->
                     state.copy(functions = state.functions.map { f ->
                        if (f.id == action.id) f.copy(isVisible = !f.isVisible) else f
                    })
                }
                triggerIntersectionCalculation()
            }
            is GraphAction.OnViewportChange -> {
                _state.update { state: GraphState ->
                    state.copy(
                        viewportScale = action.scale,
                        viewportOffsetX = action.offsetX,
                        viewportOffsetY = action.offsetY,
                        selectedIntersection = null // 뷰포트 변경 시 선택 해제
                    )
                }
                triggerIntersectionCalculation()
            }
            is GraphAction.OnCanvasSizeChanged -> {
                if (_state.value.canvasWidth == action.width) return
                _state.update { it.copy(canvasWidth = action.width) }
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
     * 사용자에게 보여줄 오류 메시지를 일회성 이벤트로 전달합니다.
     */
    private fun sendError(message: String) {
        viewModelScope.launch {
            _events.send(GraphEvent.ShowError(message))
        }
    }

    /**
     * 그래프 함수나 뷰포트가 변경될 때 교차점 계산을 요청합니다.
     * 연속적인 변경(예: 드래그)에 대응하기 위해 약간의 지연(debounce)을 둡니다.
     */
    private fun triggerIntersectionCalculation() {
        intersectionJob?.cancel()
        intersectionJob = viewModelScope.launch {
            delay(50)
            val currentState = _state.value
            val intersections = calculateIntersections(currentState)
            _state.update { it.copy(intersections = intersections) }
        }
    }



    /**
     * 현재 보이는 뷰포트 범위 내에서 활성화된 함수들 간의 교차점을
     * 백그라운드 스레드에서 비동기로 계산합니다.
     */
    private suspend fun calculateIntersections(state: GraphState): List<Offset> = withContext(Dispatchers.Default) {
        val buffer = 5.0
        val halfWidth = state.canvasWidth / 2
        val startX = ((-halfWidth - state.viewportOffsetX) / state.viewportScale) - buffer
        val endX = ((halfWidth - state.viewportOffsetX) / state.viewportScale) + buffer

        val intersections = calculateIntersectionsUseCase(
            functions = state.functions,
            rangeStart = startX,
            rangeEnd = endX
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

    /**
     * UI 표현을 위한 노드 트리(VisualMathNode)를
     * 실제 수학 계산을 위한 도메인 노드 트리(ExpressionNode)로 변환합니다.
     */
    private fun VisualMathNode.toExpressionNode(): ExpressionNode {
        return when (this) {
            is NumberNode -> ExpressionNode.Constant(this.value.toDoubleOrNull() ?: 0.0)
            is VariableNode -> ExpressionNode.Variable(this.name)
            is BinaryOpNode -> {
                val leftNode = this.left.toExpressionNode()
                val rightNode = this.right.toExpressionNode()
                val (opFunc, symbol) = when (this.op) {
                    MathOperator.PLUS -> ({ a: Double, b: Double -> a + b } to "+")
                    MathOperator.MINUS -> ({ a: Double, b: Double -> a - b } to "-")
                    MathOperator.MULTIPLY -> ({ a: Double, b: Double -> a * b } to "*")
                    MathOperator.DIVIDE -> ({ a: Double, b: Double -> a / b } to "/")
                    MathOperator.POWER -> ({ a: Double, b: Double -> a.pow(b) } to "^")
                }
                ExpressionNode.BinaryOp(leftNode, rightNode, opFunc, symbol)
            }
            is FunctionNode -> {
                val argNode = this.arg.toExpressionNode()
                val (funcOp, symbol) = when (this.func) {
                    MathFunction.SQRT -> ({ x: Double -> kotlin.math.sqrt(x) } to "sqrt")
                    MathFunction.SIN -> ({ x: Double -> kotlin.math.sin(x) } to "sin")
                    MathFunction.COS -> ({ x: Double -> kotlin.math.cos(x) } to "cos")
                    MathFunction.TAN -> ({ x: Double -> kotlin.math.tan(x) } to "tan")
                    MathFunction.LOG -> ({ x: Double -> kotlin.math.log10(x) } to "log")
                    MathFunction.LN -> ({ x: Double -> kotlin.math.ln(x) } to "ln")
                    MathFunction.ABS -> ({ x: Double -> kotlin.math.abs(x) } to "abs")
                }
                ExpressionNode.UnaryOp(argNode, funcOp, symbol)
            }
            is PowerNode -> {
                 val baseNode = this.base.toExpressionNode()
                 val exponentNode = this.exponent.toExpressionNode()
                 ExpressionNode.BinaryOp(baseNode, exponentNode, { a, b -> a.pow(b) }, "^")
            }
            is ParenNode -> this.inner.toExpressionNode()
            is NegateNode -> ExpressionNode.UnaryOp(this.operand.toExpressionNode(), { x: Double -> -x }, MathParser.NEGATE)
            PlaceholderNode -> throw IllegalStateException("Placeholder in expression")
        }
    }

    /**
     * 계산용 도메인 노드 트리(ExpressionNode)를 UI 표현용 노드 트리(VisualMathNode)로 역변환합니다.
     * 이 과정에서 0 더하기, 1 곱하기 등의 기본적인 식 간소화 로직이 적용됩니다.
     */
    private fun ExpressionNode.toVisualNode(): VisualMathNode {
        return when (this) {
            is ExpressionNode.Constant -> {
                val v = this.value
                val text = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
                NumberNode(text)
            }
            is ExpressionNode.Variable -> VariableNode(this.name)
            is ExpressionNode.BinaryOp -> {
                val leftViz = this.left.toVisualNode()
                val rightViz = this.right.toVisualNode()

                val isLeftZero = leftViz is NumberNode && (leftViz.value == "0" || leftViz.value == "0.0")
                val isLeftOne = leftViz is NumberNode && (leftViz.value == "1" || leftViz.value == "1.0")
                val isRightZero = rightViz is NumberNode && (rightViz.value == "0" || rightViz.value == "0.0")

                if (this.symbol == "^") {
                    // x^1 -> x
                    val isRightOne = rightViz is NumberNode && (rightViz.value == "1" || rightViz.value == "1.0")
                    if (isRightOne) return leftViz
                    PowerNode(base = leftViz, exponent = rightViz)
                } else {
                    val op = when (this.symbol) {
                        "+" -> MathOperator.PLUS
                        "-" -> MathOperator.MINUS
                        "*" -> MathOperator.MULTIPLY
                        "/" -> MathOperator.DIVIDE
                        else -> MathOperator.PLUS
                    }

                    if (op == MathOperator.PLUS && isRightZero) return leftViz
                    if (op == MathOperator.PLUS && isLeftZero) return rightViz

                    // x + (-3) -> x - 3
                    if (op == MathOperator.PLUS) {
                        rightViz.withoutLeadingNegation()?.let {
                            return BinaryOpNode(left = leftViz, op = MathOperator.MINUS, right = it)
                        }
                    }

                    if (op == MathOperator.MINUS && isRightZero) return leftViz

                    if (op == MathOperator.MULTIPLY && isLeftOne) return rightViz
                    if (op == MathOperator.MULTIPLY && leftViz is NumberNode && (leftViz.value == "1" || leftViz.value == "1.0")) return rightViz

                    if (op == MathOperator.MULTIPLY && isLeftZero) return NumberNode("0")
                    if (op == MathOperator.MULTIPLY && isRightZero) return NumberNode("0")

                    BinaryOpNode(
                        left = leftViz,
                        op = op,
                        right = rightViz
                    )
                }
            }
            is ExpressionNode.UnaryOp -> {
                if (this.symbol == MathParser.NEGATE) {
                    return NegateNode(this.operand.toVisualNode())
                }
                val func = when (this.symbol) {
                    "sqrt" -> MathFunction.SQRT
                    "sin" -> MathFunction.SIN
                    "cos" -> MathFunction.COS
                    "tan" -> MathFunction.TAN
                    "log" -> MathFunction.LOG
                    "ln" -> MathFunction.LN
                    "abs" -> MathFunction.ABS
                    else -> MathFunction.SIN
                }
                FunctionNode(
                    func = func,
                    arg = this.operand.toVisualNode()
                )
            }
        }
    }

    /**
     * 음수 부호로 시작하는 노드라면 부호를 뗀 노드를 반환합니다. (예: -3 -> 3, (-3)x -> 3x)
     * 부호로 시작하지 않으면 null을 반환합니다.
     */
    private fun VisualMathNode.withoutLeadingNegation(): VisualMathNode? {
        return when {
            this is NegateNode -> operand
            this is BinaryOpNode && (op == MathOperator.MULTIPLY || op == MathOperator.DIVIDE) ->
                left.withoutLeadingNegation()?.let { copy(left = it) }
            else -> null
        }
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
