package com.example.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fonrouge.fslib.android.viewModel.VMList
import kotlinx.coroutines.launch

@Composable
fun TaskFilterDrawer(
    drawerState: DrawerState,
    vmList: TaskListViewModel,
) {
    val scope = rememberCoroutineScope()
    var selectedPriority by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var assigneeSearch by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)

        // Priority filter
        Text("Priority", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Priority.entries.forEach { priority ->
                FilterChip(
                    selected = selectedPriority == priority,
                    onClick = {
                        selectedPriority = if (selectedPriority == priority) null else priority
                    },
                    label = { Text(priority) },
                )
            }
        }

        // Status filter
        Text("Status", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = {
                        selectedStatus = if (selectedStatus == status) null else status
                    },
                    label = { Text(status) },
                )
            }
        }

        // Assignee filter
        Text("Assignee", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = assigneeSearch,
            onValueChange = { assigneeSearch = it },
            label = { Text("Search by assignee") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    selectedPriority = null
                    selectedStatus = null
                    assigneeSearch = ""
                    vmList.onEvent(VMList.UIBaseEvent.RefreshByFilter)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear")
            }
            Button(
                onClick = {
                    vmList.onEvent(VMList.UIBaseEvent.RefreshByFilter)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Apply")
            }
        }
    }
}
