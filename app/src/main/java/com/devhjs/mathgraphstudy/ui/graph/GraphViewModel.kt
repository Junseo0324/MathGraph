package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.usecase.MathParser
import com.devhjs.mathgraphstudy.ui.math.*
import com.devhjs.mathgraphstudy.domain.model.math.*
import kotlin.math.pow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.random.Random

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

class GraphViewModel : ViewModel() {
    private val _state = MutableStateFlow(GraphState())
    val state: StateFlow<GraphState> = _state.asStateFlow()

    private val _events = Channel<GraphEvent>()
    val events = _events.receiveAsFlow()

    private var intersectionJob: Job? = null
    private var functionAddedCount = 0
    private val mathParser = MathParser()
    fun onAction(action: GraphAction) {
        when (action) {
            is GraphAction.OnInput -> {
                 _state.update { 
                     val newInputState = com.devhjs.mathgraphstudy.ui.math.MathInputManager.processInput(it.mathInput, action.input)
                     it.copy(mathInput = newInputState)
                 }
            }
            is GraphAction.OnFocusChange -> {
                _state.update { 
                    val newInputState = com.devhjs.mathgraphstudy.ui.math.MathInputManager.onFocusChange(it.mathInput, action.path)
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
        val visibleFunctions = state.functions.filter { it.isVisible }
        if (visibleFunctions.size < 2) return@withContext emptyList()

        // We only calculate intersections for the first pair or all combinations?
        // User said "2개 이상이면 서로 겹치는 부분". All pairs is safer.
        // For simplicity, let's just do pairs.
        
        val intersections = mutableListOf<Offset>()
        // Assume screen width approx 1080px. Center is 540.
        // We extend range slightly to catch intersections near edges
        val buffer = 5.0
        val startX = ((-540f - state.viewportOffsetX) / state.viewportScale) - buffer
        val endX = ((540f - state.viewportOffsetX) / state.viewportScale) + buffer
        
        val rangeStart = startX.toDouble()
        val rangeEnd = endX.toDouble()
        val step = 0.1 

        for (i in visibleFunctions.indices) {
            for (j in i + 1 until visibleFunctions.size) {
                val f1 = visibleFunctions[i]
                val f2 = visibleFunctions[j]
                
                var x = rangeStart
                while (x < rangeEnd) {
                    val y1_a = f1.calculate(x)
                    val y2_a = f2.calculate(x)
                    val diff_a = y1_a - y2_a
                    
                    val nextX = x + step
                    val y1_b = f1.calculate(nextX)
                    val y2_b = f2.calculate(nextX)
                    val diff_b = y1_b - y2_b
                    
                    // Check if signs are different, OR if one of them is effectively zero
                    // Note: diff_a * diff_b <= 0 captures sign change or exact zero.
                    // We need to avoid duplicates if we have consecutive zeros.
                    
                    if (diff_a * diff_b <= 0.0) {
                         // Likely intersection
                         val rootX = bisection(f1, f2, x, nextX)
                         val rootY = f1.calculate(rootX)
                         
                         // Validation: Is it really a root?
                         if (abs(f1.calculate(rootX) - f2.calculate(rootX)) < 1e-3) {
                             // Check if we already added a close point to avoid duplicates
                             val existing = intersections.find { 
                                 abs(it.x - rootX) < 0.2 && abs(it.y - rootY) < 0.2 
                             }
                             if (existing == null) {
                                 intersections.add(Offset(rootX.toFloat(), rootY.toFloat()))
                             }
                         }
                    }
                    x = nextX
                }
            }
        }
        intersections
    }

    private fun bisection(f1: GraphFunction, f2: GraphFunction, a: Double, b: Double, tol: Double = 1e-5): Double {
        var low = a
        var high = b
        var mid = (low + high) / 2.0
        
        repeat(20) { // Max iterations
            val diffLow = f1.calculate(low) - f2.calculate(low)
            val diffMid = f1.calculate(mid) - f2.calculate(mid)
            
            if (abs(diffMid) < tol) return mid
            
            if (diffLow * diffMid < 0) {
                high = mid
            } else {
                low = mid
            }
            mid = (low + high) / 2.0
        }
        return mid
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

    private fun VisualMathNode.toExpressionNode(): MathParser.ExpressionNode {
        return when (this) {
            is NumberNode -> MathParser.ExpressionNode.Constant(this.value.toDoubleOrNull() ?: 0.0)
            is VariableNode -> MathParser.ExpressionNode.Variable(this.name)
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
                MathParser.ExpressionNode.BinaryOp(leftNode, rightNode, opFunc, symbol)
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
                MathParser.ExpressionNode.UnaryOp(argNode, funcOp, symbol)
            }
            is PowerNode -> {
                 val baseNode = this.base.toExpressionNode()
                 val exponentNode = this.exponent.toExpressionNode()
                 MathParser.ExpressionNode.BinaryOp(baseNode, exponentNode, { a, b -> a.pow(b) }, "^")
            }
            PlaceholderNode -> throw IllegalStateException("Placeholder in expression")
        }
    }

    private fun MathParser.ExpressionNode.toVisualNode(): VisualMathNode {
        return when (this) {
            is MathParser.ExpressionNode.Constant -> {
                val v = this.value
                val text = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
                NumberNode(text)
            }
            is MathParser.ExpressionNode.Variable -> VariableNode(this.name)
            is MathParser.ExpressionNode.BinaryOp -> {
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
            is MathParser.ExpressionNode.UnaryOp -> {
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
