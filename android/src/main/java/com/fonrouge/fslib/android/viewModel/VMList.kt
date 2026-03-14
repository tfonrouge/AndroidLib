package com.fonrouge.fslib.android.viewModel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fonrouge.fslib.android.commonServices.appApi
import com.fonrouge.fslib.android.domain.BasePagingSource
import com.fonrouge.base.api.ApiItem
import com.fonrouge.base.api.ApiList
import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.api.IApiItem
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.common.toIApiItem
import com.fonrouge.base.model.BaseDoc
import com.fonrouge.base.state.ItemState
import com.fonrouge.base.state.ListState
import com.fonrouge.base.state.SimpleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.reflect.KSuspendFunction1

abstract class VMList<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>>(
    apiFilter: FILT,
    final override val commonContainer: CC,
    val listStateFun: KSuspendFunction1<ApiList<FILT>, ListState<T>>,
    val itemStateFun: KSuspendFunction1<IApiItem<T, ID, FILT>, ItemState<T>>? = null,
) : VMContainer<CC, T, ID, FILT>() {
    var lastRequest: Long = 0L

    private val _apiFilter = MutableStateFlow(
        Json.decodeFromString(
            commonContainer.apiFilterSerializer,
            Json.encodeToString(commonContainer.apiFilterSerializer, apiFilter)
        )
    )
    override var apiFilter: FILT
        get() = _apiFilter.value
        set(value) { _apiFilter.value = value }
    val apiFilterFlow: StateFlow<FILT> = _apiFilter.asStateFlow()

    private var filterBacking: FILT? = null

    private val _pageSize = MutableStateFlow(20)
    open val pageSize: StateFlow<Int> = _pageSize.asStateFlow()
    fun setPageSize(value: Int) { _pageSize.value = value }

    private val _refreshingList = MutableStateFlow(false)
    val refreshingList: StateFlow<Boolean> = _refreshingList.asStateFlow()
    fun setRefreshingList(value: Boolean) { _refreshingList.value = value }

    private val _requestRefresh = MutableStateFlow(false)
    var requestRefresh: Boolean
        get() = _requestRefresh.value
        set(value) { _requestRefresh.value = value }
    val requestRefreshFlow: StateFlow<Boolean> = _requestRefresh.asStateFlow()

    private val _periodicUpdate = MutableStateFlow(false)
    var periodicUpdate: Boolean
        get() = _periodicUpdate.value
        set(value) { _periodicUpdate.value = value }

    private val _periodicInterval = MutableStateFlow(5000)
    var periodicInterval: Int
        get() = _periodicInterval.value
        set(value) { _periodicInterval.value = value }

    private val _refreshListCounter = MutableStateFlow(0)
    var refreshListCounter: Int
        get() = _refreshListCounter.value
        set(value) { _refreshListCounter.value = value }
    val refreshListCounterFlow: StateFlow<Int> = _refreshListCounter.asStateFlow()

    private val _refreshByFilter = MutableStateFlow(false)
    val refreshByFilter: StateFlow<Boolean> = _refreshByFilter.asStateFlow()
    fun setRefreshByFilter(value: Boolean) { _refreshByFilter.value = value }

    open val onBeforeListStateGet: (() -> Unit)? = null

    suspend fun listStateGetter(pageNum: Int): ListState<T> {
        if (appApi.delayBeforeRequest > 0) delay(appApi.delayBeforeRequest.toLong())
        onBeforeListStateGet?.invoke()
        lastRequest = System.currentTimeMillis()
        return listStateFun.invoke(
            ApiList(
                tabPage = pageNum,
                tabSize = _pageSize.value,
                apiFilter = apiFilter
            )
        )
    }

    val flowPagingData: Flow<PagingData<T>> by lazy {
        Pager(
            config = PagingConfig(
                pageSize = _pageSize.value,
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
                if (!_refreshByFilter.value) {
                    filterBacking = apiFilter
                    _refreshByFilter.value = true
                }
            }

            UIBaseEvent.RefreshByFilter -> {
                _refreshByFilter.value = false
                if (filterBacking?.equals(apiFilter) != true) {
                    filterBacking = apiFilter
                    requestRefresh = true
                }
            }
        }
    }

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
