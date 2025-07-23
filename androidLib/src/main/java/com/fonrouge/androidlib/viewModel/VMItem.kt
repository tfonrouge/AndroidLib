package com.fonrouge.androidlib.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.common.toIApiItem
import com.fonrouge.fsLib.model.apiData.ApiItem
import com.fonrouge.fsLib.model.apiData.CrudTask
import com.fonrouge.fsLib.model.apiData.IApiFilter
import com.fonrouge.fsLib.model.apiData.IApiItem
import com.fonrouge.fsLib.model.base.BaseDoc
import com.fonrouge.fsLib.model.state.ItemState
import com.fonrouge.fsLib.model.state.SimpleState
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction1

@Suppress("unused")
abstract class VMItem<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>>(
    final override val commonContainer: CC,
    val itemStateFun: KSuspendFunction1<IApiItem<T, ID, FILT>, ItemState<T>>,
) : VMContainer<CC, T, ID, FILT>() {
    var item: T? by mutableStateOf(null)
    var crudTask: CrudTask by mutableStateOf(CrudTask.Read)
    var itemAlreadyOn by mutableStateOf<Boolean?>(null)
    var controlsEnabled by mutableStateOf(false)
    override var apiFilter: FILT = commonContainer.apiFilterInstance()
    suspend fun makeQueryCall(
        id: ID? = null,
        crudTask: CrudTask = CrudTask.Read,
        onDone: VMContainer<CC, T, ID, FILT>.(ItemState<T>) -> Unit,
    ) {
        val serializedId = id?.let { Json.encodeToString(commonContainer.idSerializer, id) }
        val apiItem: ApiItem<T, ID, FILT>? = when (crudTask) {
            CrudTask.Create -> ApiItem.Upsert.Create.Query(
                id = id,
                apiFilter = apiFilter,
            )

            CrudTask.Read -> serializedId?.let {
                ApiItem.Read(
                    id = id,
                    apiFilter = apiFilter,
                )
            }

            CrudTask.Update -> serializedId?.let {
                ApiItem.Upsert.Update.Query(
                    id = id,
                    apiFilter = apiFilter,
                )
            }

            CrudTask.Delete -> TODO()
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

    @Suppress("unused")
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

    @Suppress("unused")
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
            CrudTask.Create -> ApiItem.Upsert.Create.Action(
                item = item,
                apiFilter = apiFilter,
            )

            CrudTask.Read -> return
            CrudTask.Update -> ApiItem.Upsert.Update.Action(
                item = item,
                apiFilter = apiFilter,
                orig = null,
            )

            CrudTask.Delete -> ApiItem.Delete.Action(
                item = item,
                apiFilter = apiFilter,
            )
        }
        onDone(itemStateFun(commonContainer.toIApiItem(apiItem)))
    }
}
