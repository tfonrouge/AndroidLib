# Showcase Android Sample

Reference Android app demonstrating full integration with an [fsLib](https://github.com/tfonrouge/fsLib) backend using [fslib-android](../../README.md). Implements a task management UI with paginated lists, CRUD forms, filtering, and swipe-to-delete.

## What This Sample Demonstrates

- **Route discovery** -- Connecting to an fsLib backend via `RouteRegistry.discover()`
- **Service proxies** -- Type-safe JSON-RPC 2.0 calls with `IServiceProxy` and `call()`
- **VMList** -- Paginated task list with pull-to-refresh, periodic updates, and filter drawer
- **VMItem** -- Two-phase CRUD (Query then Action) for creating and editing tasks
- **Compose Navigation** -- Type-safe navigation with `composableItem()` and `navigateItem()`
- **ScreenList** -- Pre-built list scaffold with top bar, FAB, drawer, alerts, and snackbar
- **Swipe-to-delete** -- `SwipeToDismissBox` with confirmation dialog via `pushConfirmAlert()`
- **Filter drawer** -- `ModalNavigationDrawer` with `FilterChip` selection

## Prerequisites

This sample requires a running fsLib showcase backend server. Start it from the [fsLib](https://github.com/tfonrouge/fsLib) project:

```bash
# From the fsLib project directory
./gradlew :samples:fullstack:showcase:showcase-app:run
```

The server starts on `http://localhost:8080` by default. If running the Android app on an emulator, use `http://10.0.2.2:8080` (the emulator's alias for the host machine).

## Running

```bash
# From the fslib-android root
./gradlew :samples:showcase-android:installDebug
```

Or open the project in Android Studio and run the `showcase-android` run configuration.

## App Flow

1. **ConnectScreen** -- Enter the backend URL and tap "Discover & Connect". The app calls `routeRegistry.discover()` to fetch the API contract from `/apiContract`.

2. **TaskListScreen** -- Paginated list of tasks loaded via `VMList` + `BasePagingSource`. Features:
   - Pull-to-refresh
   - FAB to create new tasks
   - Filter drawer (priority, status, assignee)
   - Swipe left to delete (with confirmation)
   - Tap a card to edit

3. **TaskItemScreen** -- CRUD form for a single task. Uses `VMItem` with the two-phase pattern:
   - **Query phase**: `makeQueryCall()` fetches the task (or creates a blank one)
   - **Action phase**: `makeActionCall()` saves changes to the backend
   - Form fields: title, description, assignee, priority (dropdown), status (dropdown), estimated hours

## Project Structure

```
samples/showcase-android/src/main/java/com/example/showcase/
  ShowcaseActivity.kt      -- NavHost, ConnectScreen, TaskListScreen, TaskCard, badges
  TaskService.kt           -- IServiceProxy implementation mapping to "ITaskService"
  TaskListViewModel.kt     -- VMList<CommonTask, Task, String, TaskFilter>
  TaskItemViewModel.kt     -- VMItem<CommonTask, Task, String, TaskFilter>
  TaskItemScreen.kt        -- CRUD form with dropdown selectors
  TaskFilterDrawer.kt      -- Filter drawer with priority/status chips and assignee search
```

## Key Patterns

### Service Proxy

```kotlin
class TaskServiceProxy : ITaskServiceContract, IServiceProxy {
    override val serviceName = "ITaskService"

    override suspend fun apiList(apiList: ApiList<TaskFilter>): ListState<Task> =
        call(apiList)

    override suspend fun apiItem(iApiItem: IApiItem<Task, String, TaskFilter>): ItemState<Task> =
        call(iApiItem)
}
```

The `serviceName` maps to the server-side `@RpcService` interface name. Implementing `ITaskServiceContract` (from `showcase-lib`) gives compile-time validation that method signatures match the server.

### ViewModel Wiring

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
```

### Navigation

```kotlin
// Type-safe item route with automatic parameter encoding
composableItem(CommonTask) { apiItem ->
    TaskItemScreen(apiItem = apiItem)
}

// Navigate to edit
CommonTask.navigateItem(navController, apiItem)

// Navigate to create
CommonTask.navigateCreateItem(navController)
```

## Dependencies

This sample depends on:
- `:android` -- fslib-android core library (project dependency for local development)
- `com.example:showcase-lib:1.0.0-SNAPSHOT` -- Shared models and contracts from the fsLib showcase sample (Task, TaskFilter, CommonTask, ITaskServiceContract, Priority, TaskStatus)
