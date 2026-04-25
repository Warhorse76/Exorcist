package com.example.exorcist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exorcist.model.PrivilegedApp
import com.example.exorcist.ui.viewmodel.SanctuaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuaryScreen(viewModel: SanctuaryViewModel) {
    val email by viewModel.recoveryEmail.collectAsState()
    val autoExfil by viewModel.autoExfil.collectAsState()
    val owners by viewModel.owners.collectAsState()
    val isDeprovisioning by viewModel.isDeprovisioning.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sanctuary Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.loadOwners() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Forensic Recovery & Stealth De-provisioning",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Recovery Configuration", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text("Trusted Recovery Email") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Exfiltrate", style = MaterialTheme.typography.bodyLarge)
                                Text("Trigger on compromise", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = autoExfil,
                                onCheckedChange = { viewModel.updateAutoExfil(it) }
                            )
                        }
                    }
                }
            }

            item {
                Text("Privileged Owners (Managed Profiles)", style = MaterialTheme.typography.titleMedium)
            }

            if (owners.isEmpty()) {
                item {
                    Text("No high-privileged owners detected.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(owners) { app ->
                    OwnerRecoveryItem(
                        app = app,
                        isDeprovisioning = isDeprovisioning,
                        onDeprovision = { viewModel.deprovision(app.packageName) }
                    )
                }
            }

            item {
                Divider()
            }

            item {
                Button(
                    onClick = { viewModel.triggerManualExfiltration(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Manual Exfiltration")
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Forensic logs are encrypted with AES-256 (SQLCipher) and exfiltrated over an encrypted tunnel.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerRecoveryItem(
    app: PrivilegedApp,
    isDeprovisioning: Boolean,
    onDeprovision: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = app.type,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(text = app.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDeprovision,
                enabled = !isDeprovisioning,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isDeprovisioning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("De-provision Profile")
                }
            }
        }
    }
}
