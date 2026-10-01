package com.morse.wearos.transport.wifidirect

import android.Manifest
import android.os.Build

/** Runtime permission Wi-Fi P2P discovery needs on this API level. */
val wifiDirectPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }
