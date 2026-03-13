package com.example.showcase

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fonrouge.androidLib.commonServices.AppApi
import com.fonrouge.androidLib.commonServices.RouteRegistry
import com.fonrouge.base.api.ApiList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isDiscovering: Boolean = false,
    val error: String? = null,
    val contractVersion: String? = null,
)

class TaskListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState

    private val taskService = ITaskService()

    fun connectAndLoad(serverUrl: String) {
        viewModelScope.launch {
            _uiState.value = TaskListUiState(isDiscovering = true)

            // Configure the HTTP client
            AppApi.urlBase = serverUrl

            try {
                // Step 1: Discover routes via /apiContract
                RouteRegistry.discover()
                Log.d("Showcase", "Contract discovered: v${RouteRegistry.version}")

                _uiState.value = _uiState.value.copy(
                    isDiscovering = false,
                    isLoading = true,
                    contractVersion = RouteRegistry.version,
                )

                // Step 2: Fetch task list using JSON-RPC via discovered routes
                loadTasks()
            } catch (e: Exception) {
                Log.e("Showcase", "Error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isDiscovering = false,
                    isLoading = false,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    private suspend fun loadTasks() {
        val listState = taskService.apiList(
            ApiList(
                tabPage = 1,
                tabSize = 50,
                apiFilter = TaskFilter(),
            )
        )
        _uiState.value = _uiState.value.copy(
            tasks = listState.data,
            isLoading = false,
            error = if (listState.hasError) listState.msgError else null,
        )
    }
}
