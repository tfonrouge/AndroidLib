package com.fonrouge.androidLib.services

import com.fonrouge.base.model.BaseDoc

suspend fun <T : BaseDoc<ID>, ID : Any> typedSelectService(): List<ID> {
    return emptyList()
}