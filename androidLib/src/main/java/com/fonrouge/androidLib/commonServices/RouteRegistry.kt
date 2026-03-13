package com.fonrouge.androidLib.commonServices

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class ApiRouteEntry(val route: String, val method: String)

@Serializable
data class ServiceContractEntry(val service: String, val methods: Map<String, ApiRouteEntry>)

@Serializable
data class ApiContract(val version: String, val services: List<ServiceContractEntry>)

object RouteRegistry {
    private var contract: ApiContract? = null
    private val cache: MutableMap<String, Map<String, String>> = mutableMapOf()

    val isDiscovered: Boolean get() = contract != null
    val version: String? get() = contract?.version

    suspend fun discover() {
        val response: ApiContract = appApi.client.get("${appApi.urlBase}/apiContract").body()
        contract = response
        cache.clear()
        response.services.forEach { svc ->
            cache[svc.service] = svc.methods.mapValues { it.value.route }
        }
    }

    fun getRoute(serviceName: String, methodName: String): String {
        return cache[serviceName]?.get(methodName)
            ?: throw IllegalStateException(
                "Route not found: $serviceName.$methodName. " +
                    "Call RouteRegistry.discover() first."
            )
    }

    fun clear() {
        contract = null
        cache.clear()
    }
}
