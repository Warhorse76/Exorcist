package com.example.exorcist.logic

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.view.accessibility.AccessibilityManager
import com.example.exorcist.IExorcistService
import com.example.exorcist.model.PrivilegedApp
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

class SecurityAuditor(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private var userService: IExorcistService? = null

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return if (Shizuku.isPreV11()) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun getUserService(): IExorcistService? {
        if (userService != null) return userService
        if (!isShizukuAvailable() || !hasShizukuPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, ExorcistUserService::class.java.name))
                .daemon(false)
                .processNameSuffix("privileged_service")
                .debuggable(true)

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    val binder = IExorcistService.Stub.asInterface(service)
                    userService = binder
                    if (continuation.isActive) continuation.resume(binder)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    userService = null
                }
            }
            Shizuku.bindUserService(args, connection)
        }
    }

    fun getAccessibilityApps(): List<PrivilegedApp> {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(-1)
        return enabledServices.map {
            val appInfo = it.resolveInfo.serviceInfo.applicationInfo
            PrivilegedApp(
                packageName = appInfo.packageName,
                name = appInfo.loadLabel(packageManager).toString(),
                type = "Accessibility",
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }
    }

    fun getDeviceAdmins(): List<PrivilegedApp> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admins = dpm.activeAdmins ?: emptyList()
        return admins.map {
            val packageName = it.packageName
            val appInfo = try {
                packageManager.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                null
            }
            PrivilegedApp(
                packageName = packageName,
                name = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
                type = "Device Admin",
                isSystemApp = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
            )
        }
    }

    suspend fun getOwnersViaShizuku(): List<PrivilegedApp> {
        val service = getUserService() ?: return emptyList()

        val owners = mutableListOf<PrivilegedApp>()
        try {
            val output = service.exec("dumpsys device_policy")
            val lines = output.split("\n")
            var currentSection = ""
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("Device Owner:")) {
                    currentSection = "Device Owner"
                } else if (trimmed.startsWith("Profile Owner (user")) {
                    currentSection = "Profile Owner"
                }

                if (trimmed.startsWith("admin=ComponentInfo{")) {
                    val packageName = trimmed.substringAfter("{").substringBefore("/")
                    val appInfo = try {
                        packageManager.getApplicationInfo(packageName, 0)
                    } catch (e: Exception) {
                        null
                    }
                    owners.add(
                        PrivilegedApp(
                            packageName = packageName,
                            name = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
                            type = currentSection.ifEmpty { "Owner" },
                            isSystemApp = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return owners
    }

    suspend fun getActiveConnections(): List<com.example.exorcist.model.NetworkConnection> {
        val service = getUserService() ?: return emptyList()
        val connections = mutableListOf<com.example.exorcist.model.NetworkConnection>()
        
        try {
            // Query /proc/net/tcp and /proc/net/tcp6 via shell for high accuracy
            val tcp4 = service.exec("cat /proc/net/tcp")
            val tcp6 = service.exec("cat /proc/net/tcp6")
            
            connections.addAll(parseProcNet(tcp4))
            connections.addAll(parseProcNet(tcp6))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return connections
    }

    private fun parseProcNet(content: String): List<com.example.exorcist.model.NetworkConnection> {
        val lines = content.split("\n")
        val result = mutableListOf<com.example.exorcist.model.NetworkConnection>()
        
        for (i in 1 until lines.size) { // Skip header
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 8) continue
            
            val local = hexToIp(parts[1])
            val remote = hexToIp(parts[2])
            val state = parts[3]
            val uid = parts[7].toIntOrNull() ?: -1
            
            if (uid > 0) {
                val packages = packageManager.getPackagesForUid(uid)
                val packageName = packages?.firstOrNull()
                val appName = packageName?.let {
                    try {
                        packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
                    } catch (e: Exception) {
                        null
                    }
                }
                
                result.add(
                    com.example.exorcist.model.NetworkConnection(
                        localAddress = local,
                        remoteAddress = remote,
                        state = decodeTcpState(state),
                        uid = uid,
                        packageName = packageName,
                        appName = appName
                    )
                )
            }
        }
        return result
    }

    private fun hexToIp(hex: String): String {
        return try {
            val parts = hex.split(":")
            val ipPart = parts[0]
            val portPart = parts[1]
            val port = portPart.toInt(16)
            
            if (ipPart.length == 8) { // IPv4
                val i1 = ipPart.substring(6, 8).toInt(16)
                val i2 = ipPart.substring(4, 6).toInt(16)
                val i3 = ipPart.substring(2, 4).toInt(16)
                val i4 = ipPart.substring(0, 2).toInt(16)
                "$i1.$i2.$i3.$i4:$port"
            } else if (ipPart.length == 32) { // IPv6
                // Simplified IPv6 parsing (little endian per word in /proc/net/tcp6)
                // For brevity, just returning hex or simplified
                "IPv6:$port"
            } else {
                "Unknown:$port"
            }
        } catch (e: Exception) {
            "Invalid"
        }
    }

    private fun decodeTcpState(state: String): String {
        return when (state) {
            "01" -> "ESTABLISHED"
            "02" -> "SYN_SENT"
            "03" -> "SYN_RECV"
            "04" -> "FIN_WAIT1"
            "05" -> "FIN_WAIT2"
            "06" -> "TIME_WAIT"
            "07" -> "CLOSE"
            "08" -> "CLOSE_WAIT"
            "09" -> "LAST_ACK"
            "0A" -> "LISTEN"
            "0B" -> "CLOSING"
            else -> "UNKNOWN"
        }
    }

    suspend fun deprovisionProfile(packageName: String): Boolean {
        val service = getUserService() ?: return false
        return try {
            // Attempt to remove active admin for Device Owner / Profile Owner
            val result = service.exec("dpm remove-active-admin $packageName")
            !result.contains("Error")
        } catch (e: Exception) {
            false
        }
    }
}
