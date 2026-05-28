package com.soniel.plmagro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.soniel.plmagro.navigation.NavGraph
import com.soniel.plmagro.ui.theme.PlmAgroTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soniel.plmagro.service.TelemetryForegroundService
import com.soniel.plmagro.viewmodel.ConfiguracoesViewModel
import com.soniel.plmagro.viewmodel.DiagnosticViewModel
import com.soniel.plmagro.viewmodel.LinkFleetViewModel
import com.soniel.plmagro.viewmodel.MainViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.soniel.plmagro.model.PlmRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionsDenied = mutableStateOf(false)
    private val permissionsGranted = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (!fine && !coarse) {
            permissionsDenied.value = true
        } else {
            permissionsDenied.value = false
            permissionsGranted.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionsGranted.value = hasRequiredPermissions()
        // Removed checkAndRequestPermissions() to avoid blocking start
        
        val repository = (application as PlmApplication).repository
        val wialonRepository = (application as PlmApplication).wialonRepository
        val sessionManager = (application as PlmApplication).sessionManager
        val userPreferencesManager = (application as PlmApplication).userPreferencesManager
        val diagnosticRepository = (application as PlmApplication).diagnosticRepository
        val outboxManager = (application as PlmApplication).outboxManager
        val sensorWatchdog = (application as PlmApplication).sensorWatchdog
        val alertManager = com.soniel.plmagro.core.utils.AlertManager(this)
        
        // Removido start automático do TelemetryForegroundService aqui
        // O serviço será iniciado no NavGraph após as permissões serem validadas

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(repository, wialonRepository, sessionManager, userPreferencesManager, sensorWatchdog, diagnosticRepository, outboxManager, alertManager) as T
                    }
                }
            )
            val isNightMode by viewModel.isNightMode.collectAsState()

            PlmAgroTheme(isNightMode = isNightMode) {
                
                // Re-check permissions on activity resume
                androidx.compose.runtime.DisposableEffect(Unit) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            permissionsGranted.value = hasRequiredPermissions()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                val diagnosticViewModel: DiagnosticViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return DiagnosticViewModel(diagnosticRepository) as T
                        }
                    }
                )
                val configuracoesViewModel: ConfiguracoesViewModel = viewModel()
                val linkFleetViewModel: LinkFleetViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LinkFleetViewModel(repository, wialonRepository, sessionManager) as T
                        }
                    }
                )
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                        NavGraph(
                            navController = navController, 
                            viewModel = viewModel, 
                            diagnosticViewModel = diagnosticViewModel,
                            configuracoesViewModel = configuracoesViewModel,
                            linkFleetViewModel = linkFleetViewModel,
                            permissionsGranted = permissionsGranted.value,
                            permissionsDenied = permissionsDenied.value,
                            onActivateTelemetry = { checkAndRequestPermissions() },
                            onOpenGpsSettings = { openGpsSettings() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun openGpsSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun hasRequiredPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        // Para iniciar a telemetria e o GPS, basta a permissão em primeiro plano (Durante o uso)
        return fine || coarse
    }
}
