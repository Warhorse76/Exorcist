# Exorcist 🛡️🚀

**Exorcist** is a high-privileged Android security audit and forensic utility. It leverages the [Shizuku API](https://shizuku.rikka.app/) to perform deep system analysis and mitigation of advanced threats (such as malicious MDM or Work Profile compromises) without requiring root access.

---

## ✨ Key Features

### 🔍 Privileged Security Audit
- **Ownership Detection**: Identifies hidden **Device Owners** and **Profile Owners** that standard applications cannot see.
- **Permission Analysis**: Flags apps with high-risk permissions like **Accessibility**, **Device Admin**, and **INTERACT_ACROSS_PROFILES**.
- **Chameleon Scanner**: Detects malicious apps disguised as utilities (e.g., Calculators) that hold dangerous permissions.

### 🌐 Network Forensics
- **Real-time Monitor**: Queries `/proc/net/tcp` via Shizuku shell to map outbound connections to specific PIDs and package names.
- **Forensic Tunnel (VpnService)**: Routes forensic data through a secure tunnel with stealth logic to detect and bypass unauthorized "Always-on" VPNs.
- **Network Integrity**: Monitors ARP tables to detect MITM attacks and ARP poisoning attempts.

### ⌨️ Ashell Terminal & Script Engine
- **Integrated Terminal**: A retro-style monospace console for executing privileged shell commands.
- **Shizuku Script Engine**: 20+ specialized scripts for hardening and recon, including:
  - `harden-perms`: Batch revoke dangerous permissions.
  - `kill-ghosts`: Identify and remediate unauthorized managed profiles.
  - `detect-overlays`: Identify active phishing overlays via `dumpsys`.

### 🛡️ Hardened Security
- **Encrypted Persistence**: Forensic logs are stored in a **SQLCipher (AES-256)** encrypted Room database.
- **Anti-Reverse Engineering**: Optimized with **R8 obfuscation**, XOR string protection, and debugger detection.

---

## 🚀 Getting Started

1. **Install Shizuku**: Ensure the [Shizuku app](https://play.google.com/store/apps/details?id=rikka.shizuku) is installed and running on your device.
2. **Authorize Exorcist**: Open Exorcist and click **Authorize** on the dashboard to grant privileged access.
3. **Run Audit**: Use the **Audit** or **Terminal** tabs to begin your forensic investigation.

---

## 🛠️ Technical Stack

- **UI**: Jetpack Compose (Material 3) with Adaptive Layouts.
- **Navigation**: Jetpack Navigation 3 (State-driven).
- **System Interface**: Shizuku API (via AIDL User Service).
- **Database**: Room + SQLCipher (AES-256).
- **Background**: WorkManager for encrypted exfiltration.

---

## 🤝 Contribution & "Awesome Shizuku"
Exorcist is designed for security researchers and power users. We welcome contributions that add new forensic scripts or detection modules. 

**Exorcist is a candidate for the [Awesome Shizuku](https://github.com/RikkaApps/awesome-shizuku) list.**

---

## ⚖️ Disclaimer
*This tool is intended for forensic research and personal security auditing. Use responsibly. The developers are not responsible for any misuse or data loss resulting from de-provisioning system-level profiles.*

---
*Developed by Greg - Exorcist Team*
