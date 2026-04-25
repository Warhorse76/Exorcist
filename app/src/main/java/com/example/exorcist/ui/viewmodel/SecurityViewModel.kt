package com.example.exorcist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exorcist.logic.SecurityAuditor
import com.example.exorcist.model.PrivilegedApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SecurityViewModel(private val auditor: SecurityAuditor) : ViewModel() {

    private val _privilegedApps = MutableStateFlow<List<PrivilegedApp>>(emptyList())
    val privilegedApps: StateFlow<List<PrivilegedApp>> = _privilegedApps

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.DISCONNECTED)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus

    fun checkShizuku() {
        if (!auditor.isShizukuAvailable()) {
            _shizukuStatus.value = ShizukuStatus.NOT_INSTALLED
            return
        }

        if (auditor.hasShizukuPermission()) {
            _shizukuStatus.value = ShizukuStatus.AUTHORIZED
            runAudit()
        } else {
            _shizukuStatus.value = ShizukuStatus.UNAUTHORIZED
        }
    }

    fun runAudit() {
        viewModelScope.launch {
            val apps = mutableListOf<PrivilegedApp>()
            apps.addAll(auditor.getAccessibilityApps())
            apps.addAll(auditor.getDeviceAdmins())
            if (auditor.hasShizukuPermission()) {
                apps.addAll(auditor.getOwnersViaShizuku())
            }
            _privilegedApps.value = apps.distinctBy { it.packageName + it.type }
        }
    }

    enum class ShizukuStatus {
        DISCONNECTED,
        NOT_INSTALLED,
        UNAUTHORIZED,
        AUTHORIZED
    }
}
