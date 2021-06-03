package com.onelogin.mfa.data.network

sealed class NetworkResponse<out T> {
    data class Success<out T>(val value: T): NetworkResponse<T>()
    data class GenericException(val throwable: Throwable): NetworkResponse<Nothing>()
    data class NetworkException(val code: Int? = null, val errorResponse: NetworkUtils.ErrorResponse? = null): NetworkResponse<Nothing>()
}
