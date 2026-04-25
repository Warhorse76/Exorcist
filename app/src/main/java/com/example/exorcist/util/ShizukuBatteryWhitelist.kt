package com.example.exorcist.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Helper class to bypass battery optimization using Shizuku.
 * This allows the app to be added to the whitelist without manual user interaction.
 *
 * Target: io.github.nega_tron.exorcost (or your actual package)
 */
object ShizukuBatteryWhitelist {

    private const val TAG = "ShizukuBatteryWhitelist"
    private const val PACKAGE_NAME = "io.github.nega_tron.exorcost"

    /**
     * Check if Shizuku is available and has the required permission.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku not available: ${e.message}")
            false
        }
    }

    /**
     * Check if the app is already ignoring battery optimizations.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(PACKAGE_NAME)
    }

    /**
     * Request battery optimization exemption via Shizuku.
     * This executes the system command to add the package to the whitelist.
     *
     * @return true if the operation was successful
     */
    @Suppress("BatteryLife")
    fun requestBatteryWhitelist(): Boolean {
        if (!isShizukuAvailable()) {
            Log.e(TAG, "Shizuku not available")
            return false
        }

        return try {
            // Use dumpsys to set the battery optimization whitelist
            // This requires SHIZUKU_PERMISSION = "rikka.shizuku.PROCESS"
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "su",
                    "-c",
                    "dumpsys battery reset unplugged $PACKAGE_NAME && " +
                    "appops set $PACKAGE_NAME RUN_ANY_IN_BACKGROUND allow && " +
                    "settings put global device_provisioned 1"
                )
            )

            val result = process.waitFor()
            val success = result == 0

            if (success) {
                Log.i(TAG, "Battery whitelist request successful")
            } else {
                Log.w(TAG, "Battery whitelist request failed with code: $result")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request battery whitelist", e)
            false
        }
    }

    /**
     * Open battery optimization settings for manual whitelist.
     * Fallback method when Shizuku is not available.
     */
    fun openBatteryOptimizationSettings(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$PACKAGE_NAME")
        }
    }

    /**
     * Open the full battery optimization settings page.
     */
    fun openBatterySettings(context: Context): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * Check and request all necessary permissions for persistence.
     */
    fun checkAndRequestPermissions(context: Context): List<PermissionStatus> {
        val permissions = mutableListOf<PermissionStatus>()

        // Check battery optimization
        permissions.add(
            PermissionStatus(
                name = "Battery Optimization",
                granted = isIgnoringBatteryOptimizations(context),
                action = openBatteryOptimizationSettings(context)
            )
        )

        // Check VPN permission
        val vpnIntent = android.net.VpnService.prepare(context)
        permissions.add(
            PermissionStatus(
                name = "VPN Permission",
                granted = vpnIntent == null,
                action = vpnIntent
            )
        )

        // Check Shizuku
        permissions.add(
            PermissionStatus(
                name = "Shizuku",
                granted = isShizukuAvailable(),
                action = null
            )
        )

        return permissions
    }

    data class PermissionStatus(
        val name: String,
        val granted: Boolean,
        val action: Intent?
    )
}