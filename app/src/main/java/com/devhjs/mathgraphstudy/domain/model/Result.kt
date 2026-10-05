package com.devhjs.mathgraphstudy.domain.model

/**
 * 성공/실패를 타입으로 표현하는 결과 래퍼입니다.
 * UseCase만 이 타입을 반환하고, Repository/DataSource는 예외를 그대로 던집니다.
 */
sealed class Result<out T, out E> {
    data class Success<out T>(
        val data: T
    ) : Result<T, Nothing>()

    data class Error<out E>(
        val error: E
    ) : Result<Nothing, E>()
}
