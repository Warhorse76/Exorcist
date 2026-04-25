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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exorcist.model.PrivilegedApp
import com.example.exorcist.ui.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    viewModel: SecurityViewModel,
    onRequestShizukuPermission: () -> Unit
) {
    val apps by viewModel.privilegedApps.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Audit") },
                actions = {
                    IconButton(onClick = { viewModel.runAudit() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ShizukuStatusCard(
                status = shizukuStatus,
                onAuthorizeClick = onRequestShizukuPermission
            )

            if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps) { app ->
                        AppPrivilegeItem(app)
                    }
                }
            }
        }
    }
}

@Composable
fun ShizukuStatusCard(
    status: SecurityViewModel.ShizukuStatus,
    onAuthorizeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                SecurityViewModel.ShizukuStatus.AUTHORIZED -> MaterialTheme.colorScheme.primaryContainer
                SecurityViewModel.ShizukuStatus.UNAUTHORIZED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shizuku Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (status) {
                        SecurityViewModel.ShizukuStatus.AUTHORIZED -> "Authorized and active"
                        SecurityViewModel.ShizukuStatus.UNAUTHORIZED -> "Permission required for DO/PO audit"
                        SecurityViewModel.ShizukuStatus.NOT_INSTALLED -> "Shizuku not detected"
                        SecurityViewModel.ShizukuStatus.DISCONNECTED -> "Checking status..."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (status == SecurityViewModel.ShizukuStatus.UNAUTHORIZED) {
                Button(onClick = onAuthorizeClick) {
                    Text("Authorize")
                }
            } else if (status == SecurityViewModel.ShizukuStatus.NOT_INSTALLED) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            } else if (status == SecurityViewModel.ShizukuStatus.AUTHORIZED) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AppPrivilegeItem(app: PrivilegedApp) {
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
                imageVector = when (app.type) {
                    "Accessibility" -> Icons.Rounded.Accessibility
                    "Device Admin" -> Icons.Rounded.AdminPanelSettings
                    "Device Owner" -> Icons.Rounded.Security
                    "Profile Owner" -> Icons.Rounded.Work
                    else -> Icons.Rounded.Apps
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(app.type, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("System", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
