package com.onelogin.mfa.appjava;

import android.app.Application;

import com.onelogin.mfa.MfaConfiguration;
import com.onelogin.mfa.OneLoginMfa;

public class MfaJavaDemoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MfaConfiguration mfaConfig = new MfaConfiguration.Builder().isDebug(true).build();
        OneLoginMfa.initialize(this, mfaConfig);
    }
}
