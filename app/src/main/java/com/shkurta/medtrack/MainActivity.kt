package com.shkurta.medtrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import com.shkurta.medtrack.navigation.Destination
import com.shkurta.medtrack.ui.dashboard.DashboardScreen
import com.shkurta.medtrack.ui.dashboard.DashboardViewModel
import com.shkurta.medtrack.ui.history.HistoryScreen
import com.shkurta.medtrack.ui.history.HistoryViewModel
import com.shkurta.medtrack.ui.settings.SettingsScreen
import com.shkurta.medtrack.ui.settings.SettingsViewModel
import com.shkurta.medtrack.ui.theme.MedTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedTrackTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val backStack = rememberNavBackStack(Destination.Dashboard)

                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    },
                    entryProvider = entryProvider {
                        entry<Destination.Dashboard> {
                            val viewModel: DashboardViewModel = viewModel()
                            DashboardScreen(
                                viewModel = viewModel,
                                onHistory = { backStack.add(Destination.History) },
                                onSettings = { backStack.add(Destination.Settings) }
                            )
                        }
                        entry<Destination.History> {
                            val viewModel: HistoryViewModel = viewModel()
                            HistoryScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeAt(backStack.size - 1) }
                            )
                        }
                        entry<Destination.Settings> {
                            val viewModel: SettingsViewModel = viewModel()
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeAt(backStack.size - 1) }
                            )
                        }
                    }
                )
            }
        }
    }
}
