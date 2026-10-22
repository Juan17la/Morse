package com.morse.wearos.ai

import android.util.Log
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.WordsRepository
import com.morse.wearos.domain.MorseMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** A short, Morse-friendly text and whether the model (rather than the offline fallback) wrote it. */
data class Suggestion(val text: String, val fromAi: Boolean)

/**
 * Keeps haptic Morse usable: every letter costs about a second of vibration, so long text has to be
 * condensed and replies should be one tap. Uses the Gemini API when a key is set and the watch is
 * online, and a deterministic offline fallback otherwise, so the flow never dead-ends.
 */
class MorseAssistant(
    private val apiKey: String,
    private val languageOverride: String? = null,
) {
    // Read on each call so an in-app language change applies without a restart.
    private val language: String get() = languageOverride ?: Locale.getDefault().language
    val isOnlineCapable: Boolean get() = apiKey.isNotBlank()

    suspend fun condense(text: String): Suggestion {
        val prompt = "Condense this message for haptic Morse: \"$text\""
        val ai = ask(prompt, count = 1).firstOrNull()
        return if (ai != null) Suggestion(ai, fromAi = true) else Suggestion(offlineCondense(text), fromAi = false)
    }

    suspend fun replies(received: MorseMessage, history: List<MorseMessage>): Suggestions {
        val conversation = history.takeLast(HISTORY_SIZE).joinToString("\n") { "${it.senderName}: ${it.word}" }
        val prompt = "Group conversation so far (oldest first):\n$conversation\n\n" +
            "Suggest 3 different replies to the latest message from ${received.senderName}: \"${received.word}\""
        val ai = ask(prompt, count = 3).distinct()
        return if (ai.size >= 2) Suggestions(ai.take(3), fromAi = true) else Suggestions(offlineReplies(received.word, language), fromAi = false)
    }

    data class Suggestions(val options: List<String>, val fromAi: Boolean)

    /** Returns sanitized options, or an empty list on any failure (no key, offline, quota, bad output). */
    private suspend fun ask(prompt: String, count: Int): List<String> {
        if (!isOnlineCapable) return emptyList()
        return runCatching { withContext(Dispatchers.IO) { post(request(prompt, count)) } }
            .map { body -> parseOptions(body).mapNotNull(::sanitize) }
            .onFailure { Log.w(TAG, "Gemini call failed, using offline fallback: ${it.message}") }
            .getOrDefault(emptyList())
    }

    private fun request(prompt: String, count: Int): JsonObject = buildJsonObject {
        putJsonObject("systemInstruction") {
            putJsonArray("parts") { addJsonObject { put("text", systemPrompt(count)) } }
        }
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") { addJsonObject { put("text", prompt) } }
            }
        }
        putJsonObject("generationConfig") {
            put("temperature", 0.4)
            put("responseMimeType", "application/json")
            putJsonObject("responseSchema") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("options") {
                        put("type", "ARRAY")
                        putJsonObject("items") { put("type", "STRING") }
                    }
                }
                putJsonArray("required") { add("options") }
            }
        }
    }

    private fun systemPrompt(count: Int): String = """
        You help people talk through smartwatches that vibrate each message as Morse code.
        Every letter costs about one second of vibration, so text must be as short as possible.
        Write telegraph style: drop articles and filler, use digits and common abbreviations (OK, MIN, ETA, ASAP).
        Each option must be at most ${WordsRepository.MAX_WORD_LENGTH} characters, letters, digits and spaces only, no emoji.
        Keep the meaning that matters (who, where, when, yes/no). Never invent facts.
        Answer in the language with ISO code "$language".
        Return JSON {"options": [...]} with exactly $count option(s).
    """.trimIndent()

    private fun post(body: JsonObject): String {
        val connection = URL("$ENDPOINT/$MODEL:generateContent").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "HTTP $code" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "MorseAssistant"
        // Free tier model from Google AI Studio; swap here if AI Studio lists a newer flash-lite.
        const val MODEL = "gemini-2.5-flash-lite"
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val TIMEOUT_MS = 6_000
        const val HISTORY_SIZE = 10

        private val json = Json { ignoreUnknownKeys = true }

        /** Pulls `{"options": [...]}` out of a generateContent response. */
        internal fun parseOptions(response: String): List<String> {
            val text = json.parseToJsonElement(response).jsonObject["candidates"]!!.jsonArray[0]
                .jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
            val options = json.parseToJsonElement(text).jsonObject["options"] as? JsonArray ?: return emptyList()
            return options.map { it.jsonPrimitive.content }
        }

        /**
         * Model output (and dictated text) is untrusted: keep only what Morse can carry, uppercase it and
         * fit it in a word slot, cutting at a word boundary when possible. Null when nothing usable is left.
         */
        fun sanitize(raw: String): String? {
            val kept = raw.uppercase()
                .map { if (MorseCodec.isEncodable(it) || it == '\'') it else ' ' }
                .joinToString("")
                .trim()
                .replace(Regex("\\s+"), " ")
            return fit(kept).takeIf { MorseCodec.encode(it).isNotEmpty() }
        }

        private fun fit(text: String): String {
            val max = WordsRepository.MAX_WORD_LENGTH
            if (text.length <= max) return text
            val cut = text.take(max + 1).lastIndexOf(' ')
            return if (cut > 0) text.take(cut) else text.take(max)
        }

        private val stopwords = setOf(
            // English
            "a", "an", "the", "i", "i'm", "im", "am", "is", "are", "be", "will", "would", "to", "of", "for", "and",
            "that", "this", "it", "just", "so", "very", "really", "going", "gonna", "you", "your", "my",
            "me", "we", "at", "in", "on", "be", "can", "could", "there", "some", "about",
            // Spanish
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "que", "y", "a", "al", "en", "por",
            "para", "con", "es", "estoy", "voy", "muy", "me", "te", "se", "lo", "le", "mi", "tu", "yo", "ya",
            "pues", "bueno", "favor",
        )

        private val abbreviations = mapOf(
            "minutes" to "MIN", "minute" to "MIN", "minutos" to "MIN", "minuto" to "MIN",
            "hours" to "H", "hour" to "H", "horas" to "H", "hora" to "H",
            "seconds" to "S", "segundos" to "S", "okay" to "OK", "tomorrow" to "TMRW", "please" to "PLS",
        )

        /** Drops filler words, abbreviates units and keeps as many of the rest as fit, in order. */
        fun offlineCondense(text: String): String {
            val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
                .map { abbreviations[it.lowercase().trim(',', '.', '!', '?')] ?: it }
            val content = words.filter { it.lowercase().trim(',', '.', '!', '?', '¿', '¡') !in stopwords }
            val packed = StringBuilder()
            for (word in content.ifEmpty { words }) {
                val clean = sanitize(word) ?: continue
                val next = if (packed.isEmpty()) clean else "$packed $clean"
                if (next.length > WordsRepository.MAX_WORD_LENGTH) break
                packed.clear().append(next)
            }
            return packed.toString().ifEmpty { sanitize(text) ?: "OK" }
        }

        /** Keyword rules so replies still fit the message when there is no model to ask. */
        fun offlineReplies(word: String, language: String = Locale.getDefault().language): List<String> {
            val es = language == "es"
            val w = " " + word.lowercase().replace(Regex("[^\\p{L}\\p{N}?¿ ]"), " ") + " "
            fun has(vararg keys: String) = keys.any { w.contains(" $it ") || w.contains(" $it?") }
            return when {
                has("sos", "help", "ayuda", "auxilio") ->
                    if (es) listOf("VOY", "DONDE", "LLAMO 911") else listOf("ON MY WAY", "WHERE", "CALL 911")
                has("where", "donde", "dónde") ->
                    if (es) listOf("AQUI ESTOY", "CASA", "VOY EN CAMINO") else listOf("I'M HERE", "HOME", "ON MY WAY")
                has("when", "cuando", "cuándo") ->
                    if (es) listOf("YA", "5 MIN", "LUEGO") else listOf("NOW", "5 MIN", "LATER")
                has("wait", "espera") ->
                    if (es) listOf("OK", "CUANTO", "NO PUEDO") else listOf("OK", "HOW LONG", "CAN'T")
                '?' in w || '¿' in w || has("are", "can", "do", "is", "will", "puedes", "vienes", "estas") ->
                    if (es) listOf("SI", "NO", "QUIZAS") else listOf("YES", "NO", "MAYBE")
                else -> if (es) listOf("OK", "GRACIAS", "ESPERA") else listOf("OK", "THANKS", "WAIT")
            }
        }
    }
}
