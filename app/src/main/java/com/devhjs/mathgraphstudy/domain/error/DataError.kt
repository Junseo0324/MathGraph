package com.devhjs.mathgraphstudy.domain.error

sealed interface DataError {

    // 로컬 저장소(Room) 관련 에러
    enum class Local : DataError {
        UNKNOWN;

        fun toMessage(): String = when (this) {
            UNKNOWN -> "함수를 저장하거나 불러오지 못했습니다."
        }
    }
}
