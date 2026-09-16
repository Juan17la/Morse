package com.morse.wearos.ui.screens.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.data.WordsRepository
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SendViewModel(
    private val session: GroupSession,
    wordsRepository: WordsRepository,
) : ViewModel() {

    val customWords: StateFlow<List<String>> = wordsRepository.customWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val connection: StateFlow<ConnectionState> = session.state

    fun send(word: String, onSent: () -> Unit) {
        viewModelScope.launch {
            session.send(word)
            onSent()
        }
    }
}
