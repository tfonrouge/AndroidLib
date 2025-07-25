package com.fonrouge.androidlib.commonServices

import android.util.Log
import com.fonrouge.fsLib.commonServices.UserLogin
import com.fonrouge.fsLib.model.base.IUser
import com.fonrouge.fsLib.model.state.ItemState
import com.fonrouge.fsLib.model.state.SimpleState
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AppApi {
    var version: String = "0.0"
    var urlBase: String = "localhost"
    var appRoute: String = "appRoute"
    var userAgent: String = "AppAndroid"
    var serializedIUser: String? = null
    var engine: Engine? = null  // uses engine default
    var delayBeforeRequest: Int = 0
    private var _httpClient: HttpClient? = null

    private fun getEngine(
        engine: Engine?,
        block: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient {
        return when (engine) {
            Engine.Android -> HttpClient(engineFactory = Android, block = block)
            Engine.CIO -> HttpClient(engineFactory = CIO, block = block)
            Engine.OkHttp -> HttpClient(engineFactory = OkHttp, block = block)
            null -> HttpClient(block = block)
        }
    }

    val client: HttpClient
        get() {
            if (_httpClient == null) {
                _httpClient = getEngine(engine) {
                    install(Auth)
                    install(ContentNegotiation) {
                        json()
                    }
                    install(UserAgent) {
                        agent = userAgent
                    }
                    install(HttpCookies)
                    install(DefaultRequest) {
                        contentType(ContentType.Application.Json)
                    }
                    install(Logging) {
                        level = LogLevel.ALL
                        logger = object : Logger {
                            override fun log(message: String) {
                                Log.i("HttpClient", message)
                            }
                        }
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 15000
                        connectTimeoutMillis = 15000
                        socketTimeoutMillis = 15000
                    }
                }
            }
            return _httpClient!!
        }

    val logged get() = serializedIUser != null

    fun clearHttpClient() {
        _httpClient?.close()
        _httpClient = null
    }

    inline fun <reified T : IUser<*>> getUser(): T? {
        return serializedIUser?.let { Json.decodeFromString(it) }
    }

    suspend inline fun <reified T : IUser<*>> loginForm(
        loginUrl: String,
        userLogin: UserLogin,
    ): ItemState<T> {
        serializedIUser = null
        val httpResponse = try {
            client.submitForm(
                url = "$urlBase/$loginUrl",
                formParameters = parameters {
                    append(UserLogin::username.name, userLogin.username)
                    append(UserLogin::password.name, userLogin.password)
                }
            )
        } catch (e: Exception) {
            return ItemState(isOk = false, msgError = e.message)
        }
        val itemState = try {
            ItemState(item = httpResponse.body<T>()).also {
                serializedIUser = Json.encodeToString(it.item)
            }
        } catch (e: Exception) {
            ItemState(isOk = false, msgError = e.message)
        }
        return itemState
    }

    suspend fun logout(logoutUrl: String = "/logout"): SimpleState {
        serializedIUser = null
        return try {
            clearHttpClient()
            client.get("$urlBase/$logoutUrl")
            SimpleState(isOk = true)
        } catch (e: Exception) {
            e.printStackTrace()
            SimpleState(isOk = false, msgError = e.message)
        }
    }

    /**
     * link - https://ktor.io/docs/http-client-engines.html#minimal-version
     */
    enum class Engine {
        Android,
        CIO,
        OkHttp,
    }
}
