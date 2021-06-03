package com.onelogin.mfa.data.device

import android.app.KeyguardManager
import android.content.Context
import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.api.OneLoginApiService
import com.onelogin.mfa.data.api.RegistrationResponse
import com.onelogin.mfa.data.api.Settings
import com.onelogin.mfa.data.api.SettingsResponse
import com.onelogin.mfa.model.Factor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class DeviceManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val oneLoginApiService = mockk<OneLoginApiService>()
    private val keyguardManager = mockk<KeyguardManager>()

    private lateinit var deviceManager: DeviceManager

    private val errorBody = "{\"error\": \"Unexpected parameter\"}".toResponseBody("application/json".toMediaTypeOrNull())
    private val testFactor = Factor(
        id = 123456,
        credentialId = "someCredentialId",
        subdomain = "someSubdomain",
        shard = "someShard",
        username = "someUsername",
        seed = "someSeed",
        issuer = "someIssuer",
        creationDate = 7654321L,
        allowRoot = false,
        forceLock = true,
        paired = true,
        displayName = "someDisplayName",
        orderPreference = 0,
        crypto = "someCrypto",
        period = 30,
        digits = 6,
        allowBackup = true,
        requireBiometrics = false
    )

    @Before
    fun setup() {
        deviceManager = DeviceManagerImpl(context, oneLoginApiService)
    }

    @Test
    fun testRegisterDeviceSuccess() {
        val registrationResponse = RegistrationResponse(
            true,
            "someCredentialId",
            "someMessage",
            "someSeed",
            "someUsername",
            "someSubdomain"
        )
        coEvery {
            oneLoginApiService.deviceRegistration(any(), any(), any())
        } returns registrationResponse

        val registerDevice = runBlocking {
            deviceManager.registerDevice("someCode", "someIssuer", "someShard")
        }

        coVerify(exactly = 1) {
            oneLoginApiService.deviceRegistration("someCode", "someIssuer", "someShard")
        }
        assertEquals(registrationResponse, registerDevice)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterDeviceHttpException() {

        coEvery {
            oneLoginApiService.deviceRegistration(any(), any(), any())
        } throws HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                deviceManager.registerDevice("someCode", "someIssuer", "someShard")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Unexpected parameter", exception.message)
            coVerify(exactly = 1) {
                oneLoginApiService.deviceRegistration("someCode", "someIssuer", "someShard")
            }
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterDeviceRuntimeException() {
        val exception = RuntimeException("Test exception")

        coEvery { oneLoginApiService.deviceRegistration(any(), any(), any()) } throws exception

        try {
            runBlocking {
                deviceManager.registerDevice("someCode", "someIssuer", "someShard")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Test exception", exception.message)
            coVerify(exactly = 1) {
                oneLoginApiService.deviceRegistration("someCode", "someIssuer", "someShard")
            }
            throw exception
        }
    }

    @Test
    fun testCheckDeviceSettingsSuccess() {
        val settingsResponse = SettingsResponse(
            isSuccess = true,
            unpaired = false,
            settings = Settings(
                disallowJailbrokenOrRooted = true,
                forceLockProtection = true,
                disableBackup = true,
                biometricVerification = true
            )
        )

        coEvery { oneLoginApiService.deviceSettings(any(), any()) } returns settingsResponse

        val deviceSettings = runBlocking {
            deviceManager.checkDeviceSettings(testFactor)
        }

        coVerify(exactly = 1) {
            oneLoginApiService.deviceSettings(testFactor.credentialId!!, testFactor.shard!!)
        }
        assertEquals(settingsResponse, deviceSettings)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testCheckDeviceSettingsHttpException() {
        coEvery {
            oneLoginApiService.deviceSettings(any(), any())
        } throws HttpException(Response.error<Any>(422, errorBody))

        try {
            runBlocking {
                deviceManager.checkDeviceSettings(testFactor)
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Unexpected parameter", exception.message)
            coVerify(exactly = 1) {
                oneLoginApiService.deviceSettings(testFactor.credentialId!!, testFactor.shard!!)
            }
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testCheckDeviceSettingsRuntimeException() {
        val exception = RuntimeException("Test exception")

        coEvery {
            oneLoginApiService.deviceSettings(any(), any())
        } throws exception

        try {
            runBlocking {
                deviceManager.checkDeviceSettings(testFactor)
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Test exception", exception.message)
            coVerify(exactly = 1) {
                oneLoginApiService.deviceSettings(testFactor.credentialId!!, testFactor.shard!!)
            }
            throw exception
        }
    }

    @Test
    fun testIsDeviceSecureTrue() {
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManager
        every { keyguardManager.isKeyguardSecure } returns true

        val isDeviceSecure = deviceManager.isDeviceSecure()

        assertEquals(true, isDeviceSecure)
    }

    @Test
    fun testIsDeviceSecureFalse() {
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguardManager
        every { keyguardManager.isKeyguardSecure } returns false

        val isDeviceSecure = deviceManager.isDeviceSecure()

        assertEquals(false, isDeviceSecure)
    }
}
