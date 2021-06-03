package com.onelogin.mfa.data.network

import com.onelogin.mfa.data.api.SubdomainApiService.Companion.ACCESS_AUTH_ENDPOINT
import com.onelogin.mfa.data.api.SubdomainApiService.Companion.BRANDING_INFO_ENDPOINT
import com.onelogin.mfa.data.api.SubdomainApiService.Companion.DOMAIN_AVAILABLE_ENDPOINT
import okhttp3.Interceptor
import okhttp3.Response

internal class SubdomainApiInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val method = request.method

        val domain = url.queryParameter("domain")
        val jwt = url.queryParameter("jwt") ?: ""
        val newUrl = url.newBuilder()
                        .removeAllQueryParameters("domain")
                        .removeAllQueryParameters("jwt")
                        .host("$domain.onelogin.com")
                        .build()

        val newRequest = when {
            url.encodedPath.contains(
                    DOMAIN_AVAILABLE_ENDPOINT) ||
                    (url.encodedPath.contains(BRANDING_INFO_ENDPOINT)) ||
                    (method == "POST" && url.encodedPath.contains(ACCESS_AUTH_ENDPOINT))
            -> {
                request.newBuilder()
                    .url(newUrl)
                    .addHeader("User-Agent", NetworkUtils.userAgentString)
                    .build()
            } else -> {
                request.newBuilder()
                        .url(newUrl)
                        .addHeader("User-Agent", NetworkUtils.userAgentString)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer $jwt")
                        .build()
            }
        }
        return chain.proceed(newRequest)
    }
}
