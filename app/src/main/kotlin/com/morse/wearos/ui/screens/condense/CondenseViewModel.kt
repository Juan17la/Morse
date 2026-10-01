package com.morse.wearos.ui.screens.condense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.ai.MorseAssistant
import com.morse.wearos.ai.Suggestion
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.data.WordsRepository
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CondenseUiState(
    val original: String,
    /** The dictated text as-is, when it already fits a message; null otherwise. */
    val originalSendable: String?,
    val originalSeconds: Int,
    val suggestion: Suggestion? = null,
    val suggestionSeconds: Int = 0,
)

/** Seconds the watch vibrates to play [text] as Morse. */
fun vibrationSeconds(text: String): Int =
    ((MorseTiming.steps(MorseCodec.encode(text)).sumOf { it.durationMs } + 999) / 1000).toInt()

class CondenseViewModel(
    private val session: GroupSession,
    assistant: MorseAssistant,
    text: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CondenseUiState(
            original = text,
            originalSendable = text.trim().takeIf { it.length <= WordsRepository.MAX_WORD_LENGTH }
                ?.let(MorseAssistant::sanitize),
            originalSeconds = vibrationSeconds(text),
        )
    )
    val uiState: StateFlow<CondenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val suggestion = assistant.condense(text)
            _uiState.value = _uiState.value.copy(
                suggestion = suggestion,
                suggestionSeconds = vibrationSeconds(suggestion.text),
            )
        }
    }

    fun send(word: String, onSent: () -> Unit) {
        viewModelScope.launch {
            session.send(word)
            onSent()
        }
    }
}
