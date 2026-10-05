package com.devhjs.mathgraphstudy.presentation.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tan

class CurveSamplerTest {

    // 400x400 화면, 원점이 중앙, 1단위 = 40px (x 범위 -5 ~ 5)
    private val width = 400f
    private val height = 400f
    private val scale = 40f

    private fun sampler() = CurveSampler(
        width = width,
        height = height,
        toGraphX = { px -> ((px - width / 2) / scale).toDouble() },
        toScreenY = { y -> (height / 2 - y * scale).toFloat() }
    )

    private fun toGraphX(px: Float) = (px - width / 2) / scale

    @Test
    fun testLineIsSingleSegment() {
        // When: y = x
        val segments = sampler().sample { x -> x }

        // Then: 끊김 없이 하나의 구간
        assertEquals(1, segments.size)
    }

    @Test
    fun testTanBreaksAtAsymptotes() {
        // When: y = tan x (x 범위 -5 ~ 5 에는 ±π/2, ±3π/2 네 개의 점근선)
        val segments = sampler().sample { x -> tan(x) }

        // Then: 점근선마다 끊겨 5개 구간
        assertEquals(5, segments.size)
    }

    @Test
    fun testSteepLineIsNotBroken() {
        // When: 화면을 거의 수직으로 가로지르는 y = 200x
        val segments = sampler().sample { x -> 200 * x }

        // Then: 가파르지만 연속이므로 끊기지 않음
        assertEquals(1, segments.size)
    }

    @Test
    fun testSqrtReachesDomainBoundary() {
        // When: y = √x (x < 0 에서는 정의되지 않음)
        val segments = sampler().sample { x -> sqrt(x) }

        // Then: 한 구간이고, 시작점이 x = 0 에 아주 가까움
        assertEquals(1, segments.size)
        val startX = toGraphX(segments.single().first().x)
        assertTrue("start x = $startX", abs(startX) < 0.01)
    }

    @Test
    fun testUndefinedFunctionDrawsNothing() {
        // When: 어디서도 정의되지 않는 함수
        val segments = sampler().sample { Double.NaN }

        // Then
        assertTrue(segments.isEmpty())
    }
}
