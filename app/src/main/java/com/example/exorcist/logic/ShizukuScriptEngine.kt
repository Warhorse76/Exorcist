package com.example.exorcist.logic

import android.content.Context
import kotlinx.coroutines.delay

class ShizukuScriptEngine(
    private val auditor: SecurityAuditor,
    private val context: Context
) {

    suspend fun executeScript(command: String): String {
        val parts = command.trim().split(Regex("\\s+"))
        val scriptName = parts[0].lowercase()
        val rawArgs = parts.drop(1)
        val argsString = rawArgs.joinToString(" ")

        return when (scriptName) {
            "help" -> showDetailedHelp()
            
            // DEFENSIVE (Recovery & Hardening)
            "harden-perms" -> runHardenPerms()
            "kill-ghosts" -> runKillGhosts()
            "disable-bad-apps" -> runDisableBadApps(rawArgs)
            "stop-background" -> auditor.runPrivilegedShell("for pkg in $(pm list packages -3 | cut -d: -f2); do am force-stop ${'$'}pkg; done")
            "reset-net" -> runResetNet()
            "block-install" -> auditor.runPrivilegedShell("pm set-install-location 1")
            "clean-cache" -> auditor.runPrivilegedShell("pm trim-caches 999G")
            "emergency-stop" -> runEmergencyStop()

            // OFFENSIVE/AUDIT (Recon & Detection)
            "audit-manifest" -> auditor.runPrivilegedShell("pm dump ${rawArgs.getOrNull(0) ?: ""}")
            "find-chameleons" -> runFindChameleons()
            "check-ip" -> auditor.runPrivilegedShell("ip neighbor show; ip addr show")
            "list-services" -> auditor.runPrivilegedShell("dumpsys activity services")
            "detect-overlays" -> auditor.runPrivilegedShell("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp|Window #|HasSurface'")
            "app-standby" -> auditor.runPrivilegedShell("dumpsys usagestats")
            "check-firebase" -> auditor.runPrivilegedShell("dumpsys connectivity | grep -E 'google.com|firebaseio.com'")
            
            else -> auditor.runPrivilegedShell(command)
        }
    }

    private fun showDetailedHelp(): String {
        return """
            Exorcist Forensic Script Engine v1.0
            -----------------------------------
            DEFENSIVE (Hardening & Recovery):
            - harden-perms: Batch revoke SMS/Location for 3rd-party apps.
            - kill-ghosts: Audit & identify unauthorized profiles.
            - disable-bad-apps [pkg]: Instantly disable a package.
            - stop-background: am force-stop all 3rd-party apps.
            - reset-net: Toggle Airplane mode to clear socket sessions.
            - block-install: Restrict new APK install locations.
            - emergency-stop: force-stop ALL 3rd-party apps immediately.

            OFFENSIVE/AUDIT (Recon & Detection):
            - find-chameleons: Scan for disguised utilities with SMS perms.
            - check-ip: Audit local IP and ARP tables (MITM detect).
            - detect-overlays: Find active 'alert windows' (phishing).
            - check-firebase: Trace active connections to Firebase C2s.
            - audit-manifest [pkg]: Full dump of permissions and intents.
            - [raw command]: Run any native shell command directly.
            -----------------------------------
        """.trimIndent()
    }

    private suspend fun runHardenPerms(): String {
        val cmd = """
            for pkg in $(pm list packages -3 | cut -d: -f2); do
                echo "Hardening perms for: ${'$'}pkg"
                pm revoke ${'$'}pkg android.permission.READ_SMS 2>/dev/null
                pm revoke ${'$'}pkg android.permission.RECEIVE_SMS 2>/dev/null
                pm revoke ${'$'}pkg android.permission.ACCESS_FINE_LOCATION 2>/dev/null
            done
        """.trimIndent()
        return auditor.runPrivilegedShell("sh -c '$cmd'")
    }

    private suspend fun runKillGhosts(): String {
        return auditor.runPrivilegedShell("pm list users") + 
               "\n\nUse 'pm remove-user [id]' to delete unauthorized profiles."
    }

    private suspend fun runDisableBadApps(args: List<String>): String {
        if (args.isEmpty()) return "Error: Provide package names to disable."
        val results = mutableListOf<String>()
        for (pkg in args) {
            results.add(auditor.runPrivilegedShell("pm disable-user --user 0 $pkg"))
        }
        return results.joinToString("\n")
    }

    private suspend fun runResetNet(): String {
        auditor.runPrivilegedShell("cmd connectivity airplane-mode enable")
        delay(1000)
        auditor.runPrivilegedShell("cmd connectivity airplane-mode disable")
        return "Network stack reset: Airplane mode toggled."
    }

    private suspend fun runEmergencyStop(): String {
        val cmd = "for pkg in $(pm list packages -3 | cut -d: -f2); do am force-stop ${'$'}pkg; done"
        return auditor.runPrivilegedShell("sh -c '$cmd'")
    }

    private suspend fun runFindChameleons(): String {
        val cmd = """
            echo "Scanning for disguised packages..."
            for pkg in $(pm list packages -3 | cut -d: -f2); do
                if echo "${'$'}pkg" | grep -Ei "calc|torch|flash|clock|contacts|calendar" > /dev/null; then
                    perms=$(pm dump ${'$'}pkg | grep "android.permission.READ_SMS")
                    if [ ! -z "${'$'}perms" ]; then
                        echo "[!] ALERT: Disguised Utility Detected"
                        echo "Package: ${'$'}pkg"
                        echo "Reason: Utility package holding READ_SMS permission"
                        echo "-----------------------------------"
                    fi
                fi
            done
            echo "Scan complete."
        """.trimIndent()
        return auditor.runPrivilegedShell("sh -c '$cmd'")
    }
}
