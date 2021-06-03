package com.onelogin.mfa

import android.content.Context
import androidx.room.Room
import com.onelogin.mfa.data.db.MfaDatabase
import com.onelogin.mfa.data.device.DeviceManagerImpl
import com.onelogin.mfa.data.factor.FactorManagerImpl
import com.onelogin.mfa.data.encryption.EncryptionManagerImpl
import com.onelogin.mfa.data.network.NetworkProvider
import com.onelogin.mfa.data.repository.MfaRepositoryImpl

internal class MfaClientFactory(
    private val context: Context
) {

    fun build(): MfaClient {
        val oneLoginApiService = NetworkProvider.getOneLoginApi(context)
        val subdomainApiService = NetworkProvider.getSubdomainApi(context)
        val database = Room.databaseBuilder(context, MfaDatabase::class.java, "onelogin-mfa").build()
        val repository = MfaRepositoryImpl(database.factorDao())

        val encryptionManager = EncryptionManagerImpl(context)
        val deviceManager = DeviceManagerImpl(context, oneLoginApiService)
        val factorManager = FactorManagerImpl(repository, encryptionManager, deviceManager, subdomainApiService)

        return MfaClientImpl(deviceManager, factorManager)
    }
}
