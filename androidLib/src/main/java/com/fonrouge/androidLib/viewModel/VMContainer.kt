package com.fonrouge.androidLib.viewModel

import com.fonrouge.base.api.IApiFilter
import com.fonrouge.base.common.ICommonContainer
import com.fonrouge.base.model.BaseDoc

abstract class VMContainer<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> :
    VMBase() {
    abstract val commonContainer: CC
    abstract var apiFilter: FILT
    fun apiFilterBuilder(): FILT = commonContainer.apiFilterInstance()
}

