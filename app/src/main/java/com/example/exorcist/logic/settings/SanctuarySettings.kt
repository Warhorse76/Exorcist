package com.example.exorcist.logic.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sanctuary_settings")

class SanctuarySettings(private val context: Context) {

    private val RECOVERY_EMAIL_KEY = stringPreferencesKey("recovery_email")
    private val AUTO_EXFIL_KEY = booleanPreferencesKey("auto_exfil")

    val recoveryEmail: Flow<String?> = context.dataStore.data.map { it[RECOVERY_EMAIL_KEY] }
    val autoExfil: Flow<Boolean> = context.dataStore.data.map { it[AUTO_EXFIL_KEY] ?: false }

    suspend fun updateRecoveryEmail(email: String) {
        context.dataStore.edit { it[RECOVERY_EMAIL_KEY] = email }
    }

    suspend fun updateAutoExfil(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_EXFIL_KEY] = enabled }
    }
}
