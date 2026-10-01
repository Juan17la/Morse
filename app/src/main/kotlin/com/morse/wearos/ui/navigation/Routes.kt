package com.morse.wearos.ui.navigation

object Routes {
    const val HOME = "home"
    const val JOIN = "join"
    const val GROUP = "group"
    const val SEND = "send"
    const val INCOMING = "incoming"
    const val WORDS = "words"
    const val SETTINGS = "settings"

    const val CONDENSE_ARG = "text"
    const val CONDENSE = "condense/{$CONDENSE_ARG}"
    fun condense(text: String): String = "condense/${android.net.Uri.encode(text)}"
}
