package com.devhjs.mathgraphstudy.domain.model.math.enums

enum class BeginnerFunctionType(val displayName: String, val inputLabels: List<String>) {
    LINEAR("일차", listOf("a", "b")),       // y = ax + b
    QUADRATIC("이차", listOf("a", "b", "c")), // y = ax^2 + bx + c
    CUBIC("삼차", listOf("a", "b", "c", "d")), // y = ax^3 + bx^2 + cx + d
    RATIONAL("유리", listOf("a", "b", "c"));   // y = a/(x+b) + c

    /**
     * 사용자가 입력한 계수 값들(a, b, c, d)을 바탕으로
     * 파싱 가능한 수식 문자열(예: "(1)*x + (2)")을 생성합니다.
     * 비어 있는 계수는 최고차항 계수 a는 1, 나머지는 0으로 간주합니다.
     *
     * @return 수식 문자열. 숫자가 아닌 계수가 있으면 null
     */
    fun buildExpression(coefficients: Map<String, String>): String? {
        val values = inputLabels.associateWith { label ->
            val raw = coefficients[label]?.trim().orEmpty()
            when {
                raw.isEmpty() -> if (label == "a") "1" else "0"
                raw.toDoubleOrNull() != null -> raw
                else -> return null
            }
        }
        val a = values["a"]
        val b = values["b"]
        val c = values["c"]
        val d = values["d"]

        return when (this) {
            LINEAR -> "($a)*x + ($b)"
            QUADRATIC -> "($a)*x^2 + ($b)*x + ($c)"
            CUBIC -> "($a)*x^3 + ($b)*x^2 + ($c)*x + ($d)"
            RATIONAL -> "($a)/(x + ($b)) + ($c)"
        }
    }
}
