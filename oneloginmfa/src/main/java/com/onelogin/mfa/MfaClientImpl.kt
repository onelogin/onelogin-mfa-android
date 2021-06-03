package com.onelogin.mfa

import com.onelogin.mfa.data.device.DeviceManager
import com.onelogin.mfa.model.Factor
import com.onelogin.mfa.data.factor.FactorManager
import com.onelogin.mfa.model.RefreshFactorsSuccess
import com.onelogin.mfa.model.RegisterFactorError
import com.onelogin.mfa.model.RegisterFactorSuccess
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.Exception

internal class MfaClientImpl(
    private val deviceManager: DeviceManager,
    private val factorManager: FactorManager
) : MfaClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun registerFactor(
        code: String,
        registerFactorCallback: MfaCallback<RegisterFactorSuccess, RegisterFactorError>
    ) {
        scope.launch {
            runCatching {
                factorManager.registerFactor(code)
            }.fold(
                {
                    Timber.d("Successfully added factor: $it")
                    registerFactorCallback.onSuccess(RegisterFactorSuccess(it))
                },
                {
                    Timber.e("Failed to add factor")
                    registerFactorCallback.onError(RegisterFactorError("Failed to add factor", it))
                }
            )
        }
    }

    override fun registerFactorByWebLogin(
        subdomain: String,
        username: String,
        password: String,
        registerFactorByWebLogin: MfaCallback<RegisterFactorSuccess, RegisterFactorError>
    ) {
        scope.launch {
            runCatching {
                factorManager.registerFactorByWebLogin(subdomain, username, password)
            }.fold(
                {
                    Timber.d("Successfully added factor via web login: $it")
                    registerFactorByWebLogin.onSuccess(RegisterFactorSuccess(it))
                },
                {
                    Timber.e(it, "Failed to add factor via web login")
                    registerFactorByWebLogin.onError(RegisterFactorError("Failed to add factor via web login", it))
                }
            )
        }
    }

    override fun getFactors(getFactorsCallback: MfaCallback<List<Factor>, Exception>) {
        scope.launch {
            runCatching {
                factorManager.getFactors()
            }.fold(
                {
                    Timber.d("Retrieved all factors: $it")
                    getFactorsCallback.onSuccess(it)
                },
                {
                    Timber.e(it,"Failed to retrieve all factors")
                    getFactorsCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun getFactorById(id: Long, getFactorByIdCallback: MfaCallback<Factor?, Exception>) {
        scope.launch {
            runCatching {
                factorManager.getFactorById(id)
            }.fold(
                {
                    Timber.d("Get factor by ID: $it")
                    getFactorByIdCallback.onSuccess(it)
                },
                {
                    Timber.e(it,"Failed to retrieve factor by ID")
                    getFactorByIdCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun getFactorByCredentialId(
        credentialId: String,
        getFactorByCredentialIdCallback: MfaCallback<Factor?, Exception>
    ) {
        scope.launch {
            runCatching {
                factorManager.getFactorByCredentialId(credentialId)
            }.fold(
                {
                    Timber.d("Get factor by credential ID: $it")
                    getFactorByCredentialIdCallback.onSuccess(it)
                },
                {
                    Timber.e(it, "Failed to retrieve factor by credential ID: $credentialId")
                    getFactorByCredentialIdCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun removeFactor(factor: Factor, removeFactorCallback: MfaCallback<Int, Exception>) {
        scope.launch {
            runCatching {
                factorManager.removeFactor(factor)
            }.fold(
                {
                    Timber.d("Delete factor: $factor")
                    removeFactorCallback.onSuccess(it)
                },
                {
                    Timber.e(it, "Failed to delete factor: $factor")
                    removeFactorCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun removeAllFactors(removeAllFactorsCallback: MfaCallback<Int, Exception>) {
        scope.launch {
            kotlin.runCatching {
                factorManager.removeAllFactors()
            }.fold(
                {
                    Timber.d("Delete all factors!")
                    removeAllFactorsCallback.onSuccess(it)
                },
                {
                    Timber.e(it, "Failed to delete all factors!")
                    removeAllFactorsCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun removeFactorById(id: Long, removeFactorByIdCallback: MfaCallback<Int, Exception>) {
        scope.launch {
            kotlin.runCatching {
                factorManager.removeFactorById(id)
            }.fold(
                {
                    Timber.d("Delete factor by ID: $id")
                    removeFactorByIdCallback.onSuccess(it)
                },
                {
                    Timber.e(it, "Failed to delete factor by ID: $id")
                    removeFactorByIdCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun removeFactorByCredentialId(
        credentialId: String,
        removeFactorByCredentialIdCallback: MfaCallback<Int, Exception>
    ) {
        scope.launch {
            kotlin.runCatching {
                factorManager.removeFactorByCredentialId(credentialId)
            }.fold(
                {
                    Timber.d("Delete factor by credential credential ID: $credentialId")
                    removeFactorByCredentialIdCallback.onSuccess(it)
                },
                {
                    Timber.e(it, "Failed to delete factor by credential ID: $credentialId")
                    removeFactorByCredentialIdCallback.onError(Exception(it))
                }
            )
        }
    }

    override fun refreshFactors(refreshFactorsCallback: MfaCallback<RefreshFactorsSuccess, Exception>) {
        val results = RefreshFactorsSuccess()
        scope.launch {
            kotlin.runCatching {
                factorManager.getFactorsByIssuer("OneLogin").map { factor ->
                    deviceManager.checkDeviceSettings(factor).let { settingsResponse ->

                        if (!settingsResponse.isSuccess || settingsResponse.unpaired) {
                            factor.paired = false
                            results.unpairedCount++
                            results.unpairedFactors.add(factor)
                            factorManager.removeFactor(factor)
                        }

                        val settings = settingsResponse.settings

                        // Only update settings if factor is still paired
                        if (factor.paired && (settings.disallowJailbrokenOrRooted == factor.allowRoot ||
                                    settings.forceLockProtection != factor.forceLock ||
                                    settings.biometricVerification != factor.requireBiometrics)
                        ) {
                            factor.allowRoot = settings.disallowJailbrokenOrRooted == false
                            factor.forceLock = settings.forceLockProtection == true
                            factor.requireBiometrics = settings.biometricVerification
                            results.updatedCount++
                            results.updatedFactors.add(factor)
                            factorManager.updateFactor(factor)
                        }
                    }
                }
            }.fold(
                    {
                        Timber.d("Successfully refreshed factors")
                        refreshFactorsCallback.onSuccess(results)
                    },
                    {
                        Timber.e(it, "Failed to refresh factors")
                        refreshFactorsCallback.onError(Exception(it))
                    }
            )
        }
    }

    override fun cancel() {
        scope.coroutineContext.cancelChildren()
    }
}
