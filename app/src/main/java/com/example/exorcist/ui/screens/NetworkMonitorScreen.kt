package com.example.exorcist.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exorcist.model.NetworkConnection
import com.example.exorcist.ui.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMonitorScreen(viewModel: NetworkViewModel) {
    val connections by viewModel.connections.collectAsState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnService(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Forensics") },
                actions = {
                    Text(
                        text = if (isVpnActive) "TUNNEL ACTIVE" else "TUNNEL INACTIVE",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = isVpnActive,
                        onCheckedChange = { 
                            viewModel.toggleVpn(context) { intent -> 
                                intent?.let { vpnLauncher.launch(it) } 
                            } 
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (connections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active connections found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(connections) { conn ->
                        ConnectionItem(conn)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionItem(conn: NetworkConnection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Lan,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conn.remoteAddress,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${conn.appName ?: "Unknown"} (${conn.packageName ?: "N/A"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "State: ${conn.state} | UID: ${conn.uid}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
