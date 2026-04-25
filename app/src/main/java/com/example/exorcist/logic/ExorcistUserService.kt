package com.example.exorcist.logic

import com.example.exorcist.IExorcistService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class ExorcistUserService : IExorcistService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = reader.readText()
            val error = errorReader.readText()
            process.waitFor()
            if (error.isNotEmpty()) "$output\nError: $error" else output
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }
    }
}
