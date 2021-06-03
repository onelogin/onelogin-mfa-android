package com.onelogin.mfa.data.repository

import com.onelogin.mfa.data.db.FactorEntity
import com.onelogin.mfa.data.db.FactorDao
import com.onelogin.mfa.model.Factor

internal class MfaRepositoryImpl(private val factorDao: FactorDao) : MfaRepository {

    override suspend fun addFactor(factor: Factor): Long =
        factorDao.insertFactor(FactorEntity.fromFactor(factor))

    override suspend fun getFactorByCredentialId(credentialId: String): Factor? =
        factorDao.getFactorByCredentialId(credentialId)

    override suspend fun getFactorById(id: Long): Factor? =
        factorDao.getFactorById(id)

    override suspend fun getFactorsByIssuer(issuer: String): List<Factor> =
        factorDao.getFactorsByIssuer(issuer)

    override suspend fun getAllFactors(): List<Factor> =
        factorDao.getAllFactors()

    override suspend fun updateFactor(factor: Factor): Int =
        factorDao.updateFactor(FactorEntity.fromFactor(factor))

    override suspend fun deleteFactor(factor: Factor): Int =
        factorDao.deleteFactor(FactorEntity.fromFactor(factor))

    override suspend fun deleteAllFactors(): Int =
        factorDao.deleteAllFactors()

    override suspend fun deleteFactorByCredentialId(credentialId: String): Int =
        factorDao.deleteFactorByCredentialId(credentialId)

    override suspend fun deleteFactorById(id: Long): Int =
        factorDao.deleteFactorById(id)
}
