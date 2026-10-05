package com.devhjs.mathgraphstudy.presentation.graph

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.KeyPoint
import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType
import com.devhjs.mathgraphstudy.domain.service.MathParser
import com.devhjs.mathgraphstudy.domain.usecase.BuildFunctionNodeUseCase
import com.devhjs.mathgraphstudy.domain.usecase.CalculateIntersectionsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.DeleteGraphFunctionUseCase
import com.devhjs.mathgraphstudy.domain.usecase.FindKeyPointsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.ObserveParametersUseCase
import com.devhjs.mathgraphstudy.domain.usecase.RecordFunctionAddedUseCase
import com.devhjs.mathgraphstudy.domain.usecase.SaveParameterUseCase
import com.devhjs.mathgraphstudy.domain.usecase.ObserveGraphFunctionsUseCase
import com.devhjs.mathgraphstudy.domain.usecase.SaveGraphFunctionUseCase
import com.devhjs.mathgraphstudy.fake.FakeGraphFunctionRepository
import com.devhjs.mathgraphstudy.fake.FakeParameterRepository
import com.devhjs.mathgraphstudy.fake.FakeUsageRepository
import com.devhjs.mathgraphstudy.presentation.math.ExpressionEditor
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

    private lateinit var repository: FakeGraphFunctionRepository
    private lateinit var parameterRepository: FakeParameterRepository
    private lateinit var viewModel: GraphViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeGraphFunctionRepository()
        parameterRepository = FakeParameterRepository()
        viewModel = createViewModel()
    }

    private fun createViewModel() = GraphViewModel(
        buildFunctionNodeUseCase = BuildFunctionNodeUseCase(MathParser()),
        observeGraphFunctionsUseCase = ObserveGraphFunctionsUseCase(repository),
        saveGraphFunctionUseCase = SaveGraphFunctionUseCase(repository, parameterRepository),
        deleteGraphFunctionUseCase = DeleteGraphFunctionUseCase(repository),
        calculateIntersectionsUseCase = CalculateIntersectionsUseCase(),
        findKeyPointsUseCase = FindKeyPointsUseCase(),
        observeParametersUseCase = ObserveParametersUseCase(parameterRepository),
        saveParameterUseCase = SaveParameterUseCase(parameterRepository),
        recordFunctionAddedUseCase = RecordFunctionAddedUseCase(FakeUsageRepository()),
        defaultDispatcher = testDispatcher
    )

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
        assertEquals(6.0, state.functions[0].evaluate(3.0), 0.001)
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
        // 저장된 수식이 한 줄 입력기 토큰으로 불러와지고 커서는 끝에 위치
        assertEquals(listOf("x"), editing.editor.tokens)
        assertEquals(1, editing.editor.cursor)
        type("^", "2")
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // Then: 개수는 그대로, 같은 id/색상으로 수식만 교체
        val edited = viewModel.state.value.functions.single()
        assertEquals(original.id, edited.id)
        assertEquals(original.color, edited.color)
        assertEquals(9.0, edited.evaluate(3.0), 0.001)
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
        assertTrue(state.editor.tokens.isEmpty())
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
        assertEquals(2.0, function.evaluate(3.0), 0.001)
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

    @Test
    fun testFunctionsAreRestoredFromStorage() {
        // Given: 함수 하나를 추가
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("x")
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // When: 앱을 다시 켠 것처럼 ViewModel 을 새로 생성
        val restored = createViewModel()

        // Then: 저장소에서 목록을 다시 불러옴
        assertEquals(viewModel.state.value.functions, restored.state.value.functions)
        assertEquals(1, restored.state.value.functions.size)
    }

    @Test
    fun testToggleVisibilityAndDelete() {
        // Given: 함수 하나
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("x")
        viewModel.onAction(GraphAction.OnSubmitFunction)
        val id = viewModel.state.value.functions.single().id

        // When & Then: 숨기기
        viewModel.onAction(GraphAction.OnToggleVisibility(id))
        assertFalse(viewModel.state.value.functions.single().isVisible)

        // When & Then: 삭제
        viewModel.onAction(GraphAction.OnRemoveFunction(id))
        assertTrue(viewModel.state.value.functions.isEmpty())
    }

    @Test
    fun testSaveFailureSendsError() = runTest {
        // Given: 저장소 쓰기 실패
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("x")
        repository.shouldFail = true

        // When: 추가
        viewModel.onAction(GraphAction.OnSubmitFunction)

        // Then: 저장 실패 메시지
        val event = viewModel.events.first()
        assertEquals(GraphEvent.ShowError(DataError.Local.UNKNOWN.toMessage()), event)
    }

    @Test
    fun testParameterSliderChangesFunctionValue() {
        // Given: y = a·x 추가 (a 는 기본값 1 로 자동 생성)
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("a", "x")
        viewModel.onAction(GraphAction.OnSubmitFunction)
        val state = viewModel.state.value
        assertEquals(listOf("a"), state.parameters.map { it.name })
        assertEquals(3.0, state.functions.single().evaluate(3.0, state.parameterValues), 0.001)

        // When: 슬라이더로 a = 2 로 바꾸고 놓음
        viewModel.onAction(GraphAction.OnParameterChange("a", 2.0))
        viewModel.onAction(GraphAction.OnParameterChangeFinished("a"))

        // Then: 함수값이 바뀌고 저장소에도 저장됨
        val changed = viewModel.state.value
        assertEquals(6.0, changed.functions.single().evaluate(3.0, changed.parameterValues), 0.001)
        assertEquals(2.0, parameterRepository.current.single().value, 0.001)
    }

    @Test
    fun testSelectFunctionFindsKeyPoints() {
        // Given: y = x^2 - 1, 보이는 범위 -5 ~ 5
        viewModel.onAction(GraphAction.OnOpenEditor(null))
        type("x", "^", "2", ExpressionEditor.INPUT_RIGHT, "-", "1")
        viewModel.onAction(GraphAction.OnSubmitFunction)
        viewModel.onAction(GraphAction.OnVisibleRangeChange(-5.0, 5.0))
        val id = viewModel.state.value.functions.single().id

        // When: 곡선을 탭해 선택
        viewModel.onAction(GraphAction.OnSelectFunction(id))

        // Then: 근 2개, 극소 1개, y절편 1개
        val types = viewModel.state.value.keyPoints.map { it.type }
        assertEquals(2, types.count { it == KeyPoint.Type.ROOT })
        assertEquals(1, types.count { it == KeyPoint.Type.MINIMUM })
        assertEquals(1, types.count { it == KeyPoint.Type.Y_INTERCEPT })

        // When: 빈 곳 탭 -> 선택 해제
        viewModel.onAction(GraphAction.OnSelectFunction(null))
        assertTrue(viewModel.state.value.keyPoints.isEmpty())
    }

    @Test
    fun testPreviewWhileTyping() {
        // Given: 입력 패널 열기
        viewModel.onAction(GraphAction.OnOpenEditor(null))

        // When: "x^" 까지 입력 (아직 미완성)
        type("x", "^")
        // Then: 미리보기 없음
        assertEquals(null, viewModel.state.value.previewNode)

        // When: "2" 까지 입력해 x^2 완성
        type("2")
        // Then: 미리보기가 그래프 목록에 포함됨
        val state = viewModel.state.value
        assertEquals(PREVIEW_FUNCTION_ID, state.displayedFunctions.single().id)
        assertEquals(9.0, state.displayedFunctions.single().evaluate(3.0), 0.001)

        // When: 닫기 -> 미리보기 사라짐
        viewModel.onAction(GraphAction.OnCloseEditor)
        assertTrue(viewModel.state.value.displayedFunctions.isEmpty())
    }
}
