package com.onelogin.mfa.model

import java.lang.Exception

class RegisterFactorError(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
