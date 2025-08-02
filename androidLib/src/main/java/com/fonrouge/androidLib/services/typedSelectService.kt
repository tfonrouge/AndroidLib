package com.fonrouge.androidLib.services

import com.fonrouge.fsLib.model.base.BaseDoc

suspend fun <T: BaseDoc<ID>, ID: Any> typedSelectService() : List<ID> {
    return emptyList()
}