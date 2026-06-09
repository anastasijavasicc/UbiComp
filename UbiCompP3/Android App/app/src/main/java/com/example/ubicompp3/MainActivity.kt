package com.example.ubicompp3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ubicompp3.notifications.NotificationHelper
import com.example.ubicompp3.ui.navigation.AppNavHost
import com.example.ubicompp3.ui.navigation.BottomNavBar
import com.example.ubicompp3.ui.theme.UbiCompP3Theme
import com.example.ubicompp3.viewmodel.DashboardViewModel
import com.example.ubicompp3.viewmodel.DashboardViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        requestNotificationPermissionIfNeeded()

        setContent {
            UbiCompP3Theme {
                val navController = rememberNavController()
                val vm: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(applicationContext)
                )

                LaunchedEffect(Unit) {
                    vm.connect()
                }

                Scaffold(
                    bottomBar = {
                        BottomNavBar(navController = navController)
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        AppNavHost(
                            navController = navController,
                            viewModel = vm
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}