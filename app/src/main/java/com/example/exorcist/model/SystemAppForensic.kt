package com.example.exorcist.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemAppForensic(
    val packageName: String,
    val appName: String,
    val installSource: String,
    val lastUpdateTime: Long,
    val lastUsedTime: Long? = null
)
