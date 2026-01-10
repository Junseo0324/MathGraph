package com.devhjs.mathgraphstudy.domain.model.math.enums

/**
 * 수식에서 지원하는 수학 함수들(sin, cos, log 등)을 정의합니다.
 *
 * @property symbol 화면에 표시될 함수의 이름 (예: "sin", "√")
 */
enum class MathFunction(val symbol: String) {
    SQRT("√"), SIN("sin"), COS("cos"), TAN("tan"),
    LOG("log"), LN("ln"), ABS("abs")
}
