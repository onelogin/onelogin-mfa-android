package com.onelogin.mfa.data.network

import com.onelogin.mfa.data.network.NetworkUtils.apiCall
import com.onelogin.mfa.data.network.NetworkUtils.ErrorResponse
import com.onelogin.mfa.data.network.NetworkResponse.Success
import com.onelogin.mfa.data.network.NetworkResponse.NetworkException
import com.onelogin.mfa.data.network.NetworkResponse.GenericException
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@ExperimentalCoroutinesApi
class NetworkUtilsTest {

    private val dispatcher = TestCoroutineDispatcher()

    @Test
    fun testApiCallSuccess() {
        runBlocking {
            val result = apiCall(dispatcher) { true }
            assertEquals(Success(true), result)
        }
    }

    @Test
    fun testApiCallHttpException() {
        runBlocking {
            val errorBody = "{\"error\": \"Unexpected parameter\"}".toResponseBody("application/json".toMediaTypeOrNull())
            val result = apiCall(dispatcher) { throw HttpException(Response.error<Any>(422, errorBody)) }
            val expectedResult = NetworkException(422, ErrorResponse("Unexpected parameter"))
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun testApiCallHttpExceptionEmptyBody() {
        runBlocking {
            val emptyErrorBody = "".toResponseBody("application/json".toMediaTypeOrNull())
            val result = apiCall(dispatcher) { throw HttpException(Response.error<Any>(422, emptyErrorBody)) }
            val expectedResult = NetworkException(422, null)
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun testApiCallIllegalStateException() {
        runBlocking {
            val testException = IllegalStateException("Test")
            val result = apiCall(dispatcher) { throw testException }
            val expectedResult = GenericException(testException)
            assertEquals(expectedResult, result)
        }
    }
}
