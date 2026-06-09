package com.example.ubicompp3.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavHostController) {
    val screens = listOf(
        Screen.Dashboard,
        Screen.Events,
        Screen.Control,
        Screen.Settings
    )

    NavigationBar {
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = navBackStackEntry?.destination?.route

        screens.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route)
                            launchSingleTop = true
                        }
                    }
                },
                icon = {},
                label = {
                    Text(screen.title)
                }
            )
        }
    }
}