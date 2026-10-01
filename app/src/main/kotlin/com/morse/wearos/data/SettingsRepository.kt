package com.morse.wearos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class TransportMode { LAN, WIFI_DIRECT }

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "morse_settings")

class SettingsRepository(private val context: Context) {

    val transportMode: Flow<TransportMode> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_TRANSPORT]?.let { runCatching { TransportMode.valueOf(it) }.getOrNull() } ?: DEFAULT_TRANSPORT
    }

    val lanHostAddress: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_LAN_HOST] ?: DEFAULT_LAN_HOST
    }

    suspend fun setTransportMode(mode: TransportMode) {
        context.settingsDataStore.edit { it[KEY_TRANSPORT] = mode.name }
    }

    suspend fun setLanHostAddress(address: String) {
        context.settingsDataStore.edit { it[KEY_LAN_HOST] = address.trim().ifEmpty { DEFAULT_LAN_HOST } }
    }

    companion object {
        val DEFAULT_TRANSPORT = TransportMode.WIFI_DIRECT
        // 10.0.2.2 is the host machine as seen from an Android emulator.
        const val DEFAULT_LAN_HOST = "10.0.2.2"
        private val KEY_TRANSPORT = stringPreferencesKey("transport_mode")
        private val KEY_LAN_HOST = stringPreferencesKey("lan_host")
    }
}
