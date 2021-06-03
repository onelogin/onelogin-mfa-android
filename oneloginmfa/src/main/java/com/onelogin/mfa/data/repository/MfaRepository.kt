package com.onelogin.mfa.data.repository

import com.onelogin.mfa.model.Factor

internal interface MfaRepository {
    suspend fun addFactor(factor: Factor): Long

    suspend fun getFactorByCredentialId(credentialId: String): Factor?

    suspend fun getFactorById(id: Long): Factor?

    suspend fun getFactorsByIssuer(issuer: String): List<Factor>

    suspend fun getAllFactors(): List<Factor>

    suspend fun updateFactor(factor: Factor): Int

    suspend fun deleteFactor(factor: Factor): Int

    suspend fun deleteAllFactors(): Int

    suspend fun deleteFactorByCredentialId(credentialId: String): Int

    suspend fun deleteFactorById(id: Long): Int

}
