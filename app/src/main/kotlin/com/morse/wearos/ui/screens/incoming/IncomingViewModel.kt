package com.morse.wearos.ui.screens.incoming

import androidx.lifecycle.ViewModel
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.StateFlow

class IncomingViewModel(private val session: GroupSession) : ViewModel() {

    val message: StateFlow<MorseMessage?> = session.lastMessage

    fun replay() = session.replayLast()
}
