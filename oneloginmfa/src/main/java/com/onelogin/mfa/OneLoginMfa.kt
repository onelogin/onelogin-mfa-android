package com.onelogin.mfa

import android.content.Context
import timber.log.Timber

object OneLoginMfa {

    private var client: MfaClient? = null

    @JvmStatic
    fun initialize(context: Context, configuration: MfaConfiguration) {
        if (configuration.debug) {
            Timber.plant(Timber.DebugTree())
        }
        client = MfaClientFactory(context).build()
        Timber.d("Initialize OneLogin MFA client")
    }

    @JvmStatic
    fun getClient(): MfaClient = requireNotNull(client) {
        "You should call OneLoginMfa.initialize() before accessing the client"
    }
}
