package com.onelogin.mfa.data.factor

import android.net.Uri
import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.api.SubdomainApiService
import com.onelogin.mfa.data.device.DeviceManager
import com.onelogin.mfa.data.encryption.EncryptionManager
import com.onelogin.mfa.data.factor.FactorManager.Companion.CRYPTO_SUPPORTED
import com.onelogin.mfa.data.repository.MfaRepository
import com.onelogin.mfa.data.util.OneLoginMfaUtils
import com.onelogin.mfa.model.Factor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

internal class FactorManagerImpl(
    private val repository: MfaRepository,
    private val encryptionManager: EncryptionManager,
    private val deviceManager: DeviceManager,
    subdomainApi: SubdomainApiService
): FactorManager {

    private val webLoginHelper: WebLoginHelper = WebLoginHelper(subdomainApi)

    override suspend fun registerFactor(code: String): Long {
        if (code.isEmpty() || code.isBlank()) {
            throw OneLoginMfaException("Invalid code format")
        }
        return if (OneLoginMfaUtils.isOneLoginCode(code)) {
            registerOneLoginFactor(code)
        } else {
            registerThirdPartyFactor(code)
        }
    }

    override suspend fun registerFactorByWebLogin(
        subdomain: String,
        username: String,
        password: String
    ): Long = registerFactor(webLoginHelper.register(subdomain, username, password))

    override suspend fun updateFactor(factor: Factor): Int =
        repository.updateFactor(encryptFactor(factor))

    override suspend fun getFactors(): List<Factor> =
        decryptFactors(repository.getAllFactors())

    override suspend fun getFactorsByIssuer(issuer: String): List<Factor> =
        decryptFactors(repository.getFactorsByIssuer(issuer))

    override suspend fun getFactorById(id: Long): Factor? {
        val factor = repository.getFactorById(id)

        return if (factor == null) {
            null
        } else {
            decryptFactor(factor)
        }
    }

    override suspend fun getFactorByCredentialId(credentialId: String): Factor? {
        val factor = repository.getFactorByCredentialId(credentialId)

        return if (factor == null) {
            null
        } else {
            decryptFactor(factor)
        }
    }

    override suspend fun removeFactor(factor: Factor): Int =
        repository.deleteFactor(factor)

    override suspend fun removeAllFactors(): Int =
        repository.deleteAllFactors()

    override suspend fun removeFactorById(id: Long): Int =
        repository.deleteFactorById(id)

    override suspend fun removeFactorByCredentialId(credentialId: String): Int =
        repository.deleteFactorByCredentialId(credentialId)

    private fun checkCryptoSupport(crypto: String): Boolean {
        return CRYPTO_SUPPORTED.contains(crypto.toUpperCase(Locale.ROOT))
    }

    private suspend fun registerOneLoginFactor(code: String): Long {
        val secret: String? = OneLoginMfaUtils.getOtpCode(code)
        val issuer = OneLoginMfaUtils.getIssuer(code)
        val shard = if (secret != null && secret.isNotEmpty()) secret.substring(0, 2) else null

        if (secret.isNullOrEmpty() || shard.isNullOrEmpty()) {
            throw OneLoginMfaException("Missing required fields")
        }

        val registrationResponse = withContext(Dispatchers.IO) {
            deviceManager.registerDevice(secret, issuer, shard)
        }

        val factor = Factor()
        factor.shard = shard
        factor.subdomain = registrationResponse.subdomain
        factor.displayName = registrationResponse.subdomain
        factor.issuer = issuer
        factor.username = registrationResponse.username
        factor.credentialId = registrationResponse.credentialId
        factor.seed = registrationResponse.seed

        val updatedSettings = withContext(Dispatchers.IO) {
            deviceManager.checkDeviceSettings(factor)
        }.settings

        factor.allowRoot = updatedSettings.disallowJailbrokenOrRooted == false
        factor.forceLock = updatedSettings.forceLockProtection == true
        factor.requireBiometrics = updatedSettings.biometricVerification

        return repository.addFactor(encryptFactor(factor))
    }

    private suspend fun registerThirdPartyFactor(code: String): Long {
        val factor = Factor()

        val uri = Uri.parse(code)
        val seed = uri.getQueryParameter("secret")
        val issuer = uri.getQueryParameter("issuer")
        val digits = uri.getQueryParameter("digits")
        val period = uri.getQueryParameter("period")
        var crypto: String? = uri.getQueryParameter("algorithm")
        crypto = if (crypto == null || crypto.isEmpty()) "SHA1" else crypto

        val subdomainAndUsername = uri.lastPathSegment!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (seed.isNullOrEmpty()) {
            factor.seed = code
            return repository.addFactor(encryptFactor(factor))
        }

        if (!checkCryptoSupport(crypto))
            throw OneLoginMfaException("Crypto algorithm not supported")

        if (subdomainAndUsername.size == 1) {
            factor.username = subdomainAndUsername[0]
        } else if (subdomainAndUsername.size == 2) {
            factor.issuer = if (factor.issuer.isNullOrEmpty()) subdomainAndUsername[0] else factor.issuer
            factor.username = subdomainAndUsername[1]
        }

        factor.issuer = issuer
        factor.seed = seed
        factor.crypto = "Hmac${crypto.toUpperCase(Locale.ROOT)}"

        if (digits != null && digits.isNotEmpty())
            factor.digits = Integer.parseInt(digits)

        if (period != null && period.isNotEmpty())
            factor.period = Integer.parseInt(period)

        return repository.addFactor(encryptFactor(factor))
    }

    private fun encryptFactor(factor: Factor): Factor {
        if (!encryptionManager.isSupported()) {
            return factor
        }

        return try {
            val encryptedSeed = encryptionManager.encrypt(factor.seed)
            factor.seed = encryptedSeed
            factor
        } catch (e: Exception) {
            Timber.e(e, "Corrupted factor detected while attempting encryption: $factor")
            CORRUPTED_FACTOR
        }
    }

    private fun decryptFactor(factor: Factor): Factor {
        if (!encryptionManager.isSupported()) {
            return factor
        }

        return try {
            val decryptedSeed = encryptionManager.decrypt(factor.seed)
            factor.seed = decryptedSeed
            factor
        } catch (e: Exception) {
            Timber.e(e, "Corrupted factor detected while attempting decryption: $factor")
            CORRUPTED_FACTOR
        }
    }

    private fun decryptFactors(factors: List<Factor>) : List<Factor> {
        if (!encryptionManager.isSupported()) {
            return factors
        }

        val decryptedFactors = ArrayList<Factor>()
        for (factor in factors) {
            val decryptedFactor = decryptFactor(factor)
            if (decryptedFactor == CORRUPTED_FACTOR)
                continue

            decryptedFactors.add(decryptedFactor)
        }

        return decryptedFactors
    }

    companion object {
        val CORRUPTED_FACTOR: Factor = Factor()
        const val PROTECT_FACTOR_ID: Int = 8
        const val PROTECT_FACTOR_NAME: String = "OneLogin Protect"
    }
}
