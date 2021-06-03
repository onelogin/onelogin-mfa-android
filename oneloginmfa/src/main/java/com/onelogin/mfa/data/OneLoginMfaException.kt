package com.onelogin.mfa.data

data class OneLoginMfaException(
    override val message: String?,
    override val cause: Throwable? = null,
    val code: Int? = null,
    val errorDescription: String? = null
) : Exception(message, cause)