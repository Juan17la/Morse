package com.morse.wearos.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.morse.wearos.domain.Member
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.identityDataStore: DataStore<Preferences> by preferencesDataStore(name = "morse_identity")

class IdentityRepository(private val context: Context) {

    val displayName: Flow<String> = context.identityDataStore.data.map { prefs ->
        prefs[KEY_NAME_OVERRIDE]?.takeIf { it.isNotBlank() } ?: deviceName()
    }

    val hasNameOverride: Flow<Boolean> = context.identityDataStore.data.map { prefs ->
        !prefs[KEY_NAME_OVERRIDE].isNullOrBlank()
    }

    suspend fun setNameOverride(name: String?) {
        context.identityDataStore.edit { prefs ->
            val trimmed = name?.trim()
            if (trimmed.isNullOrEmpty()) prefs.remove(KEY_NAME_OVERRIDE) else prefs[KEY_NAME_OVERRIDE] = trimmed
        }
    }

    suspend fun memberId(): String {
        val existing = context.identityDataStore.data.first()[KEY_MEMBER_ID]
        if (existing != null) return existing
        val fresh = UUID.randomUUID().toString()
        context.identityDataStore.edit { prefs -> prefs[KEY_MEMBER_ID] = fresh }
        return fresh
    }

    suspend fun self(isHost: Boolean = false): Member =
        Member(id = memberId(), name = displayName.first(), isHost = isHost)

    private fun deviceName(): String =
        Settings.Global.getString(context.contentResolver, "device_name")?.takeIf { it.isNotBlank() }
            ?: Build.MODEL

    private companion object {
        val KEY_MEMBER_ID = stringPreferencesKey("member_id")
        val KEY_NAME_OVERRIDE = stringPreferencesKey("name_override")
    }
}
