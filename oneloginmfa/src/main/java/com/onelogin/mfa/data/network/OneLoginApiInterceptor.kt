package com.onelogin.mfa.data.network

import okhttp3.Interceptor
import okhttp3.Response

internal class OneLoginApiInterceptor(
    private val isSendOTP: Boolean = false
): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val url = request.url
        var shard = request.url.queryParameter("shard")
        var host = request.url.queryParameter("host")

        if (host == null) {
            if (shard == null)
                shard = "01"

            val subdomain = when (shard) {
                "01" -> if (isSendOTP) "sso" else "api"
                "02" -> if (isSendOTP) "portal-eu" else "api-eu"
                "03" -> if (isSendOTP) "sso-de" else "api-de"
                else -> "sso$shard"
            }

            host = "$subdomain.onelogin.com"
        }

        val newUrl = url.newBuilder()
            .removeAllQueryParameters("shard")
            .removeAllQueryParameters("host")
            .host(host)
            .build()

        request = request.newBuilder()
            .url(newUrl)
            .addHeader("User-Agent", NetworkUtils.userAgentString)
            .build()

        var response =  chain.proceed(request)

        var retries = 0
        while (!response.isSuccessful && retries < 5) {
            retries++
            response.close()
            response = chain.proceed(request)
        }

        return response
    }
}