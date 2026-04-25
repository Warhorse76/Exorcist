package com.example.exorcist.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Audit : Destination
    
    @Serializable
    data object NetworkMonitor : Destination
    
    @Serializable
    data object ForensicLog : Destination

    @Serializable
    data object Sanctuary : Destination
}
