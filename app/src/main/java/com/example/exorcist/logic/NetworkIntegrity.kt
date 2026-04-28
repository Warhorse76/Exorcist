package com.example.exorcist.logic

import com.example.exorcist.model.ArpEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NetworkIntegrity(private val auditor: SecurityAuditor) {

    private val _alerts = MutableSharedFlow<String>()
    val alerts: SharedFlow<String> = _alerts.asSharedFlow()

    private var gatewayMac: String? = null
    private var gatewayIp: String? = null

    suspend fun checkIntegrity(): List<String> {
        val currentAlerts = mutableListOf<String>()
        val arpTable = auditor.getArpTable()
        val currentGatewayIp = auditor.getDefaultGatewayIp()

        if (currentGatewayIp == null) return emptyList()

        // 1. Monitor Default Gateway MAC address
        val gatewayEntry = arpTable.find { it.ipAddress == currentGatewayIp }
        if (gatewayEntry != null) {
            if (gatewayMac != null && gatewayEntry.macAddress != gatewayMac) {
                val msg = "MITM ALERT: Gateway MAC changed from $gatewayMac to ${gatewayEntry.macAddress}"
                currentAlerts.add(msg)
                _alerts.emit(msg)
            }
            gatewayMac = gatewayEntry.macAddress
            gatewayIp = currentGatewayIp
        }

        // 2. Detect ARP Poisoning (Multiple IPs same MAC)
        val macGroups = arpTable.groupBy { it.macAddress }
        for ((mac, entries) in macGroups) {
            if (entries.size > 1 && mac != "00:00:00:00:00:00") {
                val ips = entries.joinToString(", ") { it.ipAddress }
                val msg = "MITM ALERT: Multiple IPs ($ips) sharing same MAC: $mac"
                currentAlerts.add(msg)
                _alerts.emit(msg)
            }
        }

        return currentAlerts
    }
}
