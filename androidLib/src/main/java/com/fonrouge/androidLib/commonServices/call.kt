package com.fonrouge.androidLib.commonServices

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ConnectTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Builds the URL for a legacy API call using the service class name and an explicit route.
 * @deprecated Use JSON-RPC route discovery via [RouteRegistry] instead.
 */
fun <A : Any> A.urlString(route: String): String =
    this::class.simpleName?.let { apiServiceName ->
        "${appApi.urlBase}/${appApi.appRoute}/$apiServiceName/$route"
    } ?: throw Exception("Error")

@PublishedApi
internal val jsonRpcCounter = AtomicInteger(1)

/**
 * Makes a remote API call with no parameters.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified RET : Any> A.call(
    functionName: String,
): RET = remoteCall(functionName, emptyList())

/**
 * Makes a remote API call with one parameter.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified P1, reified RET : Any> A.call(
    functionName: String,
    p1: P1,
): RET = remoteCall(functionName, listOf(p1?.let { Json.encodeToString<P1>(it) }))

/**
 * Makes a remote API call with two parameters.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified P1, reified P2, reified RET : Any> A.call(
    functionName: String,
    p1: P1,
    p2: P2,
): RET = remoteCall(functionName, listOf(
    p1?.let { Json.encodeToString<P1>(it) },
    p2?.let { Json.encodeToString<P2>(it) },
))

/**
 * Makes a remote API call with three parameters.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified P1, reified P2, reified P3, reified RET : Any> A.call(
    functionName: String,
    p1: P1,
    p2: P2,
    p3: P3,
): RET = remoteCall(functionName, listOf(
    p1?.let { Json.encodeToString<P1>(it) },
    p2?.let { Json.encodeToString<P2>(it) },
    p3?.let { Json.encodeToString<P3>(it) },
))

/**
 * Makes a JSON-RPC 2.0 remote call using discovered routes from [RouteRegistry].
 */
@Suppress("unused")
suspend inline fun <A : Any, reified RET : Any> A.remoteCall(
    functionName: String,
    params: List<String?>,
): RET {
    val serviceName = this::class.simpleName
        ?: throw Exception("Cannot determine service name")
    val resolvedRoute = RouteRegistry.getRoute(serviceName, functionName)
    val url = "${appApi.urlBase}$resolvedRoute"

    val rpcRequest = JsonRpcRequest(
        id = jsonRpcCounter.getAndIncrement(),
        method = resolvedRoute,
        params = params,
    )

    val tag = "HttpClient"

    if (appApi.delayBeforeRequest > 0) {
        delay(appApi.delayBeforeRequest.toLong())
    }

    val response = try {
        Log.d("API CALL", "$serviceName.$functionName → $url")
        Log.d("API CALL Type", "${RET::class.simpleName}")
        appApi.client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(rpcRequest)
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
        Log.d(tag, "Url: $url , error: ${e.message}")
        e.printStackTrace()
        throw e
    }

    val rpcResponse: JsonRpcResponse = try {
        response.body()
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }

    if (rpcResponse.error != null) {
        throw RpcException(
            message = rpcResponse.error,
            exceptionType = rpcResponse.exceptionType,
            exceptionJson = rpcResponse.exceptionJson,
        )
    }

    return Json.decodeFromString(
        rpcResponse.result ?: throw Exception("Empty result from JSON-RPC response")
    )
}
