package com.onelogin.mfa.appkotlin

import android.app.Application
import com.onelogin.mfa.MfaConfiguration
import com.onelogin.mfa.OneLoginMfa

class MfaKotlinDemoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        OneLoginMfa.initialize(
            this,
            MfaConfiguration.Builder().isDebug(true).build()
        )
    }
}