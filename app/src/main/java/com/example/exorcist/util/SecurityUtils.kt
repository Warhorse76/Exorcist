package com.example.exorcist.util

import android.content.Context
import android.os.Debug
import android.util.Base64
import java.util.*

object SecurityUtils {

    /**
     * Simple XOR-based string protection to frustrate static analysis.
     * Use this for sensitive strings like internal keys or API endpoints.
     */
    fun decrypt(encoded: String, key: String): String {
        val data = Base64.decode(encoded, Base64.DEFAULT)
        val result = ByteArray(data.size)
        val keyBytes = key.toByteArray()
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return String(result)
    }

    /**
     * Basic integrity check: detects if a debugger is attached.
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Basic check for common tampering signs.
     */
    fun isTampered(context: Context): Boolean {
        // In a real app, check signing certificate SHA-256
        // For this demo, we'll return false to allow development
        return false
    }
}
