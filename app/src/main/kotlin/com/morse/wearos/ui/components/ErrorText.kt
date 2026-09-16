package com.morse.wearos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.morse.wearos.R
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.TransportError

@Composable
fun errorMessage(error: ConnectionState.Error): String {
    val base = stringResource(
        when (error.error) {
            TransportError.WRONG_CODE -> R.string.error_wrong_code
            TransportError.GROUP_FULL -> R.string.error_group_full
            TransportError.HOST_NOT_FOUND -> R.string.error_host_not_found
            TransportError.CONNECTION_LOST -> R.string.error_connection_lost
            TransportError.NOT_SUPPORTED -> R.string.error_not_supported
            TransportError.PERMISSION_DENIED -> R.string.error_permission_denied
            TransportError.UNKNOWN -> R.string.error_unknown
        }
    )
    return error.detail?.let { "$base\n$it" } ?: base
}
