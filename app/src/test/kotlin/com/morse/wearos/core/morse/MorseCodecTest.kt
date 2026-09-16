package com.morse.wearos.core.morse

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseCodecTest {

    @Test
    fun encodesSingleWord() {
        assertEquals("... --- ...", MorseCodec.encode("SOS"))
    }

    @Test
    fun isCaseInsensitive() {
        assertEquals(MorseCodec.encode("YES"), MorseCodec.encode("yes"))
    }

    @Test
    fun separatesWordsWithSlash() {
        assertEquals(".. -- / .... . .-. .", MorseCodec.encode("im here"))
    }

    @Test
    fun collapsesWhitespaceAndTrims() {
        assertEquals(".. -- / .... . .-. .", MorseCodec.encode("  im   here "))
    }

    @Test
    fun dropsUnsupportedCharacters() {
        assertEquals(".. --", MorseCodec.encode("I'm!"))
    }

    @Test
    fun encodesDigits() {
        assertEquals(".---- ..---", MorseCodec.encode("12"))
    }

    @Test
    fun stripsSpanishAccents() {
        assertEquals(MorseCodec.encode("SI"), MorseCodec.encode("Sí"))
    }

    @Test
    fun emptyInputGivesEmptyOutput() {
        assertEquals("", MorseCodec.encode("   "))
    }
}
