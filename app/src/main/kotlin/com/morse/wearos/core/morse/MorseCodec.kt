package com.morse.wearos.core.morse

object MorseCodec {
    const val LETTER_GAP = " "
    const val WORD_GAP = "/"

    private val table: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        'Ñ' to "--.--",
    )

    fun encode(text: String): String =
        text.trim()
            .uppercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" $WORD_GAP ") { word ->
                word.mapNotNull { table[normalize(it)] }.joinToString(LETTER_GAP)
            }

    /** True when [c] (any case, accents allowed) has a Morse code; anything else is dropped by [encode]. */
    fun isEncodable(c: Char): Boolean = table.containsKey(normalize(c.uppercaseChar()))

    private fun normalize(c: Char): Char = when (c) {
        'Á' -> 'A'; 'É' -> 'E'; 'Í' -> 'I'; 'Ó' -> 'O'; 'Ú', 'Ü' -> 'U'
        else -> c
    }
}
