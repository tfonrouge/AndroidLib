package com.fonrouge.fslib.android.commonServices

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

// These types mirror fsLib's RouteContract.kt (jvmMain).
// If fsLib moves them to commonMain, replace with the shared types.

@Serializable
data class ApiRouteEntry(val route: String, val method: String)

@Serializable
data class ServiceContractEntry(val service: String, val methods: Map<String, ApiRouteEntry>)

@Serializable
data class ProtocolInfo(
    val format: String = "json-rpc-2.0",
    val contentType: String = "application/json",
    val paramEncoding: String = "",
    val resultEncoding: String = "",
)

@Serializable
data class ApiContract(
    val version: String,
    val protocol: ProtocolInfo = ProtocolInfo(),
    val services: List<ServiceContractEntry>,
)

class IncompatibleContractException(
    val expected: String,
    val actual: String,
) : Exception("Incompatible API contract: expected v$expected, got v$actual")

/**
 * Discovers and caches RPC route mappings from the server's `/apiContract` endpoint.
 *
 * Instantiate or use the global [routeRegistry] reference. Thread-safe for concurrent
 * coroutine access via internal [Mutex].
 */
class RouteRegistry {
    private var contract: ApiContract? = null
    private val cache: MutableMap<String, Map<String, String>> = mutableMapOf()
    private val discoveryMutex = Mutex()

    val isDiscovered: Boolean get() = contract != null
    val version: String? get() = contract?.version
    val protocol: ProtocolInfo? get() = contract?.protocol

    /**
     * Minimum required contract version prefix. Set before calling [discover] to
     * reject incompatible servers. Example: "1" matches "1.0", "1.2", etc.
     * Null means accept any version.
     */
    var expectedVersion: String? = null

    suspend fun discover() {
        discoveryMutex.withLock {
            val response: ApiContract = appApi.client.get("${appApi.urlBase}/apiContract").body()
            expectedVersion?.let { expected ->
                if (!response.version.startsWith(expected)) {
                    throw IncompatibleContractException(expected, response.version)
                }
            }
            contract = response
            cache.clear()
            response.services.forEach { svc ->
                cache[svc.service] = svc.methods.mapValues { it.value.route }
            }
        }
    }

    fun getRoute(serviceName: String, methodName: String): String {
        return cache[serviceName]?.get(methodName)
            ?: throw IllegalStateException(
                "Route not found: $serviceName.$methodName. " +
                    "Call RouteRegistry.discover() first."
            )
    }

    /**
     * Attempts to re-discover routes and resolve the route again.
     * Used by [remoteCall] when a route lookup fails and discovery has already happened.
     */
    suspend fun rediscoverAndGetRoute(serviceName: String, methodName: String): String {
        discover()
        return cache[serviceName]?.get(methodName)
            ?: throw IllegalStateException(
                "Route not found after re-discovery: $serviceName.$methodName"
            )
    }

    fun clear() {
        contract = null
        cache.clear()
    }
}

/**
 * Global injectable RouteRegistry instance.
 * Swap for testing: `routeRegistry = RouteRegistry()`
 */
var routeRegistry: RouteRegistry = RouteRegistry()
