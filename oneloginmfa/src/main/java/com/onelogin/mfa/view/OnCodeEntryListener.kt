package com.onelogin.mfa.view

import androidx.annotation.NonNull

interface OnCodeEntryListener {
    fun onOneLoginCode(@NonNull code: String)
    fun onThirdPartyCode(@NonNull code: String)
}
