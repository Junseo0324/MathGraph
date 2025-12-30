package com.devhjs.mathgraphstudy.ui.graph

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.usecase.MathParser
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
            is GraphAction.OnExpressionChanged -> {
                _state.update { state: GraphState -> state.copy(inputExpression = action.expression) }
            }
            is GraphAction.OnInsertSymbol -> {
                _state.update { state ->
                    val currentTextFieldValue = state.inputExpression
                    val currentText = currentTextFieldValue.text
                    val selection = currentTextFieldValue.selection

                    val newText = StringBuilder(currentText)
                        .insert(selection.start, action.symbol)
                        .toString()

                    val newCursorPosition = selection.start + action.moveCursor
                    
                    state.copy(
                        inputExpression = androidx.compose.ui.text.input.TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newCursorPosition)
                        )
                    )
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
                val expr = if (currentState.isBeginnerMode) {
                    constructBeginnerExpression(currentState)
                } else {
                    currentState.inputExpression.text
                }

                if (expr.isBlank()) return
                
                // Validate expression
                val parsed = mathParser.parse(expr)
                try {
                    if (parsed(0.0).isNaN() && !expr.contains("x") && !currentState.isBeginnerMode) { 
                         // Simple validation check (weak)
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors for now or handle gracefully
                    return 
                }

                val newFunction = GraphFunction(
                    id = System.currentTimeMillis().toString(),
                    expression = expr,
                    color = generateRandomColor(),
                    isVisible = true,
                    calculate = parsed
                )

                _state.update { state: GraphState ->
                     state.copy(
                        functions = state.functions + newFunction,
                        inputExpression = if (state.isBeginnerMode) state.inputExpression else androidx.compose.ui.text.input.TextFieldValue(""),
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

        // Helper to handle 1 and 0 logic if needed, but for now explicitly using values is safer
        // We will assume "a", "b", "c" are numbers. 
        // If empty, defaults are provided.
        // We wrap coefficients in parentheses to be safe with negative numbers? e.g. -1*x vs (-1)*x
        // Actually, let's keep it simple.

        return when (state.beginnerFunctionType) {
            BeginnerFunctionType.LINEAR -> "($a)*x + ($b)"
            BeginnerFunctionType.QUADRATIC -> "($a)*x^2 + ($b)*x + ($c)"
            BeginnerFunctionType.IRRATIONAL -> "($a)*sqrt(x + ($b)) + ($c)"
            BeginnerFunctionType.RATIONAL -> "($a)/(x + ($b)) + ($c)"
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
}
