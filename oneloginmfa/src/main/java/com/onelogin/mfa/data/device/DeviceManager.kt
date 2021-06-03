package com.onelogin.mfa.data.device

import com.onelogin.mfa.data.api.RegistrationResponse
import com.onelogin.mfa.data.api.SettingsResponse
import com.onelogin.mfa.model.Factor

internal interface DeviceManager {
    suspend fun registerDevice(code: String, issuer: String, shard: String): RegistrationResponse
    suspend fun checkDeviceSettings(factor: Factor): SettingsResponse
    fun isDeviceSecure(): Boolean
    fun isDeviceRooted(): Boolean
}
