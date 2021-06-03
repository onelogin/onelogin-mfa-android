package com.onelogin.mfa.data.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.onelogin.mfa.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.*
import kotlin.Exception

object NetworkUtils {

    internal suspend fun <T> apiCall(dispatcher: CoroutineDispatcher, apiCall: suspend () -> T): NetworkResponse<T> {
        return withContext(dispatcher) {
            try {
                NetworkResponse.Success(apiCall.invoke())
            } catch (throwable: Throwable) {
                when (throwable) {
                    is HttpException -> {
                        val errorResponse = convertErrorBody(throwable)
                        if (errorResponse == null) {
                            NetworkResponse.NetworkException(throwable.code(), ErrorResponse("Unable to parse HTTP response"))
                        }
                        NetworkResponse.NetworkException(throwable.code(), errorResponse)
                    }
                    else -> {
                        NetworkResponse.GenericException(throwable)
                    }
                }
            }
        }
    }

    internal val userAgentString: String = String.format(
        Locale.US, "OLAndroidMfaSdk/%s (%d) %s",
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
        System.getProperty("http.agent")
    )

    private fun convertErrorBody(throwable: HttpException): ErrorResponse? {
        return try {
            throwable.response()?.errorBody()?.let {
                Gson().fromJson(it.string(), ErrorResponse::class.java)
            }
        } catch (exception: Exception) {
            null
        }
    }

    data class ErrorResponse(
        @SerializedName("error")
        val error: String
    )
}
