package com.onelogin.mfa.data.factor

import com.onelogin.mfa.model.Factor
import java.util.*

internal interface FactorManager {

    companion object {
        val CRYPTO_SUPPORTED = HashSet(listOf("MD5", "SHA1", "SHA224", "SHA256", "SHA384", "SHA512"))
    }

    suspend fun registerFactor(code: String): Long
    suspend fun registerFactorByWebLogin(subdomain: String, username: String, password: String): Long
    suspend fun updateFactor(factor: Factor): Int
    suspend fun getFactors(): List<Factor>
    suspend fun getFactorsByIssuer(issuer: String): List<Factor>
    suspend fun getFactorById(id: Long): Factor?
    suspend fun getFactorByCredentialId(credentialId: String): Factor?
    suspend fun removeFactor(factor: Factor): Int
    suspend fun removeAllFactors(): Int
    suspend fun removeFactorById(id: Long): Int
    suspend fun removeFactorByCredentialId(credentialId: String): Int
}
