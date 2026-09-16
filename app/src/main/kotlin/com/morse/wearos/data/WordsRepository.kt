package com.morse.wearos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.wordsDataStore: DataStore<Preferences> by preferencesDataStore(name = "morse_words")

class WordsRepository(private val context: Context) {

    val customWords: Flow<List<String>> = context.wordsDataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_WORDS]?.let { decode(it) } ?: emptyList()
    }

    suspend fun add(raw: String): Boolean {
        val word = normalize(raw) ?: return false
        var added = false
        context.wordsDataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_WORDS]?.let { decode(it) } ?: emptyList()
            if (current.none { it.equals(word, ignoreCase = true) }) {
                prefs[KEY_CUSTOM_WORDS] = Json.encodeToString(current + word)
                added = true
            }
        }
        return added
    }

    suspend fun remove(word: String) {
        context.wordsDataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_WORDS]?.let { decode(it) } ?: return@edit
            prefs[KEY_CUSTOM_WORDS] = Json.encodeToString(current.filterNot { it == word })
        }
    }

    private fun normalize(raw: String): String? {
        val word = raw.trim().replace(Regex("\\s+"), " ")
        if (word.isEmpty() || word.length > MAX_WORD_LENGTH) return null
        return word
    }

    private fun decode(json: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(json) }.getOrDefault(emptyList())

    companion object {
        const val MAX_WORD_LENGTH = 20
        private val KEY_CUSTOM_WORDS = stringPreferencesKey("custom_words")
    }
}
