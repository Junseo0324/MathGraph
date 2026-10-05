package com.devhjs.mathgraphstudy.presentation.graph

import com.devhjs.mathgraphstudy.domain.model.math.PlaceholderNode
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.domain.service.MathParser
import com.devhjs.mathgraphstudy.domain.usecase.CalculateIntersectionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModelTest {

    private lateinit var viewModel: GraphViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = GraphViewModel(MathParser(), CalculateIntersectionsUseCase())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun type(vararg inputs: String) {
        inputs.forEach { viewModel.onAction(GraphAction.OnInput(it)) }
    }

    @Test
    fun testAddFunctionClosesEditor() {
        // Given: 새 함수 입력 패널을 열고 2x 입력
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("2", "x")

        // When: 추가
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // Then: 함수가 추가되고 입력 패널은 닫힘
        val state = viewModel.state.value
        assertEquals(1, state.functions.size)
        assertEquals(6.0, state.functions[0].calculate(3.0), 0.001)
        assertFalse(state.isEditorOpen)
    }

    @Test
    fun testEditReplacesFunctionAndKeepsColor() {
        // Given: x 함수를 하나 추가
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("x")
        viewModel.onAction(GraphAction.OnSubmitFunction)
        val original = viewModel.state.value.functions.single()

        // When: 편집으로 열어 x -> x^2 로 수정 후 저장
        viewModel.onAction(GraphAction.OnOpenEditor(original.id))
        val editing = viewModel.state.value
        assertTrue(editing.isEditorOpen)
        assertEquals(original.id, editing.editingFunctionId)
        assertEquals(original.visualNode, editing.mathInput.rootNode)
        type("^", "2")
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // Then: 개수는 그대로, 같은 id/색상으로 수식만 교체
        val edited = viewModel.state.value.functions.single()
        assertEquals(original.id, edited.id)
        assertEquals(original.color, edited.color)
        assertEquals(9.0, edited.calculate(3.0), 0.001)
        assertNull(viewModel.state.value.editingFunctionId)
    }

    @Test
    fun testCloseEditorDiscardsInput() {
        // Given: 입력 중
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("5")

        // When: 닫기
        viewModel.onAction(GraphAction.OnCloseEditor)

        // Then: 함수는 추가되지 않고 입력은 초기화
        val state = viewModel.state.value
        assertTrue(state.functions.isEmpty())
        assertFalse(state.isEditorOpen)
        assertEquals(PlaceholderNode, state.mathInput.rootNode)
    }

    @Test
    fun testTemplateRationalFunction() {
        // Given: 템플릿 모드에서 유리함수 a=1, b=-2, c=1
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        viewModel.onAction(GraphAction.OnToggleMode)
        viewModel.onAction(GraphAction.OnBeginnerTypeChanged(BeginnerFunctionType.RATIONAL))
        viewModel.onAction(GraphAction.OnCoefficientChanged("a", "1"))
        viewModel.onAction(GraphAction.OnCoefficientChanged("b", "-2"))
        viewModel.onAction(GraphAction.OnCoefficientChanged("c", "1"))

        // When: 추가
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // Then: y = 1/(x-2) + 1
        val function = viewModel.state.value.functions.single()
        assertEquals(2.0, function.calculate(3.0), 0.001)
        assertEquals("1/x-2+1", function.expression)
    }

    @Test
    fun testIncompleteExpressionSendsError() = runTest {
        // Given: sin(?) 처럼 빈 칸이 남은 수식
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("sin")

        // When: 추가 시도
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // Then: 오류 이벤트가 오고, 입력 패널은 열린 채 유지
        val event = viewModel.events.first()
        assertTrue(event is GraphEvent.ShowError)
        assertTrue(viewModel.state.value.isEditorOpen)
        assertTrue(viewModel.state.value.functions.isEmpty())
    }
}
