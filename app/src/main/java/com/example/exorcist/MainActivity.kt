package com.example.exorcist

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.exorcist.data.ExorcistDatabase
import com.example.exorcist.logic.SecurityAuditor
import com.example.exorcist.logic.settings.SanctuarySettings
import com.example.exorcist.navigation.Destination
import com.example.exorcist.ui.screens.*
import com.example.exorcist.ui.theme.ExorcistTheme
import com.example.exorcist.ui.viewmodel.NetworkViewModel
import com.example.exorcist.ui.viewmodel.SanctuaryViewModel
import com.example.exorcist.ui.viewmodel.SecurityViewModel
import com.example.exorcist.ui.viewmodel.TerminalViewModel
import com.example.exorcist.util.SecurityUtils
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val auditor by lazy { SecurityAuditor(this) }
    private val settings by lazy { SanctuarySettings(this) }
    private val database by lazy { ExorcistDatabase.getDatabase(this) }

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Anti-tamper check
        if (SecurityUtils.isDebuggerAttached()) {
            // In a production forensic tool, you might alert the user or self-destruct sensitive keys.
        }

        enableEdgeToEdge()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            ExorcistTheme {
                ExorcistApp(auditor, settings, database)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExorcistApp(auditor: SecurityAuditor, settings: SanctuarySettings, database: ExorcistDatabase) {
    val backStack = rememberNavBackStack(Destination.Audit as NavKey)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = backStack.lastOrNull()
                NavigationBarItem(
                    selected = currentDestination == Destination.Audit,
                    onClick = { 
                        if (currentDestination != Destination.Audit) {
                            backStack.remove(Destination.Audit as NavKey)
                            backStack.add(Destination.Audit as NavKey)
                        }
                    },
                    icon = { Icon(Icons.Rounded.Security, contentDescription = "Audit") },
                    label = { Text("Audit") }
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.NetworkMonitor,
                    onClick = {
                        if (currentDestination != Destination.NetworkMonitor) {
                            backStack.remove(Destination.NetworkMonitor as NavKey)
                            backStack.add(Destination.NetworkMonitor as NavKey)
                        }
                    },
                    icon = { Icon(Icons.Rounded.Lan, contentDescription = "Network") },
                    label = { Text("Network") }
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Sanctuary,
                    onClick = {
                        if (currentDestination != Destination.Sanctuary) {
                            backStack.remove(Destination.Sanctuary as NavKey)
                            backStack.add(Destination.Sanctuary as NavKey)
                        }
                    },
                    icon = { Icon(Icons.Rounded.Shield, contentDescription = "Sanctuary") },
                    label = { Text("Sanctuary") }
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Terminal,
                    onClick = {
                        if (currentDestination != Destination.Terminal) {
                            backStack.remove(Destination.Terminal as NavKey)
                            backStack.add(Destination.Terminal as NavKey)
                        }
                    },
                    icon = { Icon(Icons.Rounded.Terminal, contentDescription = "Terminal") },
                    label = { Text("Terminal") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
            entryProvider = entryProvider {
                entry<Destination.Audit>(
                    metadata = ListDetailSceneStrategy.listPane()
                ) {
                    val viewModel: SecurityViewModel = viewModel { SecurityViewModel(auditor) }
                    LaunchedEffect(Unit) {
                        viewModel.checkShizuku()
                    }
                    AuditScreen(
                        viewModel = viewModel,
                        onRequestShizukuPermission = {
                            if (Shizuku.pingBinder()) {
                                Shizuku.requestPermission(100)
                            }
                        }
                    )
                }
                entry<Destination.NetworkMonitor>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    val viewModel: NetworkViewModel = viewModel { NetworkViewModel(auditor) }
                    NetworkMonitorScreen(viewModel = viewModel)
                }
                entry<Destination.Sanctuary>(
                    metadata = ListDetailSceneStrategy.extraPane()
                ) {
                    val viewModel: SanctuaryViewModel = viewModel { 
                        SanctuaryViewModel(settings, auditor, database) 
                    }
                    SanctuaryScreen(viewModel = viewModel)
                }
                entry<Destination.Terminal>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    val viewModel: TerminalViewModel = viewModel { TerminalViewModel(auditor) }
                    TerminalScreen(viewModel = viewModel)
                }
            },
            sceneStrategy = listDetailStrategy,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
        )
    }
}
