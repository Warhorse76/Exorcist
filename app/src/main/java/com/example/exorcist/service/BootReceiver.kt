package com.example.exorcist.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to restart ForensicTunnel after device boot.
 * This ensures the VPN persists across device restarts.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Boot completed, starting ForensicTunnel")

            val tunnelIntent = Intent(context, ForensicTunnel::class.java)
            context.startForegroundService(tunnelIntent)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}