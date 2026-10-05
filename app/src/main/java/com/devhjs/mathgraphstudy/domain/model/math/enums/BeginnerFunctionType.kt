package com.devhjs.mathgraphstudy.domain.model.math.enums

/**
 * 템플릿 함수 종류. 계수만 입력하면 수식을 만들어 줍니다.
 *
 * @property inputLabels 입력받는 계수 이름
 * @property defaultValues 비워 둔 계수의 기본값 (지정하지 않은 계수는 0)
 */
enum class BeginnerFunctionType(
    val displayName: String,
    val inputLabels: List<String>,
    val defaultValues: Map<String, String> = mapOf("a" to "1")
) {
    LINEAR("일차", listOf("a", "b")),       // y = ax + b
    QUADRATIC("이차", listOf("a", "b", "c")), // y = ax^2 + bx + c
    CUBIC("삼차", listOf("a", "b", "c", "d")), // y = ax^3 + bx^2 + cx + d
    RATIONAL("유리", listOf("a", "b", "c")),   // y = a/(x+b) + c
    EXPONENTIAL("지수", listOf("a", "b", "c"), mapOf("a" to "1", "b" to "2")), // y = a·b^x + c
    LOGARITHM("로그", listOf("a", "b", "c")), // y = a·log(x+b) + c
    SINE("삼각", listOf("a", "b", "c", "d"), mapOf("a" to "1", "b" to "1")); // y = a·sin(bx+c) + d

    /**
     * 사용자가 입력한 계수 값들을 바탕으로
     * 파싱 가능한 수식 문자열(예: "(1)*x + (2)")을 생성합니다.
     * 비어 있는 계수는 [defaultValues] (없으면 0) 으로 간주합니다.
     *
     * @return 수식 문자열. 숫자가 아닌 계수가 있으면 null
     */
    fun buildExpression(coefficients: Map<String, String>): String? {
        val values = inputLabels.associateWith { label ->
            val raw = coefficients[label]?.trim().orEmpty()
            when {
                raw.isEmpty() -> defaultValues[label] ?: "0"
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
            EXPONENTIAL -> "($a)*($b)^x + ($c)"
            LOGARITHM -> "($a)*log(x + ($b)) + ($c)"
            SINE -> "($a)*sin(($b)*x + ($c)) + ($d)"
        }
    }
}
