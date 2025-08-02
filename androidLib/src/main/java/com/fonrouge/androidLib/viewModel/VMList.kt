package com.fonrouge.androidLib.viewModel

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fonrouge.androidLib.commonServices.AppApi
import com.fonrouge.androidLib.domain.BasePagingSource
import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.common.toIApiItem
import com.fonrouge.fsLib.model.apiData.ApiItem
import com.fonrouge.fsLib.model.apiData.ApiList
import com.fonrouge.fsLib.model.apiData.IApiFilter
import com.fonrouge.fsLib.model.apiData.IApiItem
import com.fonrouge.fsLib.model.base.BaseDoc
import com.fonrouge.fsLib.model.state.ItemState
import com.fonrouge.fsLib.model.state.ListState
import com.fonrouge.fsLib.model.state.SimpleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction1

abstract class VMList<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>>(
    apiFilter: FILT,
    final override val commonContainer: CC,
    val listStateFun: KSuspendFunction1<ApiList<FILT>, ListState<T>>,
    val itemStateFun: KSuspendFunction1<IApiItem<T, ID, FILT>, ItemState<T>>? = null,
) : VMContainer<CC, T, ID, FILT>() {
    companion object {
        var lastRequest: Long = 0L
    }

    override var apiFilter: FILT by mutableStateOf(
        Json.decodeFromString(
            commonContainer.apiFilterSerializer,
            Json.encodeToString(commonContainer.apiFilterSerializer, apiFilter)
        )
    )
    private var filterBacking: FILT? = null
    open val pageSize: MutableIntState = mutableIntStateOf(20)
    val refreshingList: MutableState<Boolean> = mutableStateOf(false)
    var requestRefresh by mutableStateOf(false)
    var periodicUpdate by mutableStateOf(false)
    var periodicInterval by mutableIntStateOf(5000)
    var refreshListCounter by mutableIntStateOf(0)
    val refreshByFilter = mutableStateOf(false)
    open val onBeforeListStateGet: (() -> Unit)? = null

    @OptIn(ExperimentalGetImage::class)
    suspend fun listStateGetter(pageNum: Int): ListState<T> {
        if (AppApi.delayBeforeRequest > 0) delay(AppApi.delayBeforeRequest.toLong())
        onBeforeListStateGet?.invoke()
        lastRequest = System.currentTimeMillis()
        return listStateFun.invoke(
            ApiList(
                tabPage = pageNum,
                tabSize = pageSize.intValue,
                apiFilter = apiFilter
            )
        )
    }

    val flowPagingData: Flow<PagingData<T>> by lazy {
        Pager(
            config = PagingConfig(
                pageSize = pageSize.intValue,
            ),
            pagingSourceFactory = {
                BasePagingSource(
                    vmList = this,
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    open fun onEvent(uiBaseEvent: UIBaseEvent) {
        when (uiBaseEvent) {
            UIBaseEvent.EditingFilter -> {
                if (!refreshByFilter.value) {
                    filterBacking = apiFilter
                    refreshByFilter.value = true
                }
            }

            UIBaseEvent.RefreshByFilter -> {
                refreshByFilter.value = false
                if (filterBacking?.equals(apiFilter) != true) {
                    filterBacking = apiFilter
                    requestRefresh = true
                }
            }
        }
    }

    @Suppress("unused")
    suspend fun deleteItem(
        item: T,
    ) {
        itemStateFun?.let { itemStateFun ->
            val apiItem = ApiItem.Query.Delete<T, ID, FILT>(
                id = item._id,
                apiFilter = apiFilter,
            )
            var itemState: ItemState<T> = itemStateFun(commonContainer.toIApiItem(apiItem))
            if (itemState.hasError.not()) {
                itemState = itemStateFun(
                    commonContainer.toIApiItem(
                        ApiItem.Action.Delete(
                            item = item,
                            apiFilter = apiFilter,
                        )
                    ),
                )
                if (itemState.hasError) {
                    itemState.pushAlert()
                }
            } else {
                itemState.pushAlert()
            }
        } ?: run {
            pushStateAlert(
                itemState = SimpleState(
                    isOk = false,
                    msgError = "[itemStateFun] not defined in viewModel"
                )
            )
        }
    }

    sealed class UIBaseEvent {
        data object EditingFilter : UIBaseEvent()
        data object RefreshByFilter : UIBaseEvent()
    }
}
