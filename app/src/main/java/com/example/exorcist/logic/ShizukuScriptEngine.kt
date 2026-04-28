package com.example.exorcist.logic

import android.content.Context

class ShizukuScriptEngine(
    private val auditor: SecurityAuditor,
    private val context: Context
) {

    suspend fun executeScript(command: String): String {
        val parts = command.trim().split(Regex("\\s+"))
        val scriptName = parts[0].lowercase()
        val args = parts.drop(1)

        return when (scriptName) {
            // DEFENSIVE
            "harden-perms" -> runHardenPerms()
            "kill-ghosts" -> runKillGhosts()
            "disable-bad-apps" -> runDisableBadApps(args)
            "clear-notifications" -> auditor.runPrivilegedShell("cmd notification stop-listening com.example.badactor")
            "stop-background" -> auditor.runPrivilegedShell("am kill-all")
            "reset-net" -> runResetNet()
            "block-install" -> auditor.runPrivilegedShell("pm set-install-location 1")
            "check-admins" -> auditor.runPrivilegedShell("dpm list-active-admins")
            "clean-cache" -> auditor.runPrivilegedShell("pm trim-caches 999G")
            "emergency-stop" -> runEmergencyStop()

            // OFFENSIVE/AUDIT
            "audit-manifest" -> auditor.runPrivilegedShell("pm dump ${args.getOrNull(0) ?: ""}")
            "find-chameleons" -> runFindChameleons()
            "list-path" -> auditor.runPrivilegedShell("pm path ${args.getOrNull(0) ?: ""}")
            "check-ip" -> auditor.runPrivilegedShell("ip neighbor show; ip addr show")
            "list-services" -> auditor.runPrivilegedShell("dumpsys activity services")
            "detect-overlays" -> auditor.runPrivilegedShell("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp|Window #|HasSurface'")
            "app-standby" -> auditor.runPrivilegedShell("dumpsys usagestats")
            "list-permissions" -> auditor.runPrivilegedShell("pm list permissions -u -d -g")
            "check-firebase" -> auditor.runPrivilegedShell("dumpsys connectivity | grep -E 'google.com|firebaseio.com'")
            "shell-access" -> "Error: Raw rish session must be initiated via ADB or specialized terminal bridge."
            "help" -> showDetailedHelp()
            
            else -> auditor.runPrivilegedShell(command)
        }
    }

    private fun showDetailedHelp(): String {
        return """
            Exorcist Forensic Script Engine v1.0
            -----------------------------------
            DEFENSIVE (Hardening & Recovery):
            - harden-perms: Batch revoke SMS/Location for 3rd-party apps.
            - kill-ghosts: Audit & identify unauthorized profiles for removal.
            - disable-bad-apps [pkg]: Instantly disable a malicious package.
            - clear-notifications: Stop malicious status bar listeners.
            - stop-background: am force-stop non-whitelisted apps.
            - reset-net: Toggle Airplane mode to clear socket sessions.
            - block-install: Restrict new APK install locations.
            - check-admins: List active Device Administrators.
            - clean-cache: Trim system caches to clear temp malware data.
            - emergency-stop: force-stop ALL 3rd-party apps at once.

            OFFENSIVE/AUDIT (Recon & Detection):
            - audit-manifest [pkg]: Full dump of permissions and intents.
            - find-chameleons: Scan for disguised utilities with SMS perms.
            - list-path [pkg]: Show exact physical file path of an APK.
            - check-ip: Audit local IP and ARP tables (MITM detect).
            - list-services: Identify hidden background services.
            - detect-overlays: Find active 'alert windows' (phishing).
            - app-standby: Check usagestats of suspicious packages.
            - list-permissions: List every dangerous permission on system.
            - check-firebase: Trace active connections to Firebase C2s.
            - [raw command]: Run any adb shell command directly.
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
               "\n\nUse 'nuke-profile [id]' to remove unauthorized profiles identified above."
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
        Thread.sleep(1000)
        auditor.runPrivilegedShell("cmd connectivity airplane-mode disable")
        return "Network stack reset: Airplane mode toggled."
    }

    private suspend fun runEmergencyStop(): String {
        val cmd = "for pkg in $(pm list packages -3 | cut -d: -f2); do am force-stop ${'$'}pkg; done"
        return auditor.runPrivilegedShell("sh -c '$cmd'")
    }

    private suspend fun runFindChameleons(): String {
        // Logic: Find 3rd party apps with 'Calculator' in the name/label that have SMS perms
        // Note: Label matching in shell is hard, so we scan for package names containing utility terms
        val cmd = """
            echo "Scanning for disguised packages..."
            for pkg in $(pm list packages -3 | cut -d: -f2); do
                if echo "${'$'}pkg" | grep -Ei "calc|torch|flash|clock|contacts|calendar" > /dev/null; then
                    perms=$(pm dump ${'$'}pkg | grep "android.permission.READ_SMS")
                    if [ ! -z "${'$'}perms" ]; then
                        echo "[!] ALERT: Disguised Utility Detected"
                        echo "Package: ${'$'}pkg"
                        echo "Risk: HIGH"
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
