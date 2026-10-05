package com.devhjs.mathgraphstudy.domain.usecase

import com.devhjs.mathgraphstudy.domain.error.DataError
import com.devhjs.mathgraphstudy.domain.model.GraphFunction
import com.devhjs.mathgraphstudy.domain.model.Result
import com.devhjs.mathgraphstudy.domain.model.math.VariableNode
import com.devhjs.mathgraphstudy.fake.FakeGraphFunctionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 저장소를 사용하는 UseCase(관찰/저장/삭제)의 성공·실패 경로 테스트 */
class GraphFunctionStorageUseCaseTest {

    private val repository = FakeGraphFunctionRepository()
    private val observe = ObserveGraphFunctionsUseCase(repository)
    private val save = SaveGraphFunctionUseCase(repository)
    private val delete = DeleteGraphFunctionUseCase(repository)

    private val function = GraphFunction(id = "1", node = VariableNode("x"), color = 0xFF42A5F5)

    @Test
    fun testSaveThenObserve() = runTest {
        // When: 저장
        val result = save(function)

        // Then: 성공하고 목록에 나타남
        assertEquals(Result.Success(Unit), result)
        assertEquals(Result.Success(listOf(function)), observe().first())
    }

    @Test
    fun testSaveWithSameIdReplaces() = runTest {
        // Given: 저장된 함수
        save(function)

        // When: 같은 id 로 숨김 처리해 다시 저장
        save(function.copy(isVisible = false))

        // Then: 하나만 남고 교체됨
        val functions = (observe().first() as Result.Success).data
        assertEquals(listOf(function.copy(isVisible = false)), functions)
    }

    @Test
    fun testDelete() = runTest {
        // Given: 저장된 함수
        save(function)

        // When: 삭제
        val result = delete(function.id)

        // Then
        assertEquals(Result.Success(Unit), result)
        assertEquals(Result.Success(emptyList<GraphFunction>()), observe().first())
    }

    @Test
    fun testFailuresBecomeLocalError() = runTest {
        // Given: 저장소 오류
        repository.shouldFail = true

        // When & Then: 예외 대신 DataError.Local.UNKNOWN
        assertEquals(Result.Error(DataError.Local.UNKNOWN), save(function))
        assertEquals(Result.Error(DataError.Local.UNKNOWN), delete(function.id))
        assertEquals(Result.Error(DataError.Local.UNKNOWN), observe().first())
    }
}
