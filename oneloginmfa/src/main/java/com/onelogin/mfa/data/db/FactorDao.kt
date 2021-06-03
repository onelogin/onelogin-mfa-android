package com.onelogin.mfa.data.db

import androidx.room.*
import com.onelogin.mfa.model.Factor

@Dao
internal interface FactorDao {

    @Insert
    suspend fun insertFactor(factor: FactorEntity): Long

    @Query("SELECT * FROM FactorEntity WHERE credentialId = :credentialId LIMIT 1")
    suspend fun getFactorByCredentialId(credentialId: String): Factor?

    @Query("SELECT * FROM FactorEntity WHERE id = :id LIMIT 1")
    suspend fun getFactorById(id: Long): Factor?

    @Query("SELECT * FROM FactorEntity WHERE UPPER(issuer) = UPPER(:issuer)")
    suspend fun getFactorsByIssuer(issuer: String): List<Factor>

    @Query("SELECT * FROM FactorEntity ORDER BY orderPreference ASC, creationDate ASC")
    suspend fun getAllFactors(): List<Factor>

    @Update
    suspend fun updateFactor(factor: FactorEntity): Int

    @Delete
    suspend fun deleteFactor(factor: FactorEntity): Int

    @Query("DELETE FROM FactorEntity")
    suspend fun deleteAllFactors(): Int

    @Query("DELETE FROM FactorEntity WHERE credentialId = :credentialId")
    suspend fun deleteFactorByCredentialId(credentialId: String): Int

    @Query("DELETE FROM FactorEntity WHERE id = :id")
    suspend fun deleteFactorById(id: Long): Int
}
