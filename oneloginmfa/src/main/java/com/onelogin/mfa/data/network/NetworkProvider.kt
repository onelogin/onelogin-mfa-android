package com.onelogin.mfa.data.network

import android.content.Context
import com.onelogin.mfa.data.api.OneLoginApiService
import com.onelogin.mfa.data.api.SubdomainApiService
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal object NetworkProvider {

    internal fun getOneLoginApi(context: Context): OneLoginApiService {
        val builder = Retrofit.Builder()
            .client(
                getOkHttpClient(
                    context,
                    OneLoginApiInterceptor()
                )
            )
            .baseUrl("https://api.onelogin.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return builder.create(OneLoginApiService::class.java)
    }

    internal fun getSubdomainApi(context: Context): SubdomainApiService {
        val builder = Retrofit.Builder()
            .client(
                getOkHttpClient(
                    context,
                    SubdomainApiInterceptor()
                )
            )
            .baseUrl("https://onelogininc.onelogin.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return builder.create(SubdomainApiService::class.java)
    }

    private fun getOkHttpClient(context: Context, interceptor: Interceptor) = OkHttpClient.Builder()
            .cache(Cache(context.cacheDir, 10 * 1024 * 1024))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(false)
            .addInterceptor(interceptor)
            .build()
}
