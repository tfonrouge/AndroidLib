package com.example.showcase

import com.fonrouge.fslib.android.commonServices.IServiceProxy
import com.fonrouge.fslib.android.commonServices.call
import com.fonrouge.base.api.ApiList
import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.ListState

/**
 * Android-side proxy for the showcase ITaskService.
 *
 * Implements [ITaskServiceContract] from showcase-lib for compile-time
 * validation that method signatures match the server's @RpcService interface.
 *
 * The [serviceName] explicitly maps to the server-side interface name,
 * decoupling this class's name from the route lookup.
 */
class TaskServiceProxy : ITaskServiceContract, IServiceProxy {

    override val serviceName: String = "ITaskService"

    override suspend fun apiList(apiList: ApiList<TaskFilter>): ListState<Task> =
        call(apiList)

    override suspend fun apiItem(iApiItem: IApiItem<Task, String, TaskFilter>): ItemState<Task> =
        call(iApiItem)
}
