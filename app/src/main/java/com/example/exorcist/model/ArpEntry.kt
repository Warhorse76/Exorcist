package com.example.exorcist.model

import kotlinx.serialization.Serializable

@Serializable
data class ArpEntry(
    val ipAddress: String,
    val macAddress: String,
    val device: String,
    val state: String
)
