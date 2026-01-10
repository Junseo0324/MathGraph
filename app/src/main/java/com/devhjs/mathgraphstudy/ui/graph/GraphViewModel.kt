package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.math.BinaryOpNode
import com.devhjs.mathgraphstudy.domain.model.math.ExpressionNode
import com.devhjs.mathgraphstudy.domain.model.math.FunctionNode
import com.devhjs.mathgraphstudy.domain.model.math.NumberNode
import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.PowerNode
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.domain.model.math.VisualMathNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathFunction
import com.devhjs.mathgraphstudy.domain.model.math.enums.MathOperator
import com.devhjs.mathgraphstudy.domain.model.math.toDisplayString
import com.devhjs.mathgraphstudy.domain.service.MathParser
import com.devhjs.mathgraphstudy.domain.usecase.CalculateIntersectionsUseCase
import com.devhjs.mathgraphstudy.ui.math.MathInputManager
import com.devhjs.mathgraphstudy.ui.math.MathInputState
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
import kotlin.random.Random

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
                _state.update { it.copy(isBeginnerMode = !it.isBeginnerMode) }
            }
            is GraphAction.OnBeginnerTypeChanged -> {
                _state.update { it.copy(
                    beginnerFunctionType = action.type,
                    beginnerCoefficients = emptyMap() // Reset coefficients on type change
                ) }
            }
            is GraphAction.OnCoefficientChanged -> {
                _state.update { 
                    val newCoefficients = it.beginnerCoefficients.toMutableMap()
                    newCoefficients[action.key] = action.value
                    it.copy(beginnerCoefficients = newCoefficients)
                }
            }
            GraphAction.OnAddFunction -> {
                val currentState = _state.value
                val parsed: (Double) -> Double
                val exprDisplay: String

                var visualNode: VisualMathNode? = null

                if (currentState.isBeginnerMode) {
                    exprDisplay = constructBeginnerExpression(currentState)
                    try {
                        val exprNode = mathParser.parseToNode(exprDisplay)
                        parsed = mathParser.evaluate(exprNode)
                        // Convert domain AST -> Visual AST for nice display
                        visualNode = exprNode.toVisualNode()
                    } catch (e: Exception) {
                        return
                    }
                } else {
                    // AST Mode
                    val root = currentState.mathInput.rootNode
                    try {
                        val exprNode = root.toExpressionNode()
                        parsed = mathParser.evaluate(exprNode)
                        exprDisplay = root.toDisplayString() 
                        visualNode = root
                    } catch (e: IllegalStateException) {
                        return 
                    } catch (e: Exception) {
                        return
                    }
                }
                
                if (exprDisplay.isBlank()) return

                val newFunction = GraphFunction(
                    id = System.currentTimeMillis().toString(),
                    expression = exprDisplay,
                    visualNode = visualNode,
                    color = generateRandomColor(),
                    isVisible = true,
                    calculate = parsed
                )

                _state.update { state: GraphState ->
                     state.copy(
                        functions = state.functions + newFunction,
                        mathInput = if (state.isBeginnerMode) state.mathInput else MathInputState(),
                        beginnerCoefficients = if (state.isBeginnerMode) emptyMap() else state.beginnerCoefficients
                    )
                }

                functionAddedCount++
                if (functionAddedCount % 5 == 0) {
                    viewModelScope.launch {
                        _events.send(GraphEvent.ShowInterstitialAd)
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
                        viewportOffsetY = action.offsetY
                    )
                }
                triggerIntersectionCalculation()
            }
        }
    }

    private fun constructBeginnerExpression(state: GraphState): String {
        val coeffs = state.beginnerCoefficients
        val a = coeffs["a"] ?: "1"
        val b = coeffs["b"] ?: "0"
        val c = coeffs["c"] ?: "0"
        val d = coeffs["d"] ?: "0"

        // Helper to handle 1 and 0 logic if needed, but for now explicitly using values is safer
        // We will assume "a", "b", "c" are numbers. 
        // If empty, defaults are provided.
        // We wrap coefficients in parentheses to be safe with negative numbers? e.g. -1*x vs (-1)*x
        // Actually, let's keep it simple.

        return when (state.beginnerFunctionType) {
            BeginnerFunctionType.LINEAR -> "($a)*x + ($b)"
            BeginnerFunctionType.QUADRATIC -> "($a)*x^2 + ($b)*x + ($c)"
            BeginnerFunctionType.CUBIC -> "($a)*x^3 + ($b)*x^2 + ($c)*x + ($d)"
            BeginnerFunctionType.RATIONAL -> "($a)/($b) * x + ($c)"
        }
    }

    private fun triggerIntersectionCalculation() {
        intersectionJob?.cancel()
        intersectionJob = viewModelScope.launch {
            // Optional: Debounce for smoother viewport updates
            delay(50) 
            val currentState = _state.value
            val intersections = calculateIntersections(currentState)
            _state.update { it.copy(intersections = intersections) }
        }
    }



    private suspend fun calculateIntersections(state: GraphState): List<Offset> = withContext(Dispatchers.Default) {
        // Assume screen width approx 1080px. Center is 540.
        // We extend range slightly to catch intersections near edges
        val buffer = 5.0
        val startX = ((-540f - state.viewportOffsetX) / state.viewportScale) - buffer
        val endX = ((540f - state.viewportOffsetX) / state.viewportScale) + buffer

        val intersections = calculateIntersectionsUseCase(
            functions = state.functions,
            rangeStart = startX.toDouble(),
            rangeEnd = endX.toDouble()
        )

        intersections.map { (x, y) ->
            Offset(x.toFloat(), y.toFloat())
        }
    }
    
    // ... generateRandomColor ...

    private fun generateRandomColor(): Color {
        return Color(
            red = Random.nextInt(256),
            green = Random.nextInt(256),
            blue = Random.nextInt(256),
            alpha = 255
        )
    }

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
            PlaceholderNode -> throw IllegalStateException("Placeholder in expression")
        }
    }

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

                // Simplification Logic
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

                    // 1. Addition with 0: x + 0 -> x
                    if (op == MathOperator.PLUS && isRightZero) return leftViz
                    if (op == MathOperator.PLUS && isLeftZero) return rightViz

                    // 2. Subtraction with 0: x - 0 -> x
                    if (op == MathOperator.MINUS && isRightZero) return leftViz

                    // 3. Multiplication by 1: 1 * x -> x
                    if (op == MathOperator.MULTIPLY && isLeftOne) return rightViz
                    if (op == MathOperator.MULTIPLY && leftViz is NumberNode && (leftViz.value == "1" || leftViz.value == "1.0")) return rightViz

                    // 4. Multiplication by 0: 0 * x -> 0 (Be careful with formatting, usually 0)
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
                val func = when (this.symbol) {
                    "sqrt" -> MathFunction.SQRT
                    "sin" -> MathFunction.SIN
                    "cos" -> MathFunction.COS
                    "tan" -> MathFunction.TAN
                    "log" -> MathFunction.LOG
                    "ln" -> MathFunction.LN
                    "abs" -> MathFunction.ABS
                    else -> MathFunction.SIN // Fallback
                }
                FunctionNode(
                    func = func,
                    arg = this.operand.toVisualNode()
                )
            }
        }
    }
}
