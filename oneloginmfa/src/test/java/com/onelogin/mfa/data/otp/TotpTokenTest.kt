package com.onelogin.mfa.data.otp

import org.junit.Assert
import org.junit.Test

class TotpTokenTest {

    @Test
    fun testGenerateOTP() {
        val otp = TotpToken("testSeed", getTime = { 0 }).generateOtp()
        // 298837 is the expected OTP for the given seed (testSeed) in the period 0
        Assert.assertEquals("298837", otp)
    }

    @Test
    fun testGenerateOTPEmptySeed() {
        val otp = TotpToken("", getTime = { 0 }).generateOtp()
        Assert.assertEquals("", otp)
    }

    @Test
    fun testGetTimerPeriodStart() {
        // 1350000 is 45 times the default period (30 seconds)
        val timer = TotpToken("testSeed", getTime = { 1350000 }).getTimer()
        Assert.assertEquals(30, timer)
    }

    @Test
    fun testGetTimerPeriodMiddle() {
        // 1350000 is 45 times the default period (30 seconds) plus 15 seconds
        val timer = TotpToken("testSeed", getTime = { 1365000 }).getTimer()
        Assert.assertEquals(15, timer)
    }

    @Test
    fun testGetTimerNonDefaultPeriod() {
        // 1800000 is 45 times the custom period (43 seconds)
        val timer = TotpToken("testSeed", period = 43, getTime = { 1935000 }).getTimer()
        Assert.assertEquals(43, timer)
    }

    @Test
    fun testGetTimerInMillis() {
        // 1350000 is 45 times the default period (30 seconds)
        val timer = TotpToken("testSeed", getTime = { 1350000 }).getTimerInMillis()
        Assert.assertEquals(30000, timer)
    }
}
