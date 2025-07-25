package com.fonrouge.androidlib.commonServices

import android.util.Log
import com.fonrouge.fsLib.commonServices.IApiCommonService
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeoutException

/**
 * Serializes the given value to a JSON string using the Kotlinx Serialization library.
 *
 * @param value The value to be serialized.
 * @return The JSON string representation of the serialized value.
 */
inline fun <reified PAR> serialize(value: PAR): String {
    return Json.encodeToString(value)
}

/**
 * Generates a URL string for the current `IApiService` implementation and the calling function.
 *
 * @throws Exception if the `IApiService` implementation class name cannot be retrieved.
 *
 * @return the URL string for the API service and calling function.
 */
fun <A : IApiCommonService> A.urlString(): String =
    this::class.simpleName?.let { apiServiceName ->
        val callerFuncName = Thread.currentThread().stackTrace[3].methodName
        "${AppApi.urlBase}/${AppApi.appRoute}/$apiServiceName/${callerFuncName}"
    } ?: throw Exception("Error")

/**
 * Makes a remote API call using the provided `IApiService` implementation and returns the result.
 *
 * @return The result of the API call.
 *
 * @throws ClientRequestException if there is an error with the client request.
 * @throws ServerResponseException if there is an error with the server response.
 * @throws TimeoutException if there is a timeout during the API call.
 * @throws ConnectTimeoutException if there is a connect timeout during the API call.
 * @throws Exception if there is any other error during the API call.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified RET : Any> A.call(): RET {
    return remoteCall(emptyList())
}

/**
 * Calls the remote API endpoint using the specified parameters and returns the result.
 *
 * @param p1 The parameter to be serialized and sent to the remote API.
 * @return The result of the API call, after deserialization.
 * @throws Exception if an error occurs during the API call or deserialization.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified PAR1, reified RET : Any> A.call(p1: PAR1): RET {
    val s1 = serialize(p1)
    return remoteCall(listOf(s1))
}

/**
 * Performs a remote API call with two parameters and returns the result.
 *
 * @param p1 The first parameter to be passed to the API call.
 * @param p2 The second parameter to be passed to the API call.
 * @return The result of the API call.
 *
 * @throws ClientRequestException If there is an exception during the client request.
 * @throws ServerResponseException If there is an exception during the server response.
 * @throws TimeoutException If there is a timeout during the API call.
 * @throws ConnectTimeoutException If there is a connection timeout during the API call.
 * @throws Exception If there is any other exception during the API call.
 *
 * @see IApiCommonService
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified PAR1, reified PAR2, reified RET : Any> A.call(
    p1: PAR1,
    p2: PAR2,
): RET {
    val s1 = serialize(p1)
    val s2 = serialize(p2)
    return remoteCall(listOf(s1, s2))
}

/**
 * Executes a remote API call with three parameters and returns the result.
 *
 * @param p1 The first parameter to be passed to the API call.
 * @param p2 The second parameter to be passed to the API call.
 * @param p3 The third parameter to be passed to the API call.
 * @return The result of the API call.
 * @throws ClientRequestException When a client request exception occurs.
 * @throws ServerResponseException When a server response exception occurs.
 * @throws TimeoutException When a timeout exception occurs.
 * @throws ConnectTimeoutException When a connection timeout exception occurs.
 * @throws Exception When any other exception occurs.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified PAR1, reified PAR2, reified PAR3, reified RET : Any> A.call(
    p1: PAR1,
    p2: PAR2,
    p3: PAR3,
): RET {
    val s1 = serialize(p1)
    val s2 = serialize(p2)
    val s3 = serialize(p3)
    return remoteCall(listOf(s1, s2, s3))
}

/**
 * Calls a remote API using the provided `IApiService` implementation and returns the result.
 *
 * @param p1 The first parameter to be serialized and sent to the remote API.
 * @param p2 The second parameter to be serialized and sent to the remote API.
 * @param p3 The third parameter to be serialized and sent to the remote API.
 * @param p4 The fourth parameter to be serialized and sent to the remote API.
 *
 * @return The result of the API call, after deserialization.
 *
 * @throws ClientRequestException if there is an error with the client request.
 * @throws ServerResponseException if there is an error with the server response.
 * @throws TimeoutException if there is a timeout during the API call.
 * @throws ConnectTimeoutException if there is a connect timeout during the API call.
 * @throws Exception if there is any other error during the API call.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified PAR1, reified PAR2, reified PAR3, reified PAR4, reified RET : Any> A.call(
    p1: PAR1,
    p2: PAR2,
    p3: PAR3,
    p4: PAR4,
): RET {
    val s1 = serialize(p1)
    val s2 = serialize(p2)
    val s3 = serialize(p3)
    val s4 = serialize(p4)
    return remoteCall(listOf(s1, s2, s3, s4))
}

/**
 * Makes a remote API call using the provided `IApiService` implementation and returns the result.
 *
 * @param p1 The first parameter to be serialized and sent to the remote API.
 * @param p2 The second parameter to be serialized and sent to the remote API.
 * @param p3 The third parameter to be serialized and sent to the remote API.
 * @param p4 The fourth parameter to be serialized and sent to the remote API.
 * @param p5 The fifth parameter to be serialized and sent to the remote API.
 * @return The result of the API call, after deserialization.
 *
 * @throws ClientRequestException if there is an error with the client request.
 * @throws ServerResponseException if there is an error with the server response.
 * @throws TimeoutException if there is a timeout during the API call.
 * @throws ConnectTimeoutException if there is a connect timeout during the API call.
 * @throws Exception if there is any other error during the API call.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified PAR1, reified PAR2, reified PAR3, reified PAR4, reified PAR5, reified RET : Any> A.call(
    p1: PAR1,
    p2: PAR2,
    p3: PAR3,
    p4: PAR4,
    p5: PAR5,
): RET {
    val s1 = serialize(p1)
    val s2 = serialize(p2)
    val s3 = serialize(p3)
    val s4 = serialize(p4)
    val s5 = serialize(p5)
    return remoteCall(listOf(s1, s2, s3, s4, s5))
}

/**
 * Makes a remote API call using the provided `IApiService` implementation and returns the result.
 *
 * @param params The list of parameters to be serialized and sent to the remote API.
 * @return The result of the API call, after deserialization.
 *
 * @throws ClientRequestException if there is an error with the client request.
 * @throws ServerResponseException if there is an error with the server response.
 * @throws TimeoutException if there is a timeout during the API call.
 * @throws ConnectTimeoutException if there is a connect timeout during the API call.
 * @throws Exception if there is any other error during the API call.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified RET : Any> A.remoteCall(params: List<String?>): RET {
    val urlString = urlString()
    val tag = "HttpClient"
    val response = try {
        Log.d("API CALL Url", urlString)
        Log.d("API CALL Type", "${RET::class.simpleName}")
        AppApi.client.post(urlString) {
            contentType(ContentType.Application.Json)
            setBody(params)
        }
    } catch (e: ClientRequestException) {
        Log.d(tag, "ClientRequestException ${e.message}")
        e.printStackTrace()
        throw e
    } catch (e: ServerResponseException) {
        Log.d(tag, "ServerResponseException ${e.message}")
        e.printStackTrace()
        throw e
    } catch (e: TimeoutException) {
        Log.d(tag, "TimeoutException ${e.message}")
        e.printStackTrace()
        throw e
    } catch (e: ConnectTimeoutException) {
        Log.d(tag, "ConnectTimeoutException ${e.message}")
        e.printStackTrace()
        throw e
    } catch (e: Exception) {
        Log.d(tag, "Url: $urlString , error: ${e.message}")
        e.printStackTrace()
        throw e
    }
    if (response.status.isSuccess()) {
        val item: RET = try {
            response.body()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        return item
    }
    throw Exception(response.status.toString())
}
