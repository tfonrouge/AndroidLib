package com.example.showcase

import com.fonrouge.androidLib.viewModel.VMItem

private val taskServiceProxy = TaskServiceProxy()

class TaskItemViewModel : VMItem<CommonTask, Task, String, TaskFilter>(
    commonContainer = CommonTask,
    itemStateFun = taskServiceProxy::apiItem,
)
