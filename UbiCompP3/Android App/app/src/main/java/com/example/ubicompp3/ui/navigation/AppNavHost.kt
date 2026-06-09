package com.example.ubicompp3.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ubicompp3.ui.screens.ControlScreen
import com.example.ubicompp3.ui.screens.DashboardScreen
import com.example.ubicompp3.ui.screens.EventsScreen
import com.example.ubicompp3.ui.screens.SettingsScreen
import com.example.ubicompp3.viewmodel.DashboardViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: DashboardViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel)
        }
        composable(Screen.Events.route) {
            EventsScreen(viewModel)
        }
        composable(Screen.Control.route) {
            ControlScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel)
        }
    }
}