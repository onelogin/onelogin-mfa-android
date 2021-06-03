package com.onelogin.mfa.data.factor

import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.api.*
import com.onelogin.mfa.data.factor.FactorManagerImpl.Companion.PROTECT_FACTOR_ID
import com.onelogin.mfa.data.factor.FactorManagerImpl.Companion.PROTECT_FACTOR_NAME
import com.onelogin.mfa.data.network.NetworkUtils.apiCall
import com.onelogin.mfa.data.network.NetworkResponse.Success
import com.onelogin.mfa.data.network.NetworkResponse.GenericException
import com.onelogin.mfa.data.network.NetworkResponse.NetworkException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class WebLoginHelper(
    private val subdomainApiService: SubdomainApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private lateinit var subdomain: String
    private lateinit var username: String
    private lateinit var password: String

    private lateinit var usernameJwt: String
    private lateinit var passwordJwt: String
    private lateinit var registrationNoticeJwt: String
    private lateinit var authorizationJwt: String
    private lateinit var availableFactorsJwt: String

    suspend fun register(subdomain: String, username: String, password: String): String {
        if (subdomain.isBlank() || username.isBlank() || password.isBlank()) {
            throw OneLoginMfaException("Empty web login fields")
        }

        this.subdomain = subdomain
        this.username = username
        this.password = password

        val isDomainAvailable = verifySubdomain().data
        if (isDomainAvailable.isEmpty() || isDomainAvailable[0].subdomainAvailable) {
            throw OneLoginMfaException("Invalid subdomain")
        }

        val initialAuthorization = getInitialAuthorization()
        usernameJwt = initialAuthorization.context.jwt

        val uploadUsername = uploadUsername()
        passwordJwt = uploadUsername.context.jwt

        val uploadPassword = uploadPassword()
        registrationNoticeJwt = uploadPassword.context.jwt

        val mfaRegistrationNotice = getMfaRegistrationNotice()
        authorizationJwt = mfaRegistrationNotice.mfa?.jwt ?: ""

        val mfaAuthorization = getMfaAuthorization()
        availableFactorsJwt = mfaAuthorization.jwt

        val availableFactors = getAvailableFactors()
        if (availableFactors.isEmpty()) {
            throw OneLoginMfaException("OneLogin Protect must be an available factor")
        }

        val factorId = availableFactors
            .firstOrNull {
                it.type_id == PROTECT_FACTOR_ID && it.name.contains(PROTECT_FACTOR_NAME, ignoreCase = true)
            }?.id ?: throw OneLoginMfaException("OneLogin Protect must be an available factor")

        val factorToken = getFactorToken(factorId).verificationToken

        if (factorToken.isNullOrBlank()) {
            throw OneLoginMfaException("Empty factor token")
        }
        return factorToken
    }

    private suspend fun verifySubdomain(): DomainAvailableResponse {
       val domainAvailableResponse = apiCall(dispatcher) {
           subdomainApiService.isDomainAvailable(subdomain)
       }

        when(domainAvailableResponse) {
            is Success -> return domainAvailableResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    domainAvailableResponse.errorResponse?.error,
                    code = domainAvailableResponse.code,
                    errorDescription = "HTTP exception verifying subdomain"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    domainAvailableResponse.throwable.message,
                    domainAvailableResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Error verifying subdomain")
        }
    }

    private suspend fun getInitialAuthorization(): AccessServiceResponse {
        val initialAuthorizationResponse = apiCall(dispatcher) {
            subdomainApiService.getInitialAuthorization(InitialAuthorizationRequest(""), subdomain)
        }

        when (initialAuthorizationResponse) {
            is Success -> return initialAuthorizationResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    initialAuthorizationResponse.errorResponse?.error,
                    code = initialAuthorizationResponse.code,
                    errorDescription = "HTTP exception retrieving initial authorization"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    initialAuthorizationResponse.throwable.message,
                    initialAuthorizationResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to retrieve initial authorization response")
        }
    }

    private suspend fun uploadUsername(): AccessServiceResponse {
        val uploadUsernameResponse = apiCall(dispatcher) {
            val payload = UploadUsernameRequest(UploadUsernameVariables(username))
            subdomainApiService.uploadUsername(payload = payload, domain = subdomain, jwt = usernameJwt)
        }

        when (uploadUsernameResponse) {
            is Success -> return uploadUsernameResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    uploadUsernameResponse.errorResponse?.error,
                    code = uploadUsernameResponse.code,
                    errorDescription = "HTTP exception uploading username"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    uploadUsernameResponse.throwable.message,
                    uploadUsernameResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to upload username")
        }
    }

    private suspend fun uploadPassword(): AccessServiceResponse {
        val uploadPasswordResponse = apiCall(dispatcher) {
            val payload = UploadPasswordRequest(UploadPasswordVariables(password))
            subdomainApiService.uploadPassword(payload = payload, domain = subdomain, jwt = passwordJwt)
        }

        when (uploadPasswordResponse) {
            is Success -> return uploadPasswordResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    uploadPasswordResponse.errorResponse?.error,
                    code = uploadPasswordResponse.code,
                    errorDescription = "HTTP exception uploading password"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    uploadPasswordResponse.throwable.message,
                    uploadPasswordResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to upload password")
        }
    }

    private suspend fun getMfaRegistrationNotice(): AccessServiceResponse {
        val mfaRegistrationResponse = apiCall(dispatcher) {
            val payload = GetMfaRegistrationNoticeRequest(GetMfaRegistrationNoticeVariables())
            subdomainApiService.getMfaRegistrationNotice(payload = payload, domain = subdomain, jwt = registrationNoticeJwt)
        }

        when (mfaRegistrationResponse) {
            is Success -> return mfaRegistrationResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                        mfaRegistrationResponse.errorResponse?.error,
                        code = mfaRegistrationResponse.code,
                        errorDescription = "HTTP exception retrieving MFA registration notice"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                        mfaRegistrationResponse.throwable.message,
                        mfaRegistrationResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to retrieve MFA registration notice")
        }
    }

    private suspend fun getMfaAuthorization(): AccessServiceMfa  {
        val mfaAuthorizationResponse = apiCall(dispatcher) {
            subdomainApiService.getMfaAuthorization(subdomain, jwt = authorizationJwt)
        }

        when (mfaAuthorizationResponse) {
            is Success -> return mfaAuthorizationResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    mfaAuthorizationResponse.errorResponse?.error,
                    code = mfaAuthorizationResponse.code,
                    errorDescription = "HTTP exception retrieving MFA authorization"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    mfaAuthorizationResponse.throwable.message,
                    mfaAuthorizationResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to retrieve MFA authorization")
        }
    }

    private suspend fun getAvailableFactors(): List<AvailableFactors> {
        val availableFactorsResponse = apiCall(dispatcher) {
            subdomainApiService.getAvailableFactors(subdomain, jwt = availableFactorsJwt)
        }

        when (availableFactorsResponse) {
            is Success -> return availableFactorsResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    availableFactorsResponse.errorResponse?.error,
                    code = availableFactorsResponse.code,
                    errorDescription = "HTTP exception retrieving available factors"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    availableFactorsResponse.throwable.message,
                    availableFactorsResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to retrieve available factors")
        }
    }


    private suspend fun getFactorToken(factorId: Int): GetMfaTokenResponse {
        val factorTokenResponse = apiCall(dispatcher) {
            subdomainApiService.getFactorToken(factorId, subdomain, jwt = availableFactorsJwt)
        }

        when (factorTokenResponse) {
            is Success -> return factorTokenResponse.value
            is NetworkException -> {
                throw OneLoginMfaException(
                    factorTokenResponse.errorResponse?.error,
                    code = factorTokenResponse.code,
                    errorDescription = "HTTP exception retrieving factor token"
                )
            }
            is GenericException -> {
                throw OneLoginMfaException(
                    factorTokenResponse.throwable.message,
                    factorTokenResponse.throwable.cause,
                )
            }
            else -> throw OneLoginMfaException("Failed to retrieve factor token")
        }
    }
}
