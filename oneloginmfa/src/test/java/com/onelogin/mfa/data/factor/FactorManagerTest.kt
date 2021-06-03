package com.onelogin.mfa.data.factor

import com.onelogin.mfa.data.OneLoginMfaException
import com.onelogin.mfa.data.api.*
import com.onelogin.mfa.data.device.DeviceManager
import com.onelogin.mfa.data.encryption.EncryptionManager
import com.onelogin.mfa.data.repository.MfaRepository
import com.onelogin.mfa.model.Factor
import io.mockk.*
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class FactorManagerTest {

    private val repository = mockk<MfaRepository>()
    private val encryptionManager = mockk<EncryptionManager>()
    private val deviceManager = mockk<DeviceManager>()
    private val subdomainApi = mockk<SubdomainApiService>()

    private lateinit var factorManager: FactorManager
    private lateinit var webLoginHelper: WebLoginHelper

    private val oneLoginCodePlainShort = "10-1234567"
    private val oneLoginCodePlainComplex = "10-112345678901234AB"
    private val oneLoginUrl = "otpauth://protect.onelogin.com?code=10-1234567&issuer=OneLogin"

    private val totpUrl = "otpauth://totp/SomeOtherCompany:myemail@email.com?secret=2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D&issuer=SomeOtherCompany"

    private val oneLoginTestFactor = Factor(
        id = 0,
        credentialId = "someCredentialId",
        subdomain = "someSubdomain",
        shard = "10",
        username = "someUsername",
        seed = "someSeed",
        issuer = "OneLogin",
        creationDate = 0,
        allowRoot = false,
        forceLock = true,
        paired = true,
        displayName = "someSubdomain",
        orderPreference = 0,
        crypto = "HmacSHA1",
        period = 30,
        digits = 6,
        allowBackup = true,
        requireBiometrics = true
    )

    private val thirdPartyTestFactor = Factor(
        id = 0,
        credentialId = "",
        subdomain = "",
        shard = "",
        username = "myemail@email.com",
        seed = "someSeed",
        issuer = "SomeOtherCompany",
        creationDate = 0,
        allowRoot = true,
        forceLock = false,
        paired = true,
        displayName = "",
        orderPreference = 0,
        crypto = "HmacSHA1",
        period = 30,
        digits = 6,
        allowBackup = true,
        requireBiometrics = false
    )

    private val registrationResponse = RegistrationResponse(
        true,
        "someCredentialId",
        "someMessage",
        "someSeed",
        "someUsername",
        "someSubdomain"
    )

    private val settingsResponse = SettingsResponse(
        isSuccess = true,
        unpaired = false,
        settings = Settings(
            disallowJailbrokenOrRooted = true,
            forceLockProtection = true,
            disableBackup = true,
            biometricVerification = true
        )
    )

    @Before
    fun setup() {
        factorManager = FactorManagerImpl(repository, encryptionManager, deviceManager, subdomainApi)
        webLoginHelper = WebLoginHelper(subdomainApi)
    }

    // registerFactor()

    @Test
    fun testRegisterFactorOneLoginPlainShortEncryptionEnabledSuccess() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "encryptedSeed"

        val result = runBlocking {
            factorManager.registerFactor(oneLoginCodePlainShort)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-1234567", "OneLogin", "10") }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor.copy(seed = "encryptedSeed")) }
        verify(exactly = 1) { encryptionManager.encrypt("someSeed") }

        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorOneLoginPlainShortEncryptionDisabledSuccess() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.registerFactor(oneLoginCodePlainShort)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-1234567", "OneLogin", "10") }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor) }
        verify(exactly = 0) { encryptionManager.encrypt("someSeed") }

        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorOneLoginPlainComplexEncryptionEnabledSuccess() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "encryptedSeed"

        val result = runBlocking {
            factorManager.registerFactor(oneLoginCodePlainComplex)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-112345678901234AB", "OneLogin", "10") }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor.copy(seed = "encryptedSeed")) }
        verify(exactly = 1) { encryptionManager.encrypt("someSeed") }

        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorOneLoginPlainComplexEncryptionDisabledSuccess() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.registerFactor(oneLoginCodePlainComplex)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-112345678901234AB", "OneLogin", "10") }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor) }
        verify(exactly = 0) { encryptionManager.encrypt("someSeed") }

        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorOneLoginUrlEncryptionEnabledSuccess() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "encryptedSeed"

        val result = runBlocking {
            factorManager.registerFactor(oneLoginUrl)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-1234567", "OneLogin", "10") }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor.copy(seed = "encryptedSeed")) }
        verify(exactly = 1) { encryptionManager.encrypt("someSeed") }

        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorOneLoginUrlEncryptionDisabledSuccess() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.registerFactor(oneLoginUrl)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-1234567", "OneLogin", "10") }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor) }
        verify(exactly = 0) { encryptionManager.encrypt("someSeed") }

        assertEquals(1, result)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorOneLoginEmptySeed() {
        runBlocking {
            factorManager.registerFactor("")
        }

        coVerify { deviceManager.registerDevice(any(), any(), any()) }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorOneLoginEmptyShard() {
        val emptyShard = "otpauth://protect.onelogin.com?issuer=OneLogin"

        runBlocking {
            factorManager.registerFactor(emptyShard)
        }

        coVerify(exactly = 0) { deviceManager.registerDevice(any(), any(), any()) }
    }

    @Test
    fun testRegisterFactorThirdPartyEncryptionEnabledSuccess() {
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.registerFactor(totpUrl)
        }

        coVerify(exactly = 1) { repository.addFactor(thirdPartyTestFactor.copy(seed = "decryptedSeed")) }
        verify(exactly = 1) { encryptionManager.encrypt("2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D") }
        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorThirdPartyEncryptionDisabledSuccess() {
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.registerFactor(totpUrl)
        }

        coVerify(exactly = 1) {
            repository.addFactor(thirdPartyTestFactor.copy(seed = "2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D"))
        }
        verify(exactly = 0) { encryptionManager.encrypt("2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D") }

        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorThirdPartyEmptyEncryptionEnabledEmptySeedSuccess() {
        val emptySeed = "otpauth://totp/SomeOtherCompany:myemail@email.com?&issuer=SomeOtherCompany"
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.registerFactor(emptySeed)
        }

        coVerify(exactly = 1) {
            repository.addFactor(thirdPartyTestFactor.copy(username = "" ,seed = "decryptedSeed", issuer = ""))
        }
        verify(exactly = 1) { encryptionManager.encrypt(emptySeed) }
        assertEquals(1, result)
    }

    @Test
    fun testRegisterFactorThirdPartyEmptyEncryptionDisabledEmptySeedSuccess() {
        val emptySeed = "otpauth://totp/SomeOtherCompany:myemail@email.com?&issuer=SomeOtherCompany"
        coEvery { repository.addFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.registerFactor(emptySeed)
        }

        coVerify(exactly = 1) {
            repository.addFactor(thirdPartyTestFactor.copy(username = "" ,seed = emptySeed, issuer = ""))
        }
        verify(exactly = 0) { encryptionManager.encrypt(emptySeed) }
        assertEquals(1, result)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorThirdPartyInvalidCrypto() {
        val invalidCrypto = "otpauth://totp/SomeOtherCompany:myemail@email.com?secret=2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D&algorithm=BadAlgorithm&issuer=SomeOtherCompany"

        try {
            runBlocking {
                factorManager.registerFactor(invalidCrypto)
            }
        } catch(exception: OneLoginMfaException) {
            assertEquals("Crypto algorithm not supported", exception.message)
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorEmptyCode() {
        try {
            runBlocking {
                factorManager.registerFactor("")
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Invalid code format", exception.message)
            throw exception
        }
    }

    @Test
    fun testRegisterFactorEncryptionError() {
        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        coEvery { repository.addFactor(any()) } returns -1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } throws RuntimeException()


        val result = runBlocking {
            factorManager.registerFactor(oneLoginUrl)
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-1234567", "OneLogin", "10") }
        coVerify(exactly = 0) { repository.addFactor(oneLoginTestFactor) }

        assertEquals(-1, result)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorRegistrationError() {
        val registrationException = OneLoginMfaException("Registration Error")

        coEvery { deviceManager.registerDevice(any(), any(), any()) } throws registrationException

        try {
            runBlocking {
                factorManager.registerFactor(oneLoginUrl)
            }
        } catch (exception: OneLoginMfaException) {
            assertEquals("Registration Error", exception.message)
            throw exception
        }
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorRetrieveSettingsError() {
        val settingsException = OneLoginMfaException("Settings Error")

        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } throws settingsException

        try {
            runBlocking {
                factorManager.registerFactor(oneLoginUrl)
            }
        } catch (exception: OneLoginMfaException) {
            coVerify(exactly = 1) { deviceManager.registerDevice("10-1234567", "OneLogin", "10") }
            assertEquals("Settings Error", exception.message)
            throw exception
        }
    }

    // registerFactorByWebLogin()

    @Test
    fun testRegisterFactorByWebLoginSuccess() {
        val domainAvailableResponse = DomainAvailableResponse(listOf(DomainAvailableData(false)))
        val availableFactors = listOf(AvailableFactors(123, 8, "OneLogin Protect", "someDescription"))
        val factorToken = GetMfaTokenResponse("someTokenId", "someStatus", oneLoginCodePlainComplex)
        val accessServiceResponse = AccessServiceResponse(
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

        coEvery { deviceManager.registerDevice(any(), any(), any()) } returns registrationResponse
        coEvery { deviceManager.checkDeviceSettings(any()) } returns settingsResponse
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "decryptedSeed"
        coEvery { repository.addFactor(any()) } returns 1

        coEvery { subdomainApi.isDomainAvailable(any()) } returns domainAvailableResponse
        coEvery { subdomainApi.getInitialAuthorization(any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadUsername(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.uploadPassword(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaRegistrationNotice(any(), any(), any(), any()) } returns accessServiceResponse
        coEvery { subdomainApi.getMfaAuthorization(any(), any()) } returns accessServiceResponse.mfa!!
        coEvery { subdomainApi.getAvailableFactors(any(), any()) } returns availableFactors
        coEvery { subdomainApi.getFactorToken(any(), any(), any()) } returns factorToken

        val result = runBlocking {
            factorManager.registerFactorByWebLogin("mySubdomain", "myUsername", "myPassword")
        }

        coVerify(exactly = 1) { deviceManager.registerDevice("10-112345678901234AB", "OneLogin", "10") }
        coVerify(exactly = 1) { deviceManager.checkDeviceSettings(oneLoginTestFactor.copy(seed = "decryptedSeed")) }
        coVerify(exactly = 1) { repository.addFactor(oneLoginTestFactor.copy(seed = "decryptedSeed")) }

        assertEquals(1, result)
    }

    @Test(expected = OneLoginMfaException::class)
    fun testRegisterFactorByWebLoginError() {
        coEvery { subdomainApi.isDomainAvailable(any()) } throws RuntimeException("Subdomain error")

        try {
            runBlocking {
                factorManager.registerFactorByWebLogin("mySubdomain", "myUsername", "myPassword")
            }
        } catch (exception: Exception) {
            assertEquals("Subdomain error", exception.message)
            throw exception
        }
    }

    // updateFactor()

    @Test
    fun testUpdateFactorSuccessEncryptionEnabled() {
        coEvery { repository.updateFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } returns "someSeed"

        val result = runBlocking {
            factorManager.updateFactor(oneLoginTestFactor)
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.encrypt(oneLoginTestFactor.seed) }
        coVerify(exactly = 1) { repository.updateFactor(oneLoginTestFactor) }

        assertEquals(1, result)
    }

    @Test
    fun testUpdateFactorSuccessEncryptionDisabled() {
        coEvery { repository.updateFactor(any()) } returns 1
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.updateFactor(oneLoginTestFactor)
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.encrypt(oneLoginTestFactor.seed) }
        coVerify(exactly = 1) { repository.updateFactor(oneLoginTestFactor) }

        assertEquals(1, result)
    }

    @Test
    fun testUpdateFactorEncryptionError() {
        coEvery { repository.updateFactor(any()) } returns -1
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.encrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.updateFactor(oneLoginTestFactor)
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.encrypt(oneLoginTestFactor.seed) }
        coVerify(exactly = 0) { repository.updateFactor(oneLoginTestFactor) }

        assertEquals(-1, result)
    }

    // getFactors()

    @Test
    fun testGetFactorsSuccessEncryptionEnabled() {
        val factorList = listOf(oneLoginTestFactor)
        coEvery { repository.getAllFactors() } returns factorList
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactors()
        }

        verify(exactly = 2) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getAllFactors() }

        assertEquals(listOf(oneLoginTestFactor.copy(seed = "decryptedSeed")), result)
    }

    @Test
    fun testGetFactorsSuccessEncryptionDisabled() {
        val factorList = listOf(oneLoginTestFactor)
        coEvery { repository.getAllFactors() } returns factorList
        every { encryptionManager.isSupported() } returns false

        val result = runBlocking {
            factorManager.getFactors()
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getAllFactors() }

        assertEquals(factorList, result)
    }

    @Test
    fun testGetFactorsEncryptionError() {
        val factorList = listOf(oneLoginTestFactor)
        coEvery { repository.getAllFactors() } returns factorList
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.getFactors()
        }

        verify(exactly = 2) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getAllFactors() }

        assertEquals(emptyList<Factor>(), result)
    }

    @Test
    fun testGetFactorsEmptyList() {
        coEvery { repository.getAllFactors() } returns emptyList()
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactors()
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getAllFactors() }

        assertEquals(emptyList<Factor>(), result)
    }

    // getFactorsByIssuer()

    @Test
    fun testGetFactorsByIssuerSuccessEncryptionEnabled() {
        val factorList = listOf(oneLoginTestFactor)

        coEvery { repository.getFactorsByIssuer(any()) } returns factorList
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorsByIssuer("someIssuer")
        }

        verify(exactly = 2) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorsByIssuer("someIssuer") }

        assertEquals(listOf(oneLoginTestFactor.copy(seed = "decryptedSeed")), result)
    }

    @Test
    fun testGetFactorsByIssuerSuccessEncryptionDisabled() {
        val factorList = listOf(oneLoginTestFactor)

        coEvery { repository.getFactorsByIssuer(any()) } returns factorList
        every { encryptionManager.isSupported() } returns false
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorsByIssuer("someIssuer")
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorsByIssuer("someIssuer") }

        assertEquals(factorList, result)
    }

    @Test
    fun testGetFactorsByIssuerEncryptionError() {
        val factorList = listOf(oneLoginTestFactor)

        coEvery { repository.getFactorsByIssuer(any()) } returns factorList
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.getFactorsByIssuer("someIssuer")
        }

        verify(exactly = 2) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorsByIssuer("someIssuer") }

        assertEquals(emptyList<Factor>(), result)
    }

    @Test
    fun testGetFactorsByIssuerEmptyList() {
        coEvery { repository.getFactorsByIssuer(any()) } returns emptyList()
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorsByIssuer("someIssuer")
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorsByIssuer("someIssuer") }

        assertEquals(emptyList<Factor>(), result)
    }

    // getFactorById()

    @Test
    fun testGetFactorByIdSuccessEncryptionEnabled() {
        coEvery { repository.getFactorById(any()) } returns oneLoginTestFactor
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorById(123456)
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorById(123456) }

        assertEquals(oneLoginTestFactor.copy(seed = "decryptedSeed"), result)
    }

    @Test
    fun testGetFactorsByIdSuccessEncryptionDisabled() {
        coEvery { repository.getFactorById(any()) } returns oneLoginTestFactor
        every { encryptionManager.isSupported() } returns false
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorById(123456)
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorById(123456) }

        assertEquals(oneLoginTestFactor, result)
    }

    @Test
    fun testGetFactorsByIdEncryptionError() {
        coEvery { repository.getFactorById(any()) } returns oneLoginTestFactor
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.getFactorById(123456)
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorById(123456) }

        assertEquals(0L, result?.id)
        assertEquals("", result?.credentialId)
        assertEquals("", result?.seed)
    }

    @Test
    fun testGetFactorsByIdDoesNotExist() {
        coEvery { repository.getFactorById(any()) } returns null
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.getFactorById(123456)
        }

        verify(exactly = 0) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorById(123456) }
        assertEquals(null, result)
    }

    // getFactorByCredentialId()

    @Test
    fun testGetFactorByCredentialIdSuccessEncryptionEnabled() {
        coEvery { repository.getFactorByCredentialId(any()) } returns oneLoginTestFactor
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorByCredentialId("someCredentialId")
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorByCredentialId("someCredentialId") }
        assertEquals(oneLoginTestFactor.copy(seed = "decryptedSeed"), result)
    }

    @Test
    fun testGetFactorByCredentialIdSuccessEncryptionDisabled() {
        coEvery { repository.getFactorByCredentialId(any()) } returns oneLoginTestFactor
        every { encryptionManager.isSupported() } returns false
        every { encryptionManager.decrypt(any()) } returns "decryptedSeed"

        val result = runBlocking {
            factorManager.getFactorByCredentialId("someCredentialId")
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorByCredentialId("someCredentialId") }
        assertEquals(oneLoginTestFactor, result)
    }

    @Test
    fun testGetFactorByCredentialIdEncryptionError() {
        coEvery { repository.getFactorByCredentialId(any()) } returns oneLoginTestFactor
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.getFactorByCredentialId("someCredentialId")
        }

        verify(exactly = 1) { encryptionManager.isSupported() }
        verify(exactly = 1) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorByCredentialId("someCredentialId") }

        assertEquals(0L, result?.id)
        assertEquals("", result?.credentialId)
        assertEquals("", result?.seed)
    }

    @Test
    fun testGetFactorByCredentialIdDoesNotExist() {
        coEvery { repository.getFactorByCredentialId(any()) } returns null
        every { encryptionManager.isSupported() } returns true
        every { encryptionManager.decrypt(any()) } throws RuntimeException()

        val result = runBlocking {
            factorManager.getFactorByCredentialId("invalidId")
        }

        verify(exactly = 0) { encryptionManager.isSupported() }
        verify(exactly = 0) { encryptionManager.decrypt("someSeed") }
        coVerify(exactly = 1) { repository.getFactorByCredentialId("invalidId") }
        assertEquals(null, result)
    }

    // removeFactor()

    @Test
    fun testRemoveFactorSuccess() {
        coEvery { repository.deleteFactor(any()) } returns 1

        val result = runBlocking {
            factorManager.removeFactor(oneLoginTestFactor)
        }

        coVerify(exactly = 1) { repository.deleteFactor(oneLoginTestFactor) }
        assertEquals(1, result)
    }

    // removeAllFactors()

    @Test
    fun testRemoveAllFactorsSuccess() {
        coEvery { repository.deleteAllFactors() } returns 1

        val result = runBlocking {
            factorManager.removeAllFactors()
        }

        coVerify(exactly = 1) { repository.deleteAllFactors() }
        assertEquals(1, result)
    }

    // removeFactorById()

    @Test
    fun testRemoveFactorByIdSuccess() {
        coEvery { repository.deleteFactorById(any()) } returns 1

        val result = runBlocking {
            factorManager.removeFactorById(oneLoginTestFactor.id)
        }

        coVerify(exactly = 1) { repository.deleteFactorById(oneLoginTestFactor.id) }
        assertEquals(1, result)
    }

    // removeFactorByCredentialId()

    @Test
    fun testRemoveFactorByCredentialIdSuccess() {
        coEvery { repository.deleteFactorByCredentialId(any()) } returns 1

        val result = runBlocking {
            factorManager.removeFactorByCredentialId(oneLoginTestFactor.credentialId!!)
        }

        coVerify(exactly = 1) { repository.deleteFactorByCredentialId(oneLoginTestFactor.credentialId!!) }
        assertEquals(1, result)
    }

    @Test
    fun testRemoveFactorByCredentialIdEmptyInput() {
        coEvery { repository.deleteFactorByCredentialId(any()) } returns -1

        val result = runBlocking {
            factorManager.removeFactorByCredentialId("")
        }

        coVerify(exactly = 1) { repository.deleteFactorByCredentialId("") }
        assertEquals(-1, result)
    }
}
