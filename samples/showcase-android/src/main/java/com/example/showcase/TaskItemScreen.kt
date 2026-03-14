package com.example.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.fonrouge.fslib.android.ui.ScreenStateAlert
import com.fonrouge.fslib.android.ui.snackbarHostState
import com.fonrouge.base.api.ApiItem
import com.fonrouge.base.api.CrudTask
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItemScreen(
    navHostController: NavHostController,
    apiItem: ApiItem.Query<Task, String, TaskFilter>,
    vmItem: TaskItemViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val item by vmItem.itemFlow.collectAsStateWithLifecycle()
    val crudTask by vmItem.crudTaskFlow.collectAsStateWithLifecycle()
    val controlsEnabled by vmItem.controlsEnabledFlow.collectAsStateWithLifecycle()

    LaunchedEffect(apiItem) {
        vmItem.makeQueryCall(
            id = apiItem.id,
            crudTask = apiItem.crudTask,
        ) {}
    }

    ScreenStateAlert(vmBase = vmItem)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (crudTask) {
                            CrudTask.Create -> "New Task"
                            CrudTask.Read -> "Task Details"
                            CrudTask.Update -> "Edit Task"
                            CrudTask.Delete -> "Delete Task"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
        floatingActionButton = {
            if (controlsEnabled) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            vmItem.makeActionCall { itemState ->
                                if (!itemState.hasError) {
                                    navHostController.popBackStack()
                                } else {
                                    itemState.pushAlert()
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState(vmItem)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item?.let { task ->
                var title by remember(task) { mutableStateOf(task.title) }
                var description by remember(task) { mutableStateOf(task.description) }
                var assignee by remember(task) { mutableStateOf(task.assignee) }
                var priority by remember(task) { mutableStateOf(task.priority) }
                var status by remember(task) { mutableStateOf(task.status) }
                var estimatedHours by remember(task) { mutableStateOf(task.estimatedHours.toString()) }

                fun updateItem() {
                    vmItem.item = task.copy(
                        title = title,
                        description = description,
                        assignee = assignee,
                        priority = priority,
                        status = status,
                        estimatedHours = estimatedHours.toIntOrNull() ?: 0,
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; updateItem() },
                    label = { Text("Title") },
                    enabled = controlsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; updateItem() },
                    label = { Text("Description") },
                    enabled = controlsEnabled,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it; updateItem() },
                    label = { Text("Assignee") },
                    enabled = controlsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )

                DropdownSelector(
                    label = "Priority",
                    selected = priority,
                    options = Priority.entries,
                    enabled = controlsEnabled,
                    onSelect = { priority = it; updateItem() },
                )

                DropdownSelector(
                    label = "Status",
                    selected = status,
                    options = TaskStatus.entries,
                    enabled = controlsEnabled,
                    onSelect = { status = it; updateItem() },
                )

                OutlinedTextField(
                    value = estimatedHours,
                    onValueChange = { estimatedHours = it; updateItem() },
                    label = { Text("Estimated Hours") },
                    enabled = controlsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!controlsEnabled && crudTask == CrudTask.Read) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PriorityBadge(task.priority)
                        StatusBadge(task.status)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
