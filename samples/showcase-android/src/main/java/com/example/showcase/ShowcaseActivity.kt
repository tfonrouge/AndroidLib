package com.example.showcase

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fonrouge.fslib.android.commonServices.AppApi
import com.fonrouge.fslib.android.commonServices.routeRegistry
import com.fonrouge.fslib.android.configCommon.composableItem
import com.fonrouge.fslib.android.configCommon.navigateCreateItem
import com.fonrouge.fslib.android.configCommon.navigateItem
import com.fonrouge.fslib.android.ui.DismissBackgroundDelete
import com.fonrouge.fslib.android.ui.ScreenList
import com.fonrouge.base.api.ApiItem
import kotlinx.coroutines.launch

class ShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ShowcaseApp()
            }
        }
    }
}

@Composable
fun ShowcaseApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "connect") {
        composable("connect") {
            ConnectScreen(navController)
        }
        composable("taskList") {
            TaskListScreen(navController)
        }
        composableItem(CommonTask) { apiItem ->
            TaskItemScreen(
                navHostController = navController,
                apiItem = apiItem,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(navController: NavHostController) {
    var serverUrl by rememberSaveable { mutableStateOf("http://10.0.2.2:8080") }
    var isDiscovering by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Connect to FSLib showcase backend",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Start the showcase server with: ./gradlew :samples:fullstack:showcase:run",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            singleLine = true,
            enabled = !isDiscovering,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    isDiscovering = true
                    error = null
                    AppApi.urlBase = serverUrl
                    try {
                        routeRegistry.discover()
                        Log.d("Showcase", "Contract discovered: v${routeRegistry.version}")
                        navController.navigate("taskList") {
                            popUpTo("connect") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        Log.e("Showcase", "Discovery failed: ${e.message}", e)
                        error = e.message ?: "Unknown error"
                    } finally {
                        isDiscovering = false
                    }
                }
            },
            enabled = !isDiscovering,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isDiscovering) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Discover & Connect")
            }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavHostController,
    vmList: TaskListViewModel = viewModel(),
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ScreenList<TaskListViewModel, CommonTask, Task, String, TaskFilter>(
        navHostController = navController,
        vmList = vmList,
        drawerState = drawerState,
        topBarTitle = {
            Column {
                Text("Showcase Sample")
                routeRegistry.version?.let {
                    Text(
                        text = "API contract v$it",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        topBarNavigationIcon = {},
        topBarActions = {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
            IconButton(onClick = { vmList.requestRefresh = true }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { CommonTask.navigateCreateItem(navController) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        },
        modalNavigationDrawerContent = {
            TaskFilterDrawer(drawerState = drawerState, vmList = vmList)
        },
    ) { task ->
        task?.let {
            SwipeableTaskCard(
                task = it,
                vmList = vmList,
                onClick = {
                    CommonTask.navigateItem(
                        navHostController = navController,
                        apiItem = ApiItem.Query.Update(
                            id = it._id,
                            apiFilter = vmList.apiFilter,
                        ),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    vmList: TaskListViewModel,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                vmList.pushConfirmAlert(
                    confirmText = "Delete task \"${task.title}\"?",
                    onConfirm = {
                        scope.launch {
                            vmList.deleteItem(task)
                            vmList.requestRefresh = true
                        }
                    },
                )
                false // don't dismiss yet, wait for confirm
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            DismissBackgroundDelete(
                dismissState = dismissState,
                dismissDirection = SwipeToDismissBoxValue.EndToStart,
            )
        },
        enableDismissFromStartToEnd = false,
    ) {
        TaskCard(task = task, onClick = onClick)
    }
}

@Composable
fun TaskCard(task: Task, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .let { m -> onClick?.let { m.clickable(onClick = it) } ?: m },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                PriorityBadge(task.priority)
            }

            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = task.assignee,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                StatusBadge(task.status)
                Text(
                    text = "${task.estimatedHours}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when (priority) {
        Priority.CRITICAL -> Color(0xFFD32F2F)
        Priority.HIGH -> Color(0xFFE65100)
        Priority.MEDIUM -> Color(0xFF1565C0)
        else -> Color(0xFF757575)
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = priority,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        TaskStatus.DONE -> Color(0xFF2E7D32)
        TaskStatus.IN_PROGRESS -> Color(0xFF1565C0)
        TaskStatus.OPEN -> Color(0xFF00838F)
        else -> Color(0xFF424242)
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = status,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
