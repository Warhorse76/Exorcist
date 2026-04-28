package com.example.exorcist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exorcist.logic.NetworkIntegrity
import com.example.exorcist.logic.SecurityAuditor
import com.example.exorcist.logic.ShizukuScriptEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalViewModel(private val auditor: SecurityAuditor) : ViewModel() {

    private val integrity = NetworkIntegrity(auditor)
    private val scriptEngine = ShizukuScriptEngine(auditor, auditor.getContext())

    private val _history = MutableStateFlow<List<String>>(listOf("Exorcist Ashell v1.0", "Type 'help' for available commands.", ""))
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        viewModelScope.launch {
            integrity.alerts.collect { alert ->
                addToHistory("[!] $alert")
            }
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            addToHistory("> $command")
            
            val result = scriptEngine.executeScript(command)
            addToHistory(result)
            
            _isProcessing.value = false
        }
    }

    private fun addToHistory(text: String) {
        _history.value = _history.value + text.split("\n")
    }
}
