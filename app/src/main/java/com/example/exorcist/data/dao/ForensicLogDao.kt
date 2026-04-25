package com.example.exorcist.data.dao

import androidx.room.*
import com.example.exorcist.data.entity.ForensicLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ForensicLogDao {
    @Insert
    suspend fun insert(log: ForensicLog)

    @Query("SELECT * FROM forensic_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ForensicLog>>

    @Query("SELECT * FROM forensic_logs WHERE isExfiltrated = 0")
    suspend fun getPendingLogs(): List<ForensicLog>

    @Update
    suspend fun update(log: ForensicLog)

    @Query("DELETE FROM forensic_logs WHERE isExfiltrated = 1")
    suspend fun deleteExfiltratedLogs()
}
