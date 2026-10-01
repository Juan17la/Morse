package com.morse.wearos.ui.screens.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.data.AddResult
import com.morse.wearos.data.WordsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordsViewModel(private val repository: WordsRepository) : ViewModel() {

    val customWords: StateFlow<List<String>> = repository.customWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(word: String, onRejected: (AddResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.add(word)
            if (result != AddResult.ADDED) onRejected(result)
        }
    }

    fun remove(word: String) {
        viewModelScope.launch { repository.remove(word) }
    }
}
