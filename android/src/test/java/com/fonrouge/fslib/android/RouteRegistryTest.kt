package com.fonrouge.fslib.android

import com.fonrouge.fslib.android.commonServices.ApiContract
import com.fonrouge.fslib.android.commonServices.ApiRouteEntry
import com.fonrouge.fslib.android.commonServices.IAppApi
import com.fonrouge.fslib.android.commonServices.IncompatibleContractException
import com.fonrouge.fslib.android.commonServices.ProtocolInfo
import com.fonrouge.fslib.android.commonServices.RouteRegistry
import com.fonrouge.fslib.android.commonServices.ServiceContractEntry
import com.fonrouge.fslib.android.commonServices.appApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRegistryTest {

    private lateinit var registry: RouteRegistry
    private lateinit var originalAppApi: IAppApi

    private val sampleContract = ApiContract(
        version = "1.0",
        protocol = ProtocolInfo(),
        services = listOf(
            ServiceContractEntry(
                service = "UserService",
                methods = mapOf(
                    "getUser" to ApiRouteEntry(route = "/api/user/get", method = "POST"),
                    "listUsers" to ApiRouteEntry(route = "/api/user/list", method = "POST"),
                )
            ),
            ServiceContractEntry(
                service = "OrderService",
                methods = mapOf(
                    "create" to ApiRouteEntry(route = "/api/order/create", method = "POST"),
                )
            ),
        )
    )

    private fun buildMockAppApi(contract: ApiContract = sampleContract): IAppApi {
        val jsonBody = Json.encodeToString(ApiContract.serializer(), contract)
        val mockEngine = MockEngine { _ ->
            respond(
                content = jsonBody,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val mock = mockk<IAppApi>(relaxed = true)
        every { mock.client } returns client
        every { mock.urlBase } returns "http://localhost"
        return mock
    }

    @Before
    fun setup() {
        originalAppApi = appApi
        registry = RouteRegistry()
    }

    @After
    fun tearDown() {
        appApi = originalAppApi
    }

    // --- Initial state ---

    @Test
    fun `initial state has isDiscovered false`() {
        assertFalse(registry.isDiscovered)
    }

    @Test
    fun `initial state has version null`() {
        assertNull(registry.version)
    }

    @Test
    fun `initial state has protocol null`() {
        assertNull(registry.protocol)
    }

    // --- Convention-based routes (no discover) ---

    @Test
    fun `getRoute returns convention route when not discovered`() {
        assertEquals("/rpc/UserService.getUser", registry.getRoute("UserService", "getUser"))
    }

    @Test
    fun `getRoute uses custom routePrefix for convention route`() {
        registry.routePrefix = "/api/v2"
        assertEquals("/api/v2/UserService.getUser", registry.getRoute("UserService", "getUser"))
    }

    @Test
    fun `getRoute returns convention route for unknown service after discover`() = runTest {
        appApi = buildMockAppApi()
        registry.discover()
        assertEquals("/rpc/UnknownService.getUser", registry.getRoute("UnknownService", "getUser"))
    }

    @Test
    fun `getRoute returns convention route for unknown method on known service after discover`() = runTest {
        appApi = buildMockAppApi()
        registry.discover()
        assertEquals("/rpc/UserService.unknownMethod", registry.getRoute("UserService", "unknownMethod"))
    }

    // --- clear() ---

    @Test
    fun `clear resets state after discover`() = runTest {
        appApi = buildMockAppApi()
        registry.discover()
        assertTrue(registry.isDiscovered)

        registry.clear()

        assertFalse(registry.isDiscovered)
        assertNull(registry.version)
    }

    // --- Discovered routes take priority ---

    @Test
    fun `getRoute returns discovered route over convention`() = runTest {
        appApi = buildMockAppApi()
        registry.discover()

        assertEquals("/api/user/get", registry.getRoute("UserService", "getUser"))
        assertEquals("/api/user/list", registry.getRoute("UserService", "listUsers"))
        assertEquals("/api/order/create", registry.getRoute("OrderService", "create"))
    }

    @Test
    fun `discover sets version and protocol`() = runTest {
        appApi = buildMockAppApi()
        registry.discover()

        assertTrue(registry.isDiscovered)
        assertEquals("1.0", registry.version)
        assertEquals("json-rpc-2.0", registry.protocol?.format)
    }

    // --- expectedVersion validation ---

    @Test
    fun `discover succeeds when expectedVersion matches prefix`() = runTest {
        appApi = buildMockAppApi()
        registry.expectedVersion = "1"
        registry.discover()

        assertTrue(registry.isDiscovered)
        assertEquals("1.0", registry.version)
    }

    @Test(expected = IncompatibleContractException::class)
    fun `discover throws IncompatibleContractException when version does not match`() = runTest {
        appApi = buildMockAppApi()
        registry.expectedVersion = "2"
        registry.discover()
    }

    @Test
    fun `IncompatibleContractException contains expected and actual versions`() = runTest {
        appApi = buildMockAppApi()
        registry.expectedVersion = "2"
        try {
            registry.discover()
        } catch (e: IncompatibleContractException) {
            assertEquals("2", e.expected)
            assertEquals("1.0", e.actual)
            assertTrue(e.message!!.contains("v2"))
            assertTrue(e.message!!.contains("v1.0"))
        }
    }

    @Test
    fun `discover with null expectedVersion accepts any version`() = runTest {
        appApi = buildMockAppApi()
        registry.expectedVersion = null
        registry.discover()

        assertTrue(registry.isDiscovered)
    }

    // --- rediscoverAndGetRoute() ---

    @Test
    fun `rediscoverAndGetRoute returns route after re-discovery`() = runTest {
        appApi = buildMockAppApi()
        val route = registry.rediscoverAndGetRoute("UserService", "getUser")
        assertEquals("/api/user/get", route)
        assertTrue(registry.isDiscovered)
    }

    @Test
    fun `rediscoverAndGetRoute falls back to convention for unknown service`() = runTest {
        appApi = buildMockAppApi()
        val route = registry.rediscoverAndGetRoute("UnknownService", "unknownMethod")
        assertEquals("/rpc/UnknownService.unknownMethod", route)
    }

    // --- Thread safety ---

    @Test
    fun `concurrent discover calls do not corrupt state`() = runTest {
        appApi = buildMockAppApi()

        val jobs = (1..50).map {
            launch {
                registry.discover()
            }
        }
        jobs.forEach { it.join() }

        assertTrue(registry.isDiscovered)
        assertEquals("1.0", registry.version)
        assertEquals("/api/user/get", registry.getRoute("UserService", "getUser"))
        assertEquals("/api/order/create", registry.getRoute("OrderService", "create"))
    }

    @Test
    fun `concurrent discover and getRoute do not throw ConcurrentModificationException`() = runTest {
        appApi = buildMockAppApi()
        // Initial discover so getRoute has data
        registry.discover()

        val jobs = (1..50).map { i ->
            launch {
                if (i % 2 == 0) {
                    registry.discover()
                } else {
                    registry.getRoute("UserService", "getUser")
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(registry.isDiscovered)
    }
}
