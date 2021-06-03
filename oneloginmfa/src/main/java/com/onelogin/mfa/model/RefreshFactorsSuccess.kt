package com.onelogin.mfa.model

data class RefreshFactorsSuccess(
    var unpairedCount: Int = 0,
    var unpairedFactors: ArrayList<Factor> = arrayListOf(),
    var updatedCount: Int = 0,
    var updatedFactors: ArrayList<Factor> = arrayListOf(),
)
