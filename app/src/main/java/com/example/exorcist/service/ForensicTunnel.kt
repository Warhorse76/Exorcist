package com.example.exorcist.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.exorcist.MainActivity

class ForensicTunnel : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        // Start as foreground service to prevent being killed
        startForeground(NOTIFICATION_ID, createNotification())

        startVpn()

        // START_STICKY: Restart service if killed by system
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
                .setSession("Exorcist Forensic Tunnel")
                .addAddress("10.0.0.1", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .setBlocking(true)

            // Detect existing VPN or Work Profile VPN (Stealth Logic)
            if (prepare(this) != null) {
                Log.w(TAG, "Another VPN is active or permission missing")
            }

            vpnInterface = builder.establish()
            Log.i(TAG, "Forensic Tunnel established")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
        }
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Forensic Tunnel stopped")
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service when task is removed (Samsung Freecess workaround)
        val restartIntent = Intent(applicationContext, ForensicTunnel::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Forensic Tunnel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the VPN tunnel active for data exfiltration"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Exorcist Active")
            .setContentText("Forensic tunnel running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "ForensicTunnel"
        const val ACTION_STOP = "com.example.exorcist.STOP_VPN"
        private const val CHANNEL_ID = "exorcist_tunnel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ForensicTunnel::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ForensicTunnel::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
