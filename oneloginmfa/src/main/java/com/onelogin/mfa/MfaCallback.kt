package com.onelogin.mfa

import androidx.annotation.NonNull

interface MfaCallback<Success, Error : Exception> {
    fun onSuccess(@NonNull success: Success)
    fun onError(@NonNull error: Error)
}