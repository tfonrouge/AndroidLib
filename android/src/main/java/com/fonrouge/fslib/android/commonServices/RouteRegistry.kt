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
 * Resolves RPC routes for JSON-RPC 2.0 calls.
 *
 * Routes are resolved in this order:
 * 1. **Discovered cache** — if [discover] has been called, uses the cached route from `/apiContract`.
 * 2. **Convention** — falls back to `/rpc/{serviceName}.{methodName}` (the named-route convention
 *    used by fsLib servers with `@RpcBindingRoute`).
 *
 * Calling [discover] is **optional**. It is useful for:
 * - Validating the server's API version via [expectedVersion]
 * - Inspecting available services/methods at runtime
 * - Supporting servers that use counter-based routes instead of named routes
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
     * Route prefix used for convention-based route resolution.
     * Defaults to `"/rpc"`, matching fsLib's `@RpcBindingRoute` convention.
     */
    var routePrefix: String = "/rpc"

    /**
     * Minimum required contract version prefix. Set before calling [discover] to
     * reject incompatible servers. Example: "1" matches "1.0", "1.2", etc.
     * Null means accept any version.
     */
    var expectedVersion: String? = null

    /**
     * Fetches the server's `/apiContract` endpoint and caches all service routes.
     *
     * This is **optional** when the server uses named routes (`@RpcBindingRoute`),
     * since [getRoute] falls back to the convention `{routePrefix}/{serviceName}.{methodName}`.
     *
     * Call this when you need version validation or when targeting servers
     * with counter-based routes.
     */
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

    /**
     * Resolves the route for a given service and method.
     *
     * If [discover] has been called, returns the cached route from the API contract.
     * Otherwise, returns the convention-based route: `{routePrefix}/{serviceName}.{methodName}`.
     */
    fun getRoute(serviceName: String, methodName: String): String {
        return cache[serviceName]?.get(methodName)
            ?: conventionRoute(serviceName, methodName)
    }

    /**
     * Attempts to re-discover routes and resolve the route again.
     * Used by [remoteCall] when a route lookup fails and discovery has already happened.
     */
    suspend fun rediscoverAndGetRoute(serviceName: String, methodName: String): String {
        discover()
        return cache[serviceName]?.get(methodName)
            ?: conventionRoute(serviceName, methodName)
    }

    fun clear() {
        contract = null
        cache.clear()
    }

    /**
     * Builds a convention-based route: `{routePrefix}/{serviceName}.{methodName}`.
     */
    private fun conventionRoute(serviceName: String, methodName: String): String =
        "$routePrefix/$serviceName.$methodName"
}

/**
 * Global injectable RouteRegistry instance.
 * Swap for testing: `routeRegistry = RouteRegistry()`
 */
var routeRegistry: RouteRegistry = RouteRegistry()
