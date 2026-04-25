package com.example.exorcist.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exorcist.logic.SecurityAuditor
import com.example.exorcist.model.NetworkConnection
import com.example.exorcist.service.ForensicTunnel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NetworkViewModel(private val auditor: SecurityAuditor) : ViewModel() {

    private val _connections = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val connections: StateFlow<List<NetworkConnection>> = _connections

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive

    private var monitoringActive = false

    fun startMonitoring() {
        if (monitoringActive) return
        monitoringActive = true
        viewModelScope.launch {
            while (monitoringActive) {
                val current = auditor.getActiveConnections()
                _connections.value = current
                delay(3000)
            }
        }
    }

    fun stopMonitoring() {
        monitoringActive = false
    }

    fun toggleVpn(context: Context, prepareResult: (Intent?) -> Unit) {
        if (_isVpnActive.value) {
            val intent = Intent(context, ForensicTunnel::class.java).apply {
                action = ForensicTunnel.ACTION_STOP
            }
            context.startService(intent)
            _isVpnActive.value = false
        } else {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                prepareResult(intent)
            } else {
                startVpnService(context)
            }
        }
    }

    fun startVpnService(context: Context) {
        val intent = Intent(context, ForensicTunnel::class.java)
        context.startService(intent)
        _isVpnActive.value = true
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
