package com.morse.wearos.ui.permissions

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.morse.wearos.transport.wifidirect.wifiDirectPermission

/** Returns an action that runs [onGranted] immediately if allowed, or after the system prompt. */
@Composable
fun rememberWifiDirectPermissionRequest(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnDenied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) currentOnGranted() else currentOnDenied()
    }
    return {
        val granted = ContextCompat.checkSelfPermission(context, wifiDirectPermission) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) currentOnGranted() else launcher.launch(wifiDirectPermission)
    }
}
