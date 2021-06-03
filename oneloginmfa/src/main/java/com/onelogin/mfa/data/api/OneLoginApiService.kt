package com.onelogin.mfa.data.api

import retrofit2.http.*

internal interface OneLoginApiService {

    @FormUrlEncoded
    @POST(DEVICE_ENDPOINT)
    suspend fun deviceRegistration(
        @Field("registration_id") code: String,
        @Field("platform") platform: String,
        @Query("shard") shard: String): RegistrationResponse

    @GET("$SETTINGS_ENDPOINT/{credentialId}")
    suspend fun deviceSettings(
        @Path("credentialId") credentialId: String,
        @Query("shard") shard: String): SettingsResponse

    companion object {
        internal const val DEVICE_ENDPOINT = "/api/internal/v2/otp/devices"
        internal const val SETTINGS_ENDPOINT = "/api/internal/v2/otp/settings"
    }
}