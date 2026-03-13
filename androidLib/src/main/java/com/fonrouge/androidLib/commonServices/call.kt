package com.fonrouge.androidLib.commonServices

import android.util.Log
import com.fonrouge.base.commonServices.IApiCommonService
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

inline fun <reified PAR> serialize(value: PAR): String {
    return Json.encodeToString(value)
}

/**
 * Builds the URL for an API call using the service class name and an explicit route.
 */
fun <A : IApiCommonService> A.urlString(route: String): String =
    this::class.simpleName?.let { apiServiceName ->
        "${appApi.urlBase}/${appApi.appRoute}/$apiServiceName/$route"
    } ?: throw Exception("Error")

/**
 * Makes a remote API call with an explicit route and vararg parameters.
 */
@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified RET : Any> A.call(
    route: String,
    vararg params: Any?,
): RET {
    val serializedParams = params.map { param ->
        param?.let { serialize(it) }
    }
    return remoteCall(route, serializedParams)
}

@Suppress("unused")
suspend inline fun <A : IApiCommonService, reified RET : Any> A.remoteCall(
    route: String,
    params: List<String?>,
): RET {
    val urlString = urlString(route)
    val tag = "HttpClient"
    val response = try {
        Log.d("API CALL Url", urlString)
        Log.d("API CALL Type", "${RET::class.simpleName}")
        appApi.client.post(urlString) {
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
