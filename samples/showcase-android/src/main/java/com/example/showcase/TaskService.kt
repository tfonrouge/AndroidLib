package com.example.showcase

import com.fonrouge.androidLib.commonServices.call
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
 * The class name "ITaskService" must match the service interface name
 * used on the server, since [RouteRegistry] looks up routes by
 * `serviceName.methodName`.
 */
class ITaskService : ITaskServiceContract {

    override suspend fun apiList(apiList: ApiList<TaskFilter>): ListState<Task> =
        call("apiList", apiList)

    override suspend fun apiItem(iApiItem: IApiItem<Task, String, TaskFilter>): ItemState<Task> =
        call("apiItem", iApiItem)
}
