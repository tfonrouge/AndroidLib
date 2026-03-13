package com.fonrouge.androidLib.commonServices

import io.ktor.client.HttpClient

interface IAppApi {
    var version: String
    var urlBase: String
    var appRoute: String
    var userAgent: String
    var delayBeforeRequest: Int
    val client: HttpClient
    val logged: Boolean
    fun clearHttpClient()
}

/**
 * Injectable API reference. Defaults to the [AppApi] singleton.
 * Swap this for testing: `appApi = FakeAppApi()`
 */
var appApi: IAppApi = AppApi
