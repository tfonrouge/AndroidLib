package com.fonrouge.androidLib.viewModel

import com.fonrouge.base.api.ApiItem
import com.fonrouge.base.api.CrudTask
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.common.toIApiItem
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.SimpleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction1

abstract class VMItem<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>>(
    final override val commonContainer: CC,
    val itemStateFun: KSuspendFunction1<IApiItem<T, ID, FILT>, ItemState<T>>,
) : VMContainer<CC, T, ID, FILT>() {
    private val _item = MutableStateFlow<T?>(null)
    var item: T?
        get() = _item.value
        set(value) { _item.value = value }
    val itemFlow: StateFlow<T?> = _item.asStateFlow()

    private val _crudTask = MutableStateFlow(CrudTask.Read)
    var crudTask: CrudTask
        get() = _crudTask.value
        set(value) { _crudTask.value = value }
    val crudTaskFlow: StateFlow<CrudTask> = _crudTask.asStateFlow()

    private val _itemAlreadyOn = MutableStateFlow<Boolean?>(null)
    var itemAlreadyOn: Boolean?
        get() = _itemAlreadyOn.value
        set(value) { _itemAlreadyOn.value = value }
    val itemAlreadyOnFlow: StateFlow<Boolean?> = _itemAlreadyOn.asStateFlow()

    private val _controlsEnabled = MutableStateFlow(false)
    var controlsEnabled: Boolean
        get() = _controlsEnabled.value
        set(value) { _controlsEnabled.value = value }
    val controlsEnabledFlow: StateFlow<Boolean> = _controlsEnabled.asStateFlow()

    override var apiFilter: FILT = commonContainer.apiFilterInstance()
    suspend fun makeQueryCall(
        id: ID? = null,
        crudTask: CrudTask = CrudTask.Read,
        onDone: VMContainer<CC, T, ID, FILT>.(ItemState<T>) -> Unit,
    ) {
        val serializedId = id?.let { Json.encodeToString(commonContainer.idSerializer, id) }
        val apiItem: ApiItem<T, ID, FILT>? = when (crudTask) {
            CrudTask.Create -> ApiItem.Query.Create(
                id = id,
                apiFilter = apiFilter,
            )

            CrudTask.Read -> serializedId?.let {
                ApiItem.Query.Read(
                    id = id,
                    apiFilter = apiFilter,
                )
            }

            CrudTask.Update -> serializedId?.let {
                ApiItem.Query.Update(
                    id = id,
                    apiFilter = apiFilter,
                )
            }

            CrudTask.Delete -> serializedId?.let {
                ApiItem.Query.Delete(
                    id = id,
                    apiFilter = apiFilter,
                )
            }
        }
        apiItem ?: run {
            SimpleState(
                isOk = false,
                msgError = "${commonContainer.labelItem} id null"
            ).pushAlert()
            return
        }
        return makeQueryCall(
            apiItem = apiItem,
            onDone = onDone
        )
    }

    suspend fun makeQueryCall(
        apiItem: ApiItem<T, ID, FILT>,
        onDone: VMContainer<CC, T, ID, FILT>.(ItemState<T>) -> Unit,
    ) {
        crudTask = apiItem.crudTask
        apiFilter = apiItem.apiFilter
        itemAlreadyOn = null
        val itemState = itemStateFun(commonContainer.toIApiItem(apiItem))
        if (crudTask == CrudTask.Create) {
            itemAlreadyOn = itemState.itemAlreadyOn
            if (itemAlreadyOn == true)
                crudTask = CrudTask.Update
            itemAlreadyOn = null
        }
        item = itemState.item
        controlsEnabled = when (crudTask) {
            CrudTask.Create -> true
            CrudTask.Read -> false
            CrudTask.Update -> true
            CrudTask.Delete -> false
        }
        onDone(itemState)
    }

    suspend fun makeActionCall(
        onDone: VMContainer<CC, T, ID, FILT>.(ItemState<T>) -> Unit,
    ) {
        val item = this.item ?: run {
            SimpleState(
                isOk = false,
                msgError = "${commonContainer.labelItem} item null"
            )
            return
        }
        val apiItem: ApiItem<T, ID, FILT> = when (crudTask) {
            CrudTask.Create -> ApiItem.Action.Create(
                item = item,
                apiFilter = apiFilter,
            )

            CrudTask.Read -> return
            CrudTask.Update -> ApiItem.Action.Update(
                item = item,
                apiFilter = apiFilter,
            )

            CrudTask.Delete -> ApiItem.Action.Delete(
                item = item,
                apiFilter = apiFilter,
            )
        }
        onDone(itemStateFun(commonContainer.toIApiItem(apiItem)))
    }
}
