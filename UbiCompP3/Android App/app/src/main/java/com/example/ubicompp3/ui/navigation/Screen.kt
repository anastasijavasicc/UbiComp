package com.example.ubicompp3.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Events : Screen("events", "Events")
    object Control : Screen("control", "Control")
    object Settings : Screen("settings", "Settings")
}