package com.example.exorcist.model

import kotlinx.serialization.Serializable

@Serializable
data class ChameleonResult(
    val packageName: String,
    val appLabel: String,
    val suspicionScore: Int,
    val riskLevel: String,
    val reasons: List<String>
)
