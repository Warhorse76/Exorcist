package com.example.exorcist.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.net.InetAddress

class ForensicTunnel : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        
        startVpn()
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
            // prepare(context) is usually called by UI, but here we check status
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
        stopSelf()
        Log.i(TAG, "Forensic Tunnel stopped")
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ForensicTunnel"
        const val ACTION_STOP = "com.example.exorcist.STOP_VPN"
    }
}
