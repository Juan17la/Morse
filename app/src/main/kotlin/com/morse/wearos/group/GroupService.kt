package com.morse.wearos.group

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.morse.wearos.MainActivity
import com.morse.wearos.R

/**
 * Keeps the process in the foreground while in a group. Without it Android firewalls a
 * backgrounded app's network (screen off, keyboard open, another app on top), which drops
 * the group socket, so a watch could never receive a message while idle.
 */
class GroupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.group_service_channel), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ph_vibrate)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.group_service_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "group"
        private const val NOTIFICATION_ID = 1

        fun setRunning(context: Context, running: Boolean) {
            val intent = Intent(context, GroupService::class.java)
            if (running) context.startForegroundService(intent) else context.stopService(intent)
        }
    }
}
