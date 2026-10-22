package com.morse.wearos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortNameTest {

    @Test
    fun keepsFirstWordAndCutsLongOnes() {
        assertEquals("Ana", shortName("Ana"))
        assertEquals("Juan", shortName("  Juan Diego's Galaxy Watch "))
        assertEquals("Maximilia…", shortName("Maximiliano Pérez"))
        assertEquals("?", shortName("   "))
        assertEquals("Bartholome", shortName("Bartholome"))
    }
}
