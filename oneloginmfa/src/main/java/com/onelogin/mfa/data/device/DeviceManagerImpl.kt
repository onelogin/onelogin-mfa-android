package com.onelogin.mfa.data.device

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.api.OneLoginApiService
import com.onelogin.mfa.data.api.RegistrationResponse
import com.onelogin.mfa.data.api.Settings
import com.onelogin.mfa.data.api.SettingsResponse
import com.onelogin.mfa.data.network.NetworkResponse.GenericException
import com.onelogin.mfa.data.network.NetworkResponse.Success
import com.onelogin.mfa.data.network.NetworkResponse.NetworkException
import com.onelogin.mfa.data.network.NetworkUtils.apiCall
import com.onelogin.mfa.model.Factor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

internal class DeviceManagerImpl(
    private val context: Context,
    private val oneLoginApiService: OneLoginApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): DeviceManager {

    override suspend fun registerDevice(code: String, issuer: String, shard: String): RegistrationResponse {
        val registrationResponse = apiCall(dispatcher) {
            oneLoginApiService.deviceRegistration(code, issuer, shard)
        }

        when (registrationResponse) {
            is Success -> return registrationResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    registrationResponse.errorResponse?.error,
                    code = registrationResponse.code,
                    errorDescription = "HTTP exception occurred when registering factor"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    registrationResponse.throwable.message,
                    registrationResponse.throwable.cause
                )
            }
            else -> {
                throw OneLoginMfaException("Failed to register factor")
            }
        }
    }

    override suspend fun checkDeviceSettings(factor: Factor): SettingsResponse {
        val settingsResponse = apiCall(dispatcher) {
            oneLoginApiService.deviceSettings(factor.credentialId!!, factor.shard!!)
        }

        when (settingsResponse) {
            is Success -> {
                return settingsResponse.value
            }
            is NetworkException -> {
                // Factor has been unpaired, will throw 404 when checking device settings
                if (settingsResponse.code == 404) {
                    return SettingsResponse(
                        isSuccess = false, unpaired = false, settings = Settings(
                            disallowJailbrokenOrRooted = false,
                            forceLockProtection = false,
                            disableBackup = true,
                            biometricVerification = false
                    ))
                }

                throw OneLoginMfaException(
                    settingsResponse.errorResponse?.error,
                    code = settingsResponse.code,
                    errorDescription = "HTTP exception occurred when retrieving device settings"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    settingsResponse.throwable.message,
                    settingsResponse.throwable.cause
                )
            }
            else -> {
                throw OneLoginMfaException("Failed to retrieve device settings")
            }
        }
    }

    override fun isDeviceSecure(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardSecure
    }

    override fun isDeviceRooted(): Boolean {
        // There is no way to verify with 100% certainty whether or not a device is rooted
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys"))
            return true


        // Check if Superuser.apk or any su binaries exist
        val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su")
        for (path in paths) {
            if (File(path).exists()) return true
        }

        // Attempt to run su
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val bufferedReader = BufferedReader(InputStreamReader(process!!.inputStream))
            bufferedReader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }


}
