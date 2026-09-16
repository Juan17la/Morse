package com.morse.wearos.ui.components

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.wear.input.RemoteInputIntentHelper

private const val RESULT_KEY = "morse_text"

@Composable
fun rememberTextInputLauncher(label: String, onResult: (String) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data
            ?.let { RemoteInput.getResultsFromIntent(it) }
            ?.getCharSequence(RESULT_KEY)
            ?.toString()
        if (!text.isNullOrBlank()) currentOnResult(text)
    }
    return {
        val remoteInput = RemoteInput.Builder(RESULT_KEY).setLabel(label).build()
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        launcher.launch(intent)
    }
}
