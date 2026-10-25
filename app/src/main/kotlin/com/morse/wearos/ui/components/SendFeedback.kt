package com.morse.wearos.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.ui.theme.MorseColors
import kotlinx.coroutines.launch

private class SendResult(val word: String, val ok: Boolean)

/**
 * Returns a send action that always answers: a short "Sent" with the word's Morse, then [onSent];
 * or, when the word did not leave the watch, an alert that offers to try again.
 */
@Composable
fun rememberSendWithFeedback(send: suspend (String) -> Boolean, onSent: () -> Unit): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnSent by rememberUpdatedState(onSent)
    var busy by remember { mutableStateOf(false) }
    // Kept after closing so the dialog still has its text while it animates out.
    var result by remember { mutableStateOf<SendResult?>(null) }
    var open by remember { mutableStateOf(false) }

    val sendNow: (String) -> Unit = { word ->
        if (!busy) {
            busy = true
            scope.launch {
                result = SendResult(word, send(word))
                open = true
                busy = false
            }
        }
    }
    val finish = {
        if (open) {
            open = false
            currentOnSent()
        }
    }

    val shown = result
    Dialog(showDialog = open && shown?.ok == true, onDismissRequest = finish) {
        Confirmation(
            onTimeout = finish,
            durationMillis = 1600,
            icon = {
                Icon(painterResource(R.drawable.ph_check), contentDescription = null, tint = MorseColors.Green, modifier = Modifier.size(32.dp))
            },
        ) {
            Text(stringResource(R.string.sent_title, shown?.word.orEmpty()), textAlign = TextAlign.Center, maxLines = 2)
            Text(
                MorseCodec.encode(shown?.word.orEmpty()),
                style = MaterialTheme.typography.caption1,
                color = MorseColors.Accent,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
    Dialog(showDialog = open && shown?.ok == false, onDismissRequest = { open = false }) {
        Alert(
            icon = { Icon(painterResource(R.drawable.ph_warning_circle), contentDescription = null, tint = MorseColors.Red) },
            title = { Text(stringResource(R.string.send_failed_title, shown?.word.orEmpty()), textAlign = TextAlign.Center) },
            content = { Text(stringResource(R.string.send_failed_body), textAlign = TextAlign.Center) },
            negativeButton = {
                Button(onClick = { open = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Icon(painterResource(R.drawable.ph_x), contentDescription = stringResource(R.string.action_cancel))
                }
            },
            positiveButton = {
                Button(onClick = { open = false; shown?.word?.let(sendNow) }) {
                    Icon(painterResource(R.drawable.ph_arrow_counter_clockwise), contentDescription = stringResource(R.string.action_retry))
                }
            },
        )
    }
    return sendNow
}
