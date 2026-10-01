package com.morse.wearos.transport.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.util.Log
import androidx.core.content.ContextCompat
import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.PeerFinder
import com.morse.wearos.transport.TransportException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Host: becomes Wi-Fi P2P group owner and advertises `_morse._tcp` with the access code.
 * Joiner: discovers the service whose code matches, connects, and returns the owner's address.
 */
class WifiDirectPeerFinder(private val context: Context) : PeerFinder {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    private var connectionInfo = CompletableDeferred<WifiP2pInfo>()

    @SuppressLint("MissingPermission") // open() throws PERMISSION_DENIED first
    override suspend fun publish(code: String) {
        val (manager, channel) = open()
        manager.removeGroupSafely(channel)
        manager.await(TransportError.UNKNOWN) { manager.createGroup(channel, it) }
        val service = WifiP2pDnsSdServiceInfo.newInstance(
            SERVICE_NAME, SERVICE_TYPE, mapOf(TXT_CODE to code),
        )
        manager.await(TransportError.UNKNOWN) { manager.addLocalService(channel, service, it) }
    }

    @SuppressLint("MissingPermission") // open() throws PERMISSION_DENIED first
    override suspend fun resolveHost(code: String): InetAddress {
        val (manager, channel) = open()
        manager.removeGroupSafely(channel)
        connectionInfo = CompletableDeferred()

        val hostDevice = CompletableDeferred<String>()
        manager.setDnsSdResponseListeners(
            channel,
            { _, _, _ -> },
            { _, record, device ->
                if (record[TXT_CODE] == code && !hostDevice.isCompleted) hostDevice.complete(device.deviceAddress)
            },
        )
        manager.await(TransportError.UNKNOWN) {
            manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(), it)
        }
        manager.await(TransportError.UNKNOWN) { manager.discoverServices(channel, it) }

        val deviceAddress = withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) { hostDevice.await() }
            ?: throw TransportException(TransportError.HOST_NOT_FOUND)

        val config = WifiP2pConfig().apply { this.deviceAddress = deviceAddress }
        manager.await(TransportError.HOST_NOT_FOUND) { manager.connect(channel, config, it) }

        val info = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { connectionInfo.await() }
            ?: throw TransportException(TransportError.HOST_NOT_FOUND)
        return info.groupOwnerAddress
            ?: throw TransportException(TransportError.HOST_NOT_FOUND)
    }

    override fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        val m = manager ?: return
        val c = channel ?: return
        runCatching { m.clearLocalServices(c, null) }
        runCatching { m.clearServiceRequests(c, null) }
        runCatching { m.removeGroup(c, null) }
        channel = null
    }

    private fun open(): Pair<WifiP2pManager, WifiP2pManager.Channel> {
        val manager = manager?.takeIf {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
        } ?: throw TransportException(TransportError.NOT_SUPPORTED)
        if (ContextCompat.checkSelfPermission(context, wifiDirectPermission) != PackageManager.PERMISSION_GRANTED) throw TransportException(TransportError.PERMISSION_DENIED)
        channel?.let { return manager to it }
        val channel = manager.initialize(context, context.mainLooper, null)
            ?: throw TransportException(TransportError.NOT_SUPPORTED)
        this.channel = channel
        registerReceiver(manager, channel)
        return manager to channel
    }

    private fun registerReceiver(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
                    manager.requestConnectionInfo(channel) { info ->
                        if (info.groupFormed && !connectionInfo.isCompleted) connectionInfo.complete(info)
                    }
                }
            }
        }
        val filter = IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        ContextCompat.registerReceiver(context, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiver = r
    }

    /** Drops any stale group from a previous session; failing here just means there was none. */
    private suspend fun WifiP2pManager.removeGroupSafely(channel: WifiP2pManager.Channel) {
        runCatching { await(TransportError.UNKNOWN) { removeGroup(channel, it) } }
    }

    @SuppressLint("MissingPermission")
    private suspend fun WifiP2pManager.await(
        error: TransportError,
        call: (WifiP2pManager.ActionListener) -> Unit,
    ) {
        suspendCancellableCoroutine { cont ->
            call(object : WifiP2pManager.ActionListener {
                override fun onSuccess() = cont.resume(Unit)
                override fun onFailure(reason: Int) {
                    Log.w(TAG, "p2p action failed: $reason")
                    val mapped = if (reason == WifiP2pManager.P2P_UNSUPPORTED) TransportError.NOT_SUPPORTED else error
                    cont.resumeWithException(TransportException(mapped, "p2p error $reason"))
                }
            })
        }
    }

    private companion object {
        const val TAG = "WifiDirectPeerFinder"
        const val SERVICE_NAME = "morse"
        const val SERVICE_TYPE = "_morse._tcp"
        const val TXT_CODE = "code"
        const val DISCOVERY_TIMEOUT_MS = 20_000L
        const val CONNECT_TIMEOUT_MS = 30_000L
    }
}
