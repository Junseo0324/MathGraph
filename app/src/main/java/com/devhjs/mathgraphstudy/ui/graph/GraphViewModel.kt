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
import kotlin.random.Random

class GraphViewModel : ViewModel() {

    private val _state = MutableStateFlow(GraphState())
    val state: StateFlow<GraphState> = _state.asStateFlow()

    private val _events = Channel<GraphEvent>()
    val events = _events.receiveAsFlow()

    private val mathParser = MathParser()

    fun onAction(action: GraphAction) {
        when (action) {
            is GraphAction.OnExpressionChanged -> {
                _state.update { it.copy(inputExpression = action.expression) }
            }
            GraphAction.OnAddFunction -> {
                val expr = _state.value.inputExpression
                if (expr.isBlank()) return
                
                // Validate expression
                val parsed = mathParser.parse(expr)
                if (parsed(0.0).isNaN() && !expr.contains("x")) { // Simple check, might need better validation
                     // Allow it for now, user will see nothing if invalid.
                     // Or we can check if parsable.
                }

                val newFunction = GraphFunction(
                    id = System.currentTimeMillis().toString(),
                    expression = expr,
                    color = generateRandomColor(),
                    isVisible = true,
                    calculate = parsed
                )

                _state.update {
                    it.copy(
                        functions = it.functions + newFunction,
                        inputExpression = "" // Clear input
                    )
                }
            }
            is GraphAction.OnRemoveFunction -> {
                _state.update {
                    it.copy(functions = it.functions.filter { f -> f.id != action.id })
                }
            }
            is GraphAction.OnToggleVisibility -> {
                _state.update {
                    it.copy(functions = it.functions.map { f ->
                        if (f.id == action.id) f.copy(isVisible = !f.isVisible) else f
                    })
                }
            }
            is GraphAction.OnViewportChange -> {
                _state.update {
                    it.copy(
                        viewportScale = action.scale,
                        viewportOffsetX = action.offsetX,
                        viewportOffsetY = action.offsetY
                    )
                }
            }
        }
    }

    private fun generateRandomColor(): Color {
        return Color(
            red = Random.nextInt(256),
            green = Random.nextInt(256),
            blue = Random.nextInt(256),
            alpha = 255
        )
    }
}
