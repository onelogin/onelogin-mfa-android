package com.onelogin.mfa.data.factor

import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.api.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
@ExperimentalCoroutinesApi
class WebLoginHelperTest {
    private val subdomainApi = mockk<SubdomainApiService>()
    private val dispatcher = TestCoroutineDispatcher()

    private lateinit var webLoginHelper: WebLoginHelper

    private val errorBody = "{\"error\": \"Unexpected parameter\"}".toResponseBody("application/json".toMediaTypeOrNull())
    private val domainAvailableResponse = DomainAvailableResponse(listOf(DomainAvailableData(false)))
    private val availableFactors = listOf(AvailableFactors(123, 8, "OneLogin Protect", "someDescription"))
    private val factorToken = GetMfaTokenResponse("someTokenId", "someStatus", "10-1234567")
    private val accessServiceResponse = AccessServiceResponse(
        "someAction",
        AccessServiceUser("someLogin"),
        "someStateMachine",
        AccessServiceContext("someContextJwt"),
        AccessServiceMfa("someMfaJwt"),
        persistentSessionOption = true,
        passwordValid = true,
        registrationSkippable = false,
        request = AccessServiceRequest(
            "someUri",
            AccessServiceParams(null, null, null),
            "someMethod"
        )
    )

    @Before
    fun setup() {
        webLoginHelper = WebLoginHelper(subdomainApi, dispatcher)
    }

    @Test
    fun testRegisterSuccess() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors
        coEvery { subdomainApi.getFactorToken(any(), any(), any()) } returns factorToken

        val result = runBlocking {
            webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
        }

        assertEquals("10-1234567", result)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterEmptyInputs() {
        try {
            runBlocking {
                webLoginHelper.register("", "", "")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Empty web login fields", exception.message)
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testisDomainAvailableInvalid() {
        val domainAvailableResponse = DomainAvailableResponse(listOf(DomainAvailableData(true)))
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Invalid subdomain", exception.message)
            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testIsDomainAvailableHttpException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } throws HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception verifying subdomain", exception.errorDescription)
            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testIsDomainAvailableRuntimeException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)
            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetInitialAuthHttpException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception retrieving initial authorization", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetInitialAuthRuntimeException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testUploadUsernameHttpException() {
        val uploadUsername = UploadUsernameRequest(
            UploadUsernameVariables("someUsername", false)
        )

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception uploading username", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.uploadUsername(payload = uploadUsername, domain = "someSubdomain", jwt = "someContextJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testUploadUsernameRuntimeException() {
        val uploadUsername = UploadUsernameRequest(
            UploadUsernameVariables("someUsername", false)
        )

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.uploadUsername(payload = uploadUsername, domain = "someSubdomain", jwt = "someContextJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testUploadPasswordHttpException() {
        val uploadPassword = UploadPasswordRequest(
            UploadPasswordVariables("somePassword")
        )

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception uploading password", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.uploadPassword(payload = uploadPassword, domain = "someSubdomain", jwt = "someContextJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testUploadPasswordRuntimeException() {
        val uploadPassword = UploadPasswordRequest(
            UploadPasswordVariables("somePassword")
        )

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.uploadPassword(payload = uploadPassword, domain = "someSubdomain", jwt = "someContextJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetMfaRegistrationNoticeHttpException() {
        val mfaRegistrationNotice = GetMfaRegistrationNoticeRequest(
            GetMfaRegistrationNoticeVariables()
        )

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception retrieving MFA registration notice", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getMfaRegistrationNotice(payload = mfaRegistrationNotice, domain = "someSubdomain", jwt = "someContextJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetMfaRegistrationNoticeRuntimeException() {
        val mfaRegistrationNotice = GetMfaRegistrationNoticeRequest(
            GetMfaRegistrationNoticeVariables()
        )

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getMfaRegistrationNotice(payload = mfaRegistrationNotice, domain = "someSubdomain", jwt = "someContextJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetMfaAuthorizationHttpException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception retrieving MFA authorization", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getMfaAuthorization(domain = "someSubdomain", jwt = "someMfaJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetMfaAuthorizationRuntimeException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getMfaAuthorization(domain = "someSubdomain", jwt = "someMfaJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetAvailableFactorsIneligibleFactorList() {
        val availableFactors = listOf(AvailableFactors(123, 100, "Not Correct", "someDescription"))

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("OneLogin Protect must be an available factor", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getAvailableFactors(domain = "someSubdomain", jwt = "someMfaJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetAvailableFactorsEmptyList() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns emptyList()

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("OneLogin Protect must be an available factor", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getAvailableFactors(domain = "someSubdomain", jwt = "someMfaJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetAvailableFactorsHttpException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception retrieving available factors", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getAvailableFactors(domain = "someSubdomain", jwt = "someMfaJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetAvailableFactorsRuntimeException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getAvailableFactors(domain = "someSubdomain", jwt = "someMfaJwt") }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetFactorTokenNullToken() {
        val factorToken = GetMfaTokenResponse("someTokenId", "someStatus", null)

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors
        coEvery { subdomainApi.getFactorToken(any(), any(), any()) } returns factorToken

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Empty factor token", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getFactorToken(domain = "someSubdomain", jwt = "someMfaJwt", factorId = 123) }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetFactorTokenBlankToken() {
        val factorToken = GetMfaTokenResponse("someTokenId", "someStatus", "       ")

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors
        coEvery { subdomainApi.getFactorToken(any(), any(), any()) } returns factorToken

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Empty factor token", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getFactorToken(domain = "someSubdomain", jwt = "someMfaJwt", factorId = 123) }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetFactorTokenHttpException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors
        coEvery { subdomainApi.getFactorToken(any(), any(), any()) } throws
                HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals(422, exception.code)
            assertEquals("Unexpected parameter", exception.message)
            assertEquals("HTTP exception retrieving factor token", exception.errorDescription)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getFactorToken(domain = "someSubdomain", jwt = "someMfaJwt", factorId = 123) }

            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testGetFactorTokenRuntimeException() {
        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors
        coEvery { subdomainApi.getFactorToken(any(), any(), any()) } throws RuntimeException("Runtime exception message")

        try {
            runBlocking {
                webLoginHelper.register("someSubdomain", "someUsername", "somePassword")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Runtime exception message", exception.message)

            coVerify(exactly = 1) { subdomainApi.isDomainAvailable("someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getInitialAuthorization(InitialAuthorizationRequest(""), "someSubdomain") }
            coVerify(exactly = 1) { subdomainApi.getFactorToken(domain = "someSubdomain", jwt = "someMfaJwt", factorId = 123) }

            throw exception
        }
    }
}
