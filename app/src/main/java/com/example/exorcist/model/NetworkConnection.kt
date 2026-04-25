package com.example.exorcist.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkConnection(
    val localAddress: String,
    val remoteAddress: String,
    val state: String,
    val uid: Int,
    val packageName: String?,
    val appName: String?
)
