package com.devhjs.mathgraphstudy.domain.model

import com.devhjs.mathgraphstudy.domain.model.math.enums.BeginnerFunctionType

/** 함수를 만드는 두 가지 입력 방식 */
sealed interface FunctionInput {
    /** 키패드로 직접 입력한 수식 문자열 (예: "2 x ^ 2 + 1") */
    data class Expression(val text: String) : FunctionInput

    /** 템플릿(일차/이차/삼차/유리) 종류와 계수 */
    data class Template(
        val type: BeginnerFunctionType,
        val coefficients: Map<String, String>
    ) : FunctionInput
}
