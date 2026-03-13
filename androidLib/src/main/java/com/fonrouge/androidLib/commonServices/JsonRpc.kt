package com.fonrouge.androidLib.commonServices

import kotlinx.serialization.Serializable

@Serializable
data class JsonRpcRequest(
    val id: Int,
    val method: String,
    val params: List<String?>,
    val jsonrpc: String = "2.0",
)

@Serializable
data class JsonRpcResponse(
    val id: Int? = null,
    val result: String? = null,
    val error: String? = null,
    val exceptionType: String? = null,
    val exceptionJson: String? = null,
    val jsonrpc: String = "2.0",
)

class RpcException(
    message: String,
    val exceptionType: String? = null,
    val exceptionJson: String? = null,
) : Exception(message)
