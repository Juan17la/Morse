package com.morse.wearos.ai

import com.morse.wearos.data.WordsRepository
import com.morse.wearos.domain.MorseMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseAssistantTest {

    @Test
    fun sanitizeUppercasesAndDropsWhatMorseCannotCarry() {
        assertEquals("OK SEE YOU", MorseAssistant.sanitize("ok, see you! 👋"))
        assertEquals("SÍ", MorseAssistant.sanitize("sí"))
        assertEquals("I'M HERE", MorseAssistant.sanitize("I'm here"))
    }

    @Test
    fun sanitizeCutsAtWordBoundaryWithinLimit() {
        val out = MorseAssistant.sanitize("running late meet at the north entrance")!!
        assertTrue(out.length <= WordsRepository.MAX_WORD_LENGTH)
        assertEquals("RUNNING LATE MEET AT", out)
    }

    @Test
    fun sanitizeRejectsOutputWithNothingToVibrate() {
        assertNull(MorseAssistant.sanitize("🙂 ?! "))
    }

    @Test
    fun offlineCondenseKeepsContentWords() {
        val out = MorseAssistant.offlineCondense("I'm going to be 10 minutes late, wait for me at the entrance")
        assertTrue(out.length <= WordsRepository.MAX_WORD_LENGTH)
        assertEquals("10 MIN LATE WAIT", out)
    }

    @Test
    fun offlineCondenseWorksInSpanish() {
        val out = MorseAssistant.offlineCondense("Estoy en la puerta de la biblioteca")
        assertEquals("PUERTA BIBLIOTECA", out)
    }

    @Test
    fun offlineRepliesFitTheMessage() {
        assertEquals(listOf("YES", "NO", "MAYBE"), MorseAssistant.offlineReplies("Coming?", "en"))
        assertEquals(listOf("I'M HERE", "HOME", "ON MY WAY"), MorseAssistant.offlineReplies("Where are you", "en"))
        assertEquals(listOf("VOY", "DONDE", "LLAMO 911"), MorseAssistant.offlineReplies("SOS", "es"))
        assertEquals(listOf("SI", "NO", "QUIZAS"), MorseAssistant.offlineReplies("¿Vienes?", "es"))
    }

    @Test
    fun withoutKeyEverythingFallsBackOffline() = runTest {
        val assistant = MorseAssistant(apiKey = "", language = "en")
        assertFalse(assistant.condense("see you at the gym at 5").fromAi)
        val msg = MorseMessage("1", "a", "Ana", "Where", ".-- ....", 0)
        val replies = assistant.replies(msg, listOf(msg))
        assertFalse(replies.fromAi)
        assertEquals(3, replies.options.size)
    }

    @Test
    fun parsesGeminiResponse() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"{\"options\":[\"OK\",\"5 MIN\"]}"}]}}]}"""
        assertEquals(listOf("OK", "5 MIN"), MorseAssistant.parseOptions(body))
    }
}
