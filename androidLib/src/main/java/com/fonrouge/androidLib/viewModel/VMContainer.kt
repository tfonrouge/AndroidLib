package com.fonrouge.androidLib.viewModel

import com.fonrouge.fsLib.common.ICommonContainer
import com.fonrouge.fsLib.model.apiData.IApiFilter
import com.fonrouge.fsLib.model.base.BaseDoc

abstract class VMContainer<CC : ICommonContainer<T, ID, FILT>, T : BaseDoc<ID>, ID : Any, FILT : IApiFilter<*>> :
    VMBase() {
    abstract val commonContainer: CC
    abstract var apiFilter: FILT
    fun apiFilterBuilder(): FILT = commonContainer.apiFilterInstance()
}

