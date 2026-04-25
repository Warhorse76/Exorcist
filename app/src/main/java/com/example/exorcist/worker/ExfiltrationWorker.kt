package com.example.exorcist.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.exorcist.data.ExorcistDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class ExfiltrationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = ExorcistDatabase.getDatabase(applicationContext)
            val dao = db.forensicLogDao()
            val pendingLogs = dao.getPendingLogs()

            if (pendingLogs.isEmpty()) {
                return@withContext Result.success()
            }

            val payload = pendingLogs.joinToString("\n") { 
                "[${it.timestamp}] ${it.type}: ${it.content}" 
            }

            val encryptedPayload = encryptPayload(payload)
            
            // Mock Exfiltration to Trusted Recovery Email
            Log.d("ExfiltrationWorker", "EXFILTRATING ENCRYPTED DATA TO RECOVERY EMAIL")
            Log.d("ExfiltrationWorker", "Payload Size: ${payload.length} bytes")
            Log.d("ExfiltrationWorker", "Encrypted Blob: $encryptedPayload")

            // Mark as exfiltrated
            pendingLogs.forEach {
                dao.update(it.copy(isExfiltrated = true))
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("ExfiltrationWorker", "Exfiltration failed", e)
            Result.retry()
        }
    }

    private fun encryptPayload(data: String): String {
        // Simple AES-256 for proof of concept
        // Key should be derived from user secret or Keystore
        val key = "exorcist_exfil_key_32bytes_total!".toByteArray().copyOf(32)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }
}
