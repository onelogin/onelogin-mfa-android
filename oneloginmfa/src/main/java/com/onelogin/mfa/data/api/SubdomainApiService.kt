package com.onelogin.mfa.data.api

import retrofit2.http.*

internal interface SubdomainApiService {

    @GET(DOMAIN_AVAILABLE_ENDPOINT)
    suspend fun isDomainAvailable(@Query("domain") domain: String): DomainAvailableResponse

    @POST(ACCESS_AUTH_ENDPOINT)
    suspend fun getInitialAuthorization(
        @Body payload: InitialAuthorizationRequest,
        @Query("domain") domain: String
    ): AccessServiceResponse

    @PUT(ACCESS_AUTH_ENDPOINT)
    suspend fun uploadUsername(
        @Query("state") state: String? = "username",
        @Body payload: UploadUsernameRequest,
        @Query("domain") domain: String,
        @Query("jwt") jwt: String
    ): AccessServiceResponse

    @PUT(ACCESS_AUTH_ENDPOINT)
    suspend fun uploadPassword(
        @Query("state") state: String? = "password",
        @Body payload: UploadPasswordRequest,
        @Query("domain") domain: String,
        @Query("jwt") jwt: String
    ): AccessServiceResponse

    @PUT(ACCESS_AUTH_ENDPOINT)
    suspend fun getMfaRegistrationNotice(
        @Query("state") state: String? = "mfa_registration_notice",
        @Body payload: GetMfaRegistrationNoticeRequest,
        @Query("domain") domain: String,
        @Query("jwt") jwt: String
    ): AccessServiceResponse

    @GET(MFA_AUTH_ENDPOINT)
    suspend fun getMfaAuthorization(
        @Query("domain") domain: String,
        @Query("jwt") jwt: String
    ): AccessServiceMfa

    @GET(FACTORS_ENDPOINT)
    suspend fun getAvailableFactors(
        @Query("domain") domain: String,
        @Query("jwt") jwt: String
    ): List<AvailableFactors>

    @POST(REGISTRATION_ENDPOINT)
    suspend fun getFactorToken(
        @Query("factor_id") factorId: Int,
        @Query("domain") domain: String,
        @Query("jwt") jwt: String
    ): GetMfaTokenResponse

    companion object {
        internal const val DOMAIN_AVAILABLE_ENDPOINT = "/api/v1/domain_available"
        internal const val BRANDING_INFO_ENDPOINT = "/api/v3/custom_brand"
        internal const val ACCESS_AUTH_ENDPOINT = "/access/auth"
        internal const val MFA_AUTH_ENDPOINT = "/mfa/v1/auth"
        internal const val FACTORS_ENDPOINT = "/mfa/v1/factors"
        internal const val REGISTRATION_ENDPOINT = "/mfa/v1/registrations"
    }
}