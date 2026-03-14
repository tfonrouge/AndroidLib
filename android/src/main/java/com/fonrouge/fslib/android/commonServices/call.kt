package com.fonrouge.fslib.android.commonServices

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

/**
 * Interface for service proxies with an explicit service name.
 * Implement this to decouple the Android class name from the server interface name.
 */
interface IServiceProxy {
    val serviceName: String
}

/**
 * Builds the URL for a legacy API call using the service class name and an explicit route.
 * @deprecated Use JSON-RPC route discovery via [RouteRegistry] instead.
 */
@Deprecated("Use JSON-RPC route discovery via RouteRegistry instead")
fun <A : Any> A.urlString(route: String): String =
    this::class.simpleName?.let { apiServiceName ->
        "${appApi.urlBase}/${appApi.appRoute}/$apiServiceName/$route"
    } ?: throw Exception("Error")

@PublishedApi
internal val jsonRpcCounter = AtomicInteger(1)

/**
 * Extracts the calling method name from the stack trace.
 * Works because [call] and [remoteCall] are inline — their bytecode
 * is inlined into the caller, so stackTrace[1] is the actual calling method.
 */
@PublishedApi
internal fun callerMethodName(): String =
    Throwable().stackTrace[1].methodName

/**
 * Resolves the service name from the receiver: uses [IServiceProxy.serviceName]
 * if implemented, otherwise falls back to the class simple name.
 */
@PublishedApi
internal fun <A : Any> A.resolveServiceName(): String =
    if (this is IServiceProxy) serviceName
    else this::class.simpleName ?: throw Exception("Cannot determine service name")

/**
 * Makes a remote API call with no parameters.
 * Method name is resolved automatically from the call stack.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified RET : Any> A.call(): RET =
    remoteCall(callerMethodName(), emptyList())

/**
 * Makes a remote API call with one parameter.
 * Method name is resolved automatically from the call stack.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified P1, reified RET : Any> A.call(
    p1: P1,
): RET = remoteCall(callerMethodName(), listOf(p1?.let { Json.encodeToString<P1>(it) }))

/**
 * Makes a remote API call with two parameters.
 * Method name is resolved automatically from the call stack.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified P1, reified P2, reified RET : Any> A.call(
    p1: P1,
    p2: P2,
): RET = remoteCall(callerMethodName(), listOf(
    p1?.let { Json.encodeToString<P1>(it) },
    p2?.let { Json.encodeToString<P2>(it) },
))

/**
 * Makes a remote API call with three parameters.
 * Method name is resolved automatically from the call stack.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified P1, reified P2, reified P3, reified RET : Any> A.call(
    p1: P1,
    p2: P2,
    p3: P3,
): RET = remoteCall(callerMethodName(), listOf(
    p1?.let { Json.encodeToString<P1>(it) },
    p2?.let { Json.encodeToString<P2>(it) },
    p3?.let { Json.encodeToString<P3>(it) },
))

/**
 * Makes a JSON-RPC 2.0 remote call using discovered routes from [RouteRegistry].
 *
 * If the route lookup fails and discovery has already happened, automatically
 * attempts re-discovery once before giving up.
 */
@Suppress("unused")
suspend inline fun <A : Any, reified RET : Any> A.remoteCall(
    functionName: String,
    params: List<String?>,
): RET {
    val serviceName = resolveServiceName()
    val resolvedRoute = try {
        routeRegistry.getRoute(serviceName, functionName)
    } catch (e: IllegalStateException) {
        if (routeRegistry.isDiscovered) {
            routeRegistry.rediscoverAndGetRoute(serviceName, functionName)
        } else {
            throw e
        }
    }
    val url = "${appApi.urlBase}$resolvedRoute"

    val rpcRequest = JsonRpcRequest(
        id = jsonRpcCounter.getAndIncrement(),
        method = resolvedRoute,
        params = params,
    )

    if (appApi.delayBeforeRequest > 0) {
        delay(appApi.delayBeforeRequest.toLong())
    }

    Log.d("RPC", "$serviceName.$functionName -> $url")

    val response = try {
        appApi.client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(rpcRequest)
        }
    } catch (e: Exception) {
        Log.e("RPC", "${e::class.simpleName}: ${e.message} (url: $url)")
        throw e
    }

    val rpcResponse: JsonRpcResponse = response.body()

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
