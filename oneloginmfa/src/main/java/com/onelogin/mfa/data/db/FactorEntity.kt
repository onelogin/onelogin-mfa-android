@file:Suppress("unused", "unused")

package com.onelogin.mfa.data.db

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.onelogin.mfa.model.Factor

@Entity
internal data class FactorEntity(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var credentialId: String?,
    var subdomain: String?,
    var shard: String?,
    var username: String?,
    var seed: String,
    var issuer: String?,
    var creationDate: Long,
    var allowRoot: Boolean = true,
    var forceLock: Boolean = false,
    var paired: Boolean = true,
    var displayName: String?,
    var orderPreference: Int = 0,
    var crypto: String = "SHA1",
    var period: Int = 30,
    var digits: Int = 6,
    var allowBackup: Boolean = true,
    var requireBiometrics: Boolean = false
) {
    @Ignore
    constructor() : this(0, "", "", "", "", "", "", 0, true, false, true, "", 0, "HmacSHA1", 30, 6, true, false)

    fun toFactor(): Factor {
        return Factor(
            id = id,
            credentialId = credentialId,
            subdomain = subdomain,
            shard = shard,
            username = username,
            seed = seed,
            issuer = issuer,
            creationDate = creationDate,
            allowRoot = allowRoot,
            forceLock = forceLock,
            paired = paired,
            displayName = displayName,
            orderPreference = orderPreference,
            crypto = crypto,
            period = period,
            digits = digits,
            allowBackup = allowBackup,
            requireBiometrics = requireBiometrics
        )
    }

    companion object {
        fun fromFactor(factor: Factor): FactorEntity {
            return FactorEntity(
                id = factor.id,
                credentialId = factor.credentialId,
                subdomain = factor.subdomain,
                shard = factor.shard,
                username = factor.username,
                seed = factor.seed,
                issuer = factor.issuer,
                creationDate = factor.creationDate,
                allowRoot = factor.allowRoot,
                forceLock = factor.forceLock,
                paired = factor.paired,
                displayName = factor.displayName,
                orderPreference = factor.orderPreference,
                crypto = factor.crypto,
                period = factor.period,
                digits = factor.digits,
                allowBackup = factor.allowBackup,
                requireBiometrics = factor.requireBiometrics
            )
        }
    }
}
