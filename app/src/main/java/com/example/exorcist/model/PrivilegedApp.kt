package com.example.exorcist.model

import kotlinx.serialization.Serializable

@Serializable
data class PrivilegedApp(
    val packageName: String,
    val name: String,
    val type: String,
    val isSystemApp: Boolean = false
)
