package com.example.showcase

import com.fonrouge.fslib.android.viewModel.VMList
import com.fonrouge.base.api.ApiList
import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.ListState

private val taskServiceProxy = TaskServiceProxy()

class TaskListViewModel(
    apiFilter: TaskFilter = TaskFilter(),
) : VMList<CommonTask, Task, String, TaskFilter>(
    apiFilter = apiFilter,
    commonContainer = CommonTask,
    listStateFun = taskServiceProxy::apiList,
    itemStateFun = taskServiceProxy::apiItem,
)
