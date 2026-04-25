package com.example.exorcist.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forensic_logs")
data class ForensicLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "AUDIT", "NETWORK", "COMPROMISE"
    val content: String,
    val isExfiltrated: Boolean = false
)
