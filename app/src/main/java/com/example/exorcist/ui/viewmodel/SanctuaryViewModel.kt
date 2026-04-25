package com.example.exorcist.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.exorcist.data.ExorcistDatabase
import com.example.exorcist.data.entity.ForensicLog
import com.example.exorcist.logic.SecurityAuditor
import com.example.exorcist.logic.settings.SanctuarySettings
import com.example.exorcist.model.PrivilegedApp
import com.example.exorcist.worker.ExfiltrationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SanctuaryViewModel(
    private val settings: SanctuarySettings,
    private val auditor: SecurityAuditor,
    private val database: ExorcistDatabase
) : ViewModel() {

    private val _recoveryEmail = MutableStateFlow("")
    val recoveryEmail: StateFlow<String> = _recoveryEmail.asStateFlow()

    private val _autoExfil = MutableStateFlow(false)
    val autoExfil: StateFlow<Boolean> = _autoExfil.asStateFlow()

    private val _owners = MutableStateFlow<List<PrivilegedApp>>(emptyList())
    val owners: StateFlow<List<PrivilegedApp>> = _owners.asStateFlow()

    private val _isDeprovisioning = MutableStateFlow(false)
    val isDeprovisioning: StateFlow<Boolean> = _isDeprovisioning.asStateFlow()

    init {
        viewModelScope.launch {
            settings.recoveryEmail.collect { _recoveryEmail.value = it ?: "" }
        }
        viewModelScope.launch {
            settings.autoExfil.collect { _autoExfil.value = it }
        }
        loadOwners()
    }

    fun loadOwners() {
        viewModelScope.launch {
            _owners.value = auditor.getOwnersViaShizuku()
        }
    }

    fun updateEmail(email: String) {
        viewModelScope.launch { settings.updateRecoveryEmail(email) }
    }

    fun updateAutoExfil(enabled: Boolean) {
        viewModelScope.launch { settings.updateAutoExfil(enabled) }
    }

    fun triggerManualExfiltration(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ExfiltrationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun deprovision(packageName: String) {
        viewModelScope.launch {
            _isDeprovisioning.value = true
            val success = auditor.deprovisionProfile(packageName)
            if (success) {
                database.forensicLogDao().insert(
                    ForensicLog(
                        type = "COMPROMISE_RECOVERY",
                        content = "Successfully de-provisioned potential malicious owner: $packageName"
                    )
                )
                loadOwners()
            }
            _isDeprovisioning.value = false
        }
    }
}
