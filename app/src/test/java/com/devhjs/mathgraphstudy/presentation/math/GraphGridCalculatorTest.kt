package com.devhjs.mathgraphstudy.presentation.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphGridCalculatorTest {

    @Test
    fun `calculateGridStep returns correct step for standard zoom`() {
        // Given: 줌 스케일이 50 (1단위 = 50px)
        // 100px이 기준이므로 2단위(100px)가 적당함
        val scale = 50f
        
        // When
        val step = GraphGridCalculator.calculateGridStep(scale)
        
        // Then
        assertEquals(2, step)
    }

    @Test
    fun `calculateGridStep returns correct step for zoom in`() {
        // Given: 줌 스케일이 100 (1단위 = 100px)
        // 100px이 기준이므로 1단위가 적당함
        val scale = 100f
        
        // When
        val step = GraphGridCalculator.calculateGridStep(scale)
        
        // Then
        assertEquals(1, step)
    }

    @Test
    fun `calculateGridStep returns correct step for zoom out`() {
        // Given: 줌 스케일이 10 (1단위 = 10px)
        // 100px이 기준이므로 10단위가 되어야 100px 간격이 됨
        val scale = 10f
        
        // When
        val step = GraphGridCalculator.calculateGridStep(scale)
        
        // Then
        assertEquals(10, step)
    }

    @Test
    fun `calculateVisibleGridLines returns correct range`() {
        // Given: -5 ~ 5 범위, step 2
        val minVal = -5f
        val maxVal = 5f
        val step = 2
        
        // When
        val lines = GraphGridCalculator.calculateVisibleGridLines(minVal, maxVal, step)
        
        // Then
        // 예상되는 그리드 라인: -4, -2, 0, 2, 4
        // -5보다 크거나 같은 첫 2의 배수는 -4
        val expected = listOf(-4f, -2f, 0f, 2f, 4f)
        
        assertEquals(expected.size, lines.size)
        // 오차 범위(delta) 0.001f 내에서 값 비교
        expected.zip(lines).forEach { (exp, actual) ->
            assertEquals(exp, actual, 0.001f)
        }
    }
    
    @Test
    fun `calculateVisibleGridLines handles range strictly inside step`() {
        // Given: 0.5 ~ 1.5 범위, step 5 (범위가 step보다 작음)
        // 이 범위 안에 5의 배수는 없음. 라인이 없어야 함? 
        // 0.5 ~ 1.5 사이에는 5의 배수가 없음.
        val minVal = 0.5f
        val maxVal = 1.5f
        val step = 5
        
        // When
        val lines = GraphGridCalculator.calculateVisibleGridLines(minVal, maxVal, step)
        
        // Then
        // 0.5보다 큰 첫 5의 배수는 5임. 5는 1.5보다 큼.
        // 따라서 빈 리스트여야 함
        assertTrue(lines.isEmpty())
    }
    
    @Test
    fun `calculateVisibleGridLines includes boundary values`() {
        // Given: 0 ~ 10 범위, step 5
        val minVal = 0f
        val maxVal = 10f
        val step = 5
        
        // When
        val lines = GraphGridCalculator.calculateVisibleGridLines(minVal, maxVal, step)
        
        // Then
        // 0, 5, 10
        val expected = listOf(0f, 5f, 10f)
        assertEquals(expected.size, lines.size)
        expected.zip(lines).forEach { (exp, actual) ->
            assertEquals(exp, actual, 0.001f)
        }
    }
}
