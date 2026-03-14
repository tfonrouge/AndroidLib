# Usage Guide

Detailed guide for building Android apps with **fslib-android** against an [fsLib](https://github.com/tfonrouge/fsLib) backend.

## Table of Contents

- [Setup](#setup)
- [HTTP Client Configuration](#http-client-configuration)
- [Route Discovery](#route-discovery)
- [Service Proxies & JSON-RPC Calls](#service-proxies--json-rpc-calls)
- [ViewModel Hierarchy](#viewmodel-hierarchy)
  - [VMBase](#vmbase)
  - [VMContainer](#vmcontainer)
  - [VMList](#vmlist)
  - [VMItem](#vmitem)
- [Compose UI Components](#compose-ui-components)
  - [ScreenList](#screenlist)
  - [Alerts & Snackbars](#alerts--snackbars)
  - [Select & SelectRemoteItem](#select--selectremoteitem)
- [Navigation & Routing](#navigation--routing)
- [Pagination](#pagination)
- [Barcode Scanning](#barcode-scanning)
- [Testing](#testing)
- [ProGuard / R8](#proguard--r8)

---

## Setup

Add the dependencies to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.fonrouge.fslib:android:2.0.0")

    // Required transitive dependency (models, API interfaces)
    implementation("com.fonrouge.fslib:core:3.0.2")

    // Optional: barcode scanning
    implementation("com.fonrouge.fslib:barcode:2.0.0")
}
```

Minimum requirements:
- Min SDK 30
- JVM target 21
- Kotlin 2.3.x with Compose plugin
- kotlinx.serialization plugin

---

## HTTP Client Configuration

`AppApi` is a singleton Ktor HTTP client pre-configured with retry, cookies, JSON content negotiation, and logging.

```kotlin
import com.fonrouge.fslib.android.commonServices.AppApi

// Set the server URL before any API calls
AppApi.urlBase = "http://10.0.2.2:8080"

// Optional: configure engine (default is Android)
// Available: IAppApi.Engine.Android, IAppApi.Engine.CIO, IAppApi.Engine.OkHttp

// Optional: set a delay before each request (useful for testing loading states)
AppApi.delayBeforeRequest = 0 // milliseconds

// Optional: clear and recreate the HTTP client
AppApi.clearHttpClient()
```

The client includes:
- **Retry**: 3 retries with exponential backoff (2s base)
- **Timeouts**: 15s connect, 15s socket
- **Cookies**: Session persistence across requests
- **JSON**: kotlinx.serialization via Ktor ContentNegotiation
- **Logging**: Configurable log level

---

## Route Discovery

`RouteRegistry` discovers all available JSON-RPC routes by fetching the server's `/apiContract` endpoint. This must be called once before making any RPC calls.

```kotlin
import com.fonrouge.fslib.android.commonServices.routeRegistry

// Discover routes (call once, e.g., on a connect screen)
routeRegistry.discover()

// Optional: validate server version
routeRegistry.expectedVersion = "1.0.0"

// Check discovery state
routeRegistry.isDiscovered  // true after successful discover()
routeRegistry.version       // server-reported version
routeRegistry.protocol      // protocol info from contract
```

Route discovery is thread-safe (uses Mutex internally). If a route lookup fails after discovery, the library automatically attempts re-discovery once before throwing.

---

## Service Proxies & JSON-RPC Calls

Service proxies map your Android-side classes to server-side RPC interfaces. The `call()` extension function handles JSON-RPC serialization, routing, and deserialization automatically.

### Defining a service proxy

```kotlin
import com.fonrouge.fslib.android.commonServices.IServiceProxy
import com.fonrouge.fslib.android.commonServices.call

class TaskServiceProxy : IServiceProxy {
    // Maps to the server-side interface name for route lookup
    override val serviceName = "ITaskService"

    // Method name "apiList" is resolved automatically from the call stack
    suspend fun apiList(apiList: ApiList<TaskFilter>): ListState<Task> =
        call(apiList)

    suspend fun apiItem(iApiItem: IApiItem<Task, String, TaskFilter>): ItemState<Task> =
        call(iApiItem)
}
```

### How `call()` works

1. `call()` is an inline extension function -- it gets inlined into the calling method
2. `callerMethodName()` inspects the stack trace to find the method name (e.g., `"apiList"`)
3. `resolveServiceName()` reads `IServiceProxy.serviceName` (e.g., `"ITaskService"`)
4. `RouteRegistry` maps `("ITaskService", "apiList")` to the discovered route
5. A JSON-RPC 2.0 request is sent via Ktor
6. The response is deserialized into the return type

### Call variants

```kotlin
// No parameters
suspend fun status(): ServerStatus = call()

// One parameter
suspend fun apiList(apiList: ApiList<Filter>): ListState<Item> = call(apiList)

// Two parameters
suspend fun search(query: String, limit: Int): SearchResult = call(query, limit)

// Three parameters
suspend fun update(id: String, data: Payload, options: Options): Result = call(id, data, options)
```

### Compile-time validation (optional)

If you have a shared interface from `showcase-lib` or similar:

```kotlin
class TaskServiceProxy : ITaskServiceContract, IServiceProxy {
    override val serviceName = "ITaskService"

    // Compiler enforces that signatures match ITaskServiceContract
    override suspend fun apiList(apiList: ApiList<TaskFilter>): ListState<Task> =
        call(apiList)
}
```

---

## ViewModel Hierarchy

```
ViewModel (Jetpack)
  └── VMBase            -- snackbar, state alerts, confirm alerts
       └── VMContainer  -- ICommonContainer + apiFilter
            ├── VMList   -- pagination, periodic updates, pull-to-refresh
            └── VMItem   -- single-item CRUD (Query → Action)
```

### VMBase

Foundation ViewModel providing UI feedback mechanisms.

```kotlin
// State alerts (info, warning, error)
vmBase.stateAlert       // StateFlow<StateAlert?>
vmBase.pushSimpleState(simpleState)
vmBase.clearStateAlert()

// Confirm dialogs (yes/no, yes/cancel)
vmBase.confirmAlert     // StateFlow<ConfirmAlert?>
vmBase.pushConfirmAlert(title, message, onYes)
vmBase.pushConfirmCancelAlert(title, message, onYes)
vmBase.clearConfirmAlert()

// Snackbar
vmBase.snackBarStatus   // StateFlow<String?>
```

### VMContainer

Adds the `ICommonContainer` pattern and API filter management.

```kotlin
abstract class VMContainer<CC, T, ID, FILT> : VMBase() {
    abstract val commonContainer: CC  // type-safe container (navigation, serialization)
    abstract var apiFilter: FILT      // current filter state
}
```

### VMList

Paginated list with periodic refresh and filter support.

```kotlin
class TaskListViewModel(
    apiFilter: TaskFilter = TaskFilter(),
) : VMList<CommonTask, Task, String, TaskFilter>(
    apiFilter = apiFilter,
    commonContainer = CommonTask,
    listStateFun = taskService::apiList,    // called for each page
    itemStateFun = taskService::apiItem,    // optional, enables swipe-to-delete
)
```

Key properties and methods:

```kotlin
// Pagination
vmList.flowPagingData       // Flow<PagingData<T>> -- collect in Compose with collectAsLazyPagingItems()
vmList.pageSize             // StateFlow<Int> (default 20)
vmList.setPageSize(50)

// Refresh
vmList.requestRefresh = true       // trigger list reload
vmList.refreshingList              // StateFlow<Boolean> -- pull-to-refresh state
vmList.refreshListCounter          // StateFlow<Int> -- increments on each refresh

// Periodic updates
vmList.periodicUpdate = true       // enable auto-refresh
vmList.periodicInterval = 5000     // milliseconds between refreshes

// Filter management
vmList.apiFilter                   // current filter (read/write)
vmList.apiFilterFlow               // StateFlow<FILT>
vmList.onEvent(VMList.UIBaseEvent.EditingFilter)    // start editing
vmList.onEvent(VMList.UIBaseEvent.RefreshByFilter)  // apply filter changes

// Delete items (requires itemStateFun)
vmList.deleteItem(item)
```

### VMItem

Single-item CRUD with the two-phase Query/Action pattern.

```kotlin
class TaskItemViewModel : VMItem<CommonTask, Task, String, TaskFilter>(
    commonContainer = CommonTask,
    itemStateFun = taskService::apiItem,
)
```

Key properties and methods:

```kotlin
// Current item state
vmItem.item                // StateFlow<T?> -- the loaded/edited item
vmItem.crudTask            // StateFlow<CrudTask> -- Create, Read, Update, or Delete
vmItem.controlsEnabled     // StateFlow<Boolean> -- false during API calls
vmItem.itemAlreadyOn       // StateFlow<Boolean> -- item existed when creating

// Two-phase CRUD
// Phase 1: Query -- fetch the item
vmItem.makeQueryCall(apiItem)

// Phase 2: Action -- save/delete the item
vmItem.makeActionCall(apiItem)

// Convenience overloads
vmItem.makeQueryCall(id, apiFilter)          // read by ID
vmItem.makeActionCall(item, apiFilter)       // update item
```

---

## Compose UI Components

### ScreenList

Full-featured list screen with drawer, FAB, top bar, alerts, and pull-to-refresh.

```kotlin
@Composable
fun TaskListScreen(navController: NavHostController) {
    val vm: TaskListViewModel = viewModel()

    ScreenList(
        vmList = vm,
        navHostController = navController,
        topBarTitle = "Tasks",
        topBarActions = {
            IconButton(onClick = { /* ... */ }) {
                Icon(Icons.Default.Refresh, null)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                CommonTask.navigateCreateItem(navController) { TaskFilter() }
            }) {
                Icon(Icons.Default.Add, null)
            }
        },
        modalDrawerContent = { drawerState ->
            TaskFilterDrawer(vm, drawerState)
        },
    ) { lazyPagingItems ->
        items(lazyPagingItems.itemCount) { index ->
            lazyPagingItems[index]?.let { task ->
                TaskCard(task, onClick = {
                    CommonTask.navigateItem(navController, task._id, vm.apiFilter)
                })
            }
        }
    }
}
```

### Alerts & Snackbars

Alert screens are included automatically by `ScreenList`. For custom screens:

```kotlin
@Composable
fun MyScreen(vm: VMBase) {
    // State alert dialog (info/warning/error)
    ScreenStateAlert(vmBase = vm)

    // Confirm dialog (yes/no, yes/cancel)
    ScreenConfirmAlert(vmBase = vm)
}
```

### Select & SelectRemoteItem

Dropdown selectors for forms:

```kotlin
// Local dropdown
Select(
    label = "Priority",
    options = Priority.entries.toList(),
    selected = selectedPriority,
    onSelect = { priority -> /* ... */ },
    displayText = { it.name },
)

// Remote search with debounce
SelectRemoteItem(
    label = "Assignee",
    searchFun = { query -> searchUsers(query) },
    displayText = { it.name },
    onSelect = { user -> /* ... */ },
)
```

---

## Navigation & Routing

The library provides type-safe navigation helpers that handle serialization of API parameters into URI-encoded route arguments.

### Setting up routes

```kotlin
NavHost(navController, startDestination = "taskList") {
    composable("taskList") {
        TaskListScreen(navController)
    }

    // Type-safe item route with automatic parameter decoding
    composableItem(CommonTask) { apiItem ->
        TaskItemScreen(apiItem = apiItem)
    }
}
```

### Navigating

```kotlin
// Navigate to view/edit an existing item
CommonTask.navigateItem(navController, itemId, currentFilter)

// Navigate to create a new item
CommonTask.navigateCreateItem(navController) { TaskFilter() }

// Navigate to a child list (master-detail)
CommonTask.navigateChildList(navController, parentId) { TaskFilter() }
```

---

## Pagination

Pagination is built on Jetpack Paging 3 via `BasePagingSource`.

```kotlin
@Composable
fun TaskListContent(vm: TaskListViewModel) {
    val lazyPagingItems = vm.flowPagingData.collectAsLazyPagingItems()

    LazyColumn {
        items(lazyPagingItems.itemCount) { index ->
            lazyPagingItems[index]?.let { task ->
                TaskCard(task)
            }
        }
    }
}
```

`BasePagingSource` calls `VMList.listStateGetter(pageNum)` for each page, which in turn calls your `listStateFun`. Errors are caught and reported via `pushSimpleState()`.

---

## Barcode Scanning

The barcode module (`:barcode`) provides ready-to-use scanning screens. It has **no dependency** on the `:android` module.

### Setup

Add the dependency:

```kotlin
implementation("com.fonrouge.fslib:barcode:2.0.0")
```

### Using ScanBarcodeScreen

```kotlin
import com.fonrouge.fslib.android.barcode.VMCamera
import com.fonrouge.fslib.android.barcode.ScanBarcodeScreen
import com.fonrouge.fslib.android.barcode.CodeEntry

@Composable
fun MyScreen() {
    val vmCamera: VMCamera = viewModel()

    ScanBarcodeScreen(
        vmCamera = vmCamera,
        onReadBarcode = { codeEntry: CodeEntry ->
            // codeEntry.code -- the scanned string
            // codeEntry.source -- Camera or Keyboard
            // codeEntry.barcode -- raw ML Kit Barcode (if from camera)
        },
    ) {
        // Content shown when scanner is closed
        Button(onClick = { vmCamera.onEvent(VMCamera.UIEvent.Open) }) {
            Text("Scan Barcode")
        }
    }
}
```

### Camera type selection

```kotlin
// Default for all VMCamera instances
VMCamera.defaultCameraType = VMCamera.CameraType.GooglePlay  // or CameraType.CameraX

// Per-instance
vmCamera.setSelectedCameraType(VMCamera.CameraType.CameraX)
```

- **GooglePlay** -- Uses Google Play Services code scanner (no camera permission needed, simpler UI)
- **CameraX** -- Full camera preview with ML Kit image analysis, overlay, torch control, manual text entry

### Manual entry

The CameraX scanner includes a manual text entry option. You can also trigger it programmatically:

```kotlin
vmCamera.setManualEntry("ABC-123")
vmCamera.onEvent(VMCamera.UIEvent.ManualEntry)
```

---

## Testing

The library supports testing through injectable globals:

```kotlin
import com.fonrouge.fslib.android.commonServices.appApi
import com.fonrouge.fslib.android.commonServices.routeRegistry

@Before
fun setup() {
    // Swap the HTTP client for a mock
    appApi = FakeAppApi()

    // Swap the route registry for a pre-configured one
    routeRegistry = RouteRegistry().apply {
        // configure as needed
    }
}
```

### Unit test dependencies

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
testImplementation("app.cash.turbine:turbine:1.2.1")
testImplementation("io.mockk:mockk:1.14.9")
testImplementation("io.ktor:ktor-client-mock:3.4.1")
```

### Example: testing a ViewModel

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state alert is pushed on error`() = runTest {
        val vm = MyViewModel()
        vm.pushSimpleState(SimpleState(isOk = false, msgError = "fail"))
        assertNotNull(vm.stateAlert.value)
    }
}
```

---

## ProGuard / R8

Consumer ProGuard rules are bundled in the `:android` AAR. They keep:

- **`@Serializable` JSON-RPC wire types** -- `JsonRpcRequest`, `JsonRpcResponse`, `RpcException`, `ApiContract`, etc.
- **`IServiceProxy` implementors** -- method names must be preserved because `callerMethodName()` uses stack trace reflection
- **kotlinx.serialization serializers** -- generated `serializer()` methods

No additional ProGuard configuration is needed in consumer apps.
