# fslib-android

Android client SDK for [fsLib](https://github.com/tfonrouge/fsLib) (Full-Stack Lib). Provides MVVM ViewModels, Compose UI components, a type-safe JSON-RPC 2.0 client with automatic route discovery, and optional barcode scanning -- everything needed to build Android apps against an fsLib backend.

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| `:android` | `com.fonrouge.fslib:android` | Core library: ViewModels, JSON-RPC 2.0, Compose UI, navigation helpers |
| `:barcode` | `com.fonrouge.fslib:barcode` | Optional barcode scanning (CameraX, ML Kit, Google Play Services) |

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.fonrouge.fslib:android:2.0.0")

    // Optional -- only if you need barcode scanning
    implementation("com.fonrouge.fslib:barcode:2.0.0")
}
```

Both artifacts are published to Maven Central. Make sure `mavenCentral()` is in your repositories block.

## Quick Start

### 1. Configure the HTTP client

```kotlin
import com.fonrouge.fslib.android.commonServices.AppApi

AppApi.urlBase = "http://10.0.2.2:8080"
```

### 2. Discover API routes

```kotlin
import com.fonrouge.fslib.android.commonServices.routeRegistry

// Call once at startup (e.g., in a connect screen)
routeRegistry.discover()
```

### 3. Define a service proxy

```kotlin
import com.fonrouge.fslib.android.commonServices.IServiceProxy
import com.fonrouge.fslib.android.commonServices.call

class TaskServiceProxy : IServiceProxy {
    override val serviceName = "ITaskService"

    suspend fun apiList(apiList: ApiList<TaskFilter>): ListState<Task> =
        call(apiList)

    suspend fun apiItem(iApiItem: IApiItem<Task, String, TaskFilter>): ItemState<Task> =
        call(iApiItem)
}
```

### 4. Create ViewModels

```kotlin
private val taskService = TaskServiceProxy()

class TaskListViewModel(
    apiFilter: TaskFilter = TaskFilter(),
) : VMList<CommonTask, Task, String, TaskFilter>(
    apiFilter = apiFilter,
    commonContainer = CommonTask,
    listStateFun = taskService::apiList,
    itemStateFun = taskService::apiItem,
)

class TaskItemViewModel : VMItem<CommonTask, Task, String, TaskFilter>(
    commonContainer = CommonTask,
    itemStateFun = taskService::apiItem,
)
```

### 5. Build Compose screens

```kotlin
@Composable
fun MyApp() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "taskList") {
        composable("taskList") {
            val vm: TaskListViewModel = viewModel()
            ScreenList(vmList = vm, /* ... */)
        }
        composableItem(CommonTask) { apiItem ->
            TaskItemScreen(apiItem = apiItem)
        }
    }
}
```

See the [Usage Guide](USAGE-GUIDE.md) for detailed documentation and the [`samples/showcase-android`](samples/showcase-android) module for a complete working example.

## Architecture

```
┌─────────────────────────────────────────────────┐
│  Compose UI (ScreenList, ScreenConfirmAlert, …) │
├─────────────────────────────────────────────────┤
│  ViewModels (VMBase → VMContainer → VMList/Item)│
├─────────────────────────────────────────────────┤
│  JSON-RPC 2.0 (call(), RouteRegistry, AppApi)   │
├─────────────────────────────────────────────────┤
│  Ktor HTTP Client (Android/CIO/OkHttp engines)  │
└─────────────────────────────────────────────────┘
         ↕ /apiContract discovery
┌─────────────────────────────────────────────────┐
│  fsLib Backend (Ktor server + @RpcService)       │
└─────────────────────────────────────────────────┘
```

### Key Concepts

- **JSON-RPC 2.0 with route discovery** -- `RouteRegistry` fetches the server's `/apiContract` endpoint once, caching all service routes. The `call()` extension function resolves method names automatically from the call stack.
- **Two-phase CRUD** -- `VMItem` uses a Query phase (fetch/validate) followed by an Action phase (save/delete), matching fsLib's server-side pattern.
- **Type-safe generics** -- `ICommonContainer<T, ID, FILT>` provides compile-time validation of document types, ID types, and filter types across navigation, serialization, and API calls.
- **Injectable globals** -- `appApi` and `routeRegistry` can be swapped for testing.

## Tech Stack

- Kotlin 2.3.10 / JVM 21
- AGP 9.1.0
- Compose BOM 2026.03.00 / Material 3
- Ktor Client 3.4.1
- Jetpack Navigation Compose 2.9.7
- Paging Compose 3.4.2
- CameraX 1.5.3 + ML Kit Barcode 17.3.0 (`:barcode` only)
- Min SDK 30, Target/Compile SDK 36

## Building from Source

```bash
./gradlew build                   # Full build
./gradlew :android:test           # Unit tests
./gradlew :barcode:test           # Barcode module tests
./gradlew :app:assembleDebug      # Demo app
```

## Migration from 1.x

Version 2.0.0 is a breaking change:

| Before (1.x) | After (2.0.0) |
|---------------|---------------|
| `com.fonrouge:androidLib:1.1.0` | `com.fonrouge.fslib:android:2.0.0` |
| `import com.fonrouge.androidLib.*` | `import com.fonrouge.fslib.android.*` |
| Barcode included in core | `com.fonrouge.fslib:barcode:2.0.0` (separate) |
| `project(":androidLib")` | `project(":android")` |

## License

See [LICENSE](LICENSE) for details.
