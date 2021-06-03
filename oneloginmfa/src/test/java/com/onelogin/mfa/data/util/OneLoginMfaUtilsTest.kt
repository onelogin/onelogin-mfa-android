package com.onelogin.mfa.data.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class OneLoginMfaUtilsTest {

    private val oneLoginCodePlainShort = "10-1234567"
    private val oneLoginCodePlainComplex = "12-112345678901234AB"
    private val oneLoginUrl = "otpauth://protect.onelogin.com?code=13-1234568&issuer=OneLogin"
    private val totpUrl = "otpauth://totp?secret=2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D&issuer=SomeOtherCompany"

    // isOneLoginCode()

    @Test
    fun testIsOneLoginCodePlainShort() {
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(oneLoginCodePlainShort)

        assertEquals(true, isOneLogin)
    }

    @Test
    fun testIsOneLoginCodePlainShortNoDash() {
        val noDashCode = "123456789"
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(noDashCode)

        assertEquals(true, isOneLogin)
    }

    @Test
    fun testIsOneLoginCodePlainComplex() {
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(oneLoginCodePlainComplex)

        assertEquals(true, isOneLogin)
    }

    @Test
    fun testIsOneLoginCodePlainComplexNoDash() {
        val noDashCode = "12112345678901234AB"
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(noDashCode)

        assertEquals(true, isOneLogin)
    }

    @Test
    fun testIsOneLoginCodePlainInvalid() {
        val invalidCode = "13-215D"
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(invalidCode)

        assertEquals(false, isOneLogin)
    }

    @Test
    fun testIsOneLoginCodeWithOneLoginUrl() {
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(oneLoginUrl)

        assertEquals(true, isOneLogin)
    }

    @Test
    fun testIsOneLoginCodeWithTotpUrl() {
        val isOneLogin = OneLoginMfaUtils.isOneLoginCode(totpUrl)

        assertEquals(false, isOneLogin)
    }

    // isValidThirdPartyCode()

    @Test
    fun testIsValidThirdPartyCode() {
        val isValidThirdParty = OneLoginMfaUtils.isValidThirdPartyCode(totpUrl)

        assertEquals(true, isValidThirdParty)
    }

    @Test
    fun testIsValidThirdPartyCodeEmptyCode() {
        val isValidThirdParty = OneLoginMfaUtils.isValidThirdPartyCode("")

        assertEquals(false, isValidThirdParty)
    }

    @Test
    fun testIsValidThirdPartyCodeNull() {
        val isValidThirdParty = OneLoginMfaUtils.isValidThirdPartyCode(null)

        assertEquals(false, isValidThirdParty)
    }

    @Test
    fun testIsValidThirdPartyCodeEmptySecret() {
        val thirdPartyCodeEmptySecret = "otpauth://totp?issuer=SomeOtherCompany"
        val isValidThirdParty = OneLoginMfaUtils.isValidThirdPartyCode(thirdPartyCodeEmptySecret)

        assertEquals(false, isValidThirdParty)
    }

    // getOtpCode()

    @Test
    fun testGetOtpCodeValidOneLoginPlainShort() {
        val otpCode = OneLoginMfaUtils.getOtpCode(oneLoginCodePlainShort)

        assertEquals(oneLoginCodePlainShort, otpCode)
    }

    @Test
    fun testGetOtpCodeValidOneLoginPlainComplex() {
        val otpCode = OneLoginMfaUtils.getOtpCode(oneLoginCodePlainComplex)

        assertEquals(oneLoginCodePlainComplex, otpCode)
    }

    @Test
    fun testGetOtpCodeValidOneLoginUrl() {
        val otpCode = OneLoginMfaUtils.getOtpCode(oneLoginUrl)

        assertEquals("13-1234568", otpCode)
    }

    @Test
    fun testGetOtpCodeEmptyCodeOneLoginUrl() {
        val otpCode = OneLoginMfaUtils.getOtpCode("otpauth://notprotect.onelogin.com?issuer=OneLogin")

        assertEquals(null, otpCode)
    }

    @Test
    fun testGetOtpCodeEmptyIssuerOneLoginUrl() {
        val otpCode = OneLoginMfaUtils.getOtpCode("otpauth://notprotect.onelogin.com?issuer=OneLogin")

        assertEquals(null, otpCode)
    }

    @Test
    fun testGetOtpCodeNonOneLoginIssuerUrl() {
        val otpCode = OneLoginMfaUtils.getOtpCode("otpauth://notprotect.onelogin.com?issuer=TwoLogin")

        assertEquals(null, otpCode)
    }

    @Test
    fun testGetOtpCodeEmptyCodeString() {
        val otpCode = OneLoginMfaUtils.getOtpCode("")

        assertEquals(null, otpCode)
    }

    // getIssuer()

    @Test
    fun testGetIssuerValidOneLoginUrl() {
        val issuer = OneLoginMfaUtils.getIssuer(oneLoginUrl)

        assertEquals("OneLogin", issuer)
    }

    @Test
    fun testGetIssuerTotpUrl() {
        val issuer = OneLoginMfaUtils.getIssuer(totpUrl)

        assertEquals("SomeOtherCompany", issuer)
    }

    @Test
    fun testGetIssuerEmptyIssuer() {
        val emptyIssuerUrl = "otpauth://totp?secret=2DIMSYT3EDEOPLGTIUIWP7SRNVPNM73D"
        val issuer = OneLoginMfaUtils.getIssuer(emptyIssuerUrl)

        assertEquals("", issuer)
    }

    @Test
    fun testGetIssuerNullInput() {
        val issuer = OneLoginMfaUtils.getIssuer(null)

        assertEquals("OneLogin", issuer)
    }

    // Modify username for display

    @Test
    fun testModifyUserNameForDisplay() {
        val input = "bob@mac.com"
        val displayUsername = OneLoginMfaUtils.modifyUserNameForDisplay(input)

        assertEquals("bob@mac.com", displayUsername)
    }

    @Test
    fun testModifyUserNameForDisplayWithEscapes() {
        val input = "bob-o&#39;reilly@mac.com"
//        val expected = "bob-o'reilly@mac.com"
        val displayUsername = OneLoginMfaUtils.modifyUserNameForDisplay(input)

        assertEquals("bob-o'reilly@mac.com", displayUsername)
    }

    @Test
    fun testModifyUserNameForDisplayWithNull() {
        val displayUsername = OneLoginMfaUtils.modifyUserNameForDisplay(null)

        assertEquals(null, displayUsername)
    }

    @Test
    fun testModifyUserNameForDisplayWithEmptyString() {
        val displayUsername = OneLoginMfaUtils.modifyUserNameForDisplay("")

        assertEquals("", displayUsername)
    }
}
