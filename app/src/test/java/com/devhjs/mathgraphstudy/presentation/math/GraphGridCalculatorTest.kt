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
        assertEquals(2.0, step, 1e-9)
    }

    @Test
    fun `calculateGridStep returns correct step for zoom in`() {
        // Given: 줌 스케일이 100 (1단위 = 100px)
        // 100px이 기준이므로 1단위가 적당함
        val scale = 100f
        
        // When
        val step = GraphGridCalculator.calculateGridStep(scale)
        
        // Then
        assertEquals(1.0, step, 1e-9)
    }

    @Test
    fun `calculateGridStep returns correct step for zoom out`() {
        // Given: 줌 스케일이 10 (1단위 = 10px)
        // 100px이 기준이므로 10단위가 되어야 100px 간격이 됨
        val scale = 10f
        
        // When
        val step = GraphGridCalculator.calculateGridStep(scale)
        
        // Then
        assertEquals(10.0, step, 1e-9)
    }

    @Test
    fun `calculateVisibleGridLines returns correct range`() {
        // Given: -5 ~ 5 범위, step 2
        val minVal = -5.0
        val maxVal = 5.0
        val step = 2.0
        
        // When
        val lines = GraphGridCalculator.calculateVisibleGridLines(minVal, maxVal, step)
        
        // Then
        // 예상되는 그리드 라인: -4, -2, 0, 2, 4
        // -5보다 크거나 같은 첫 2의 배수는 -4
        val expected = listOf(-4.0, -2.0, 0.0, 2.0, 4.0)
        
        assertEquals(expected.size, lines.size)
        // 오차 범위(delta) 0.001f 내에서 값 비교
        expected.zip(lines).forEach { (exp, actual) ->
            assertEquals(exp, actual, 0.001)
        }
    }
    
    @Test
    fun `calculateVisibleGridLines handles range strictly inside step`() {
        // Given: 0.5 ~ 1.5 범위, step 5 (범위가 step보다 작음)
        // 이 범위 안에 5의 배수는 없음. 라인이 없어야 함? 
        // 0.5 ~ 1.5 사이에는 5의 배수가 없음.
        val minVal = 0.5
        val maxVal = 1.5
        val step = 5.0
        
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
        val minVal = 0.0
        val maxVal = 10.0
        val step = 5.0
        
        // When
        val lines = GraphGridCalculator.calculateVisibleGridLines(minVal, maxVal, step)
        
        // Then
        // 0, 5, 10
        val expected = listOf(0.0, 5.0, 10.0)
        assertEquals(expected.size, lines.size)
        expected.zip(lines).forEach { (exp, actual) ->
            assertEquals(exp, actual, 0.001)
        }
    }

    @Test
    fun `calculateGridStep returns fractional step when zoomed in a lot`() {
        // Given: 1단위 = 1000px -> 0.1단위가 100px
        // When & Then
        assertEquals(0.1, GraphGridCalculator.calculateGridStep(1000f), 1e-9)
        // 1단위 = 300px -> 0.5단위(150px) 가 100px 이상인 가장 작은 값
        assertEquals(0.5, GraphGridCalculator.calculateGridStep(300f), 1e-9)
    }

    @Test
    fun `calculateVisibleGridLines has no accumulated error for fractional step`() {
        // Given: step 0.1, 0 ~ 1
        val lines = GraphGridCalculator.calculateVisibleGridLines(0.0, 1.0, 0.1)

        // Then: 0.0 ~ 1.0 까지 11개, 마지막 값도 정확히 1.0 근처
        assertEquals(11, lines.size)
        assertEquals(1.0, lines.last(), 1e-9)
    }

    @Test
    fun `formatLabel shows decimals matching step`() {
        assertEquals("4", GraphGridCalculator.formatLabel(4.0, 2.0))
        assertEquals("1.5", GraphGridCalculator.formatLabel(1.5, 0.5))
        assertEquals("0.03", GraphGridCalculator.formatLabel(0.03, 0.01))
        assertEquals("-0.2", GraphGridCalculator.formatLabel(-0.2, 0.1))
        assertEquals("0", GraphGridCalculator.formatLabel(-1e-12, 0.1))
    }
}
