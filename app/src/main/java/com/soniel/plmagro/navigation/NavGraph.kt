package com.soniel.plmagro.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soniel.plmagro.ui.screens.*
import com.soniel.plmagro.ui.permissions.LocationPermissionIntroScreen
import com.soniel.plmagro.viewmodel.ConfiguracoesViewModel
import com.soniel.plmagro.viewmodel.DiagnosticViewModel
import com.soniel.plmagro.viewmodel.LinkFleetViewModel
import com.soniel.plmagro.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object VehicleConfig : Screen("vehicle_config")
    object StartJourney : Screen("start_journey")
    object Dashboard : Screen("dashboard")
    object InformOperation : Screen("inform_operation")
    object InformStop : Screen("inform_stop")
    object Refueling : Screen("refueling")
    object AutomaticStop : Screen("automatic_stop")
    object EndJourney : Screen("end_journey")
    object Reports : Screen("reports")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Ocorrencias : Screen("ocorrencias")
    object WialonDiagnostic : Screen("wialon_diagnostic")
    object WialonIpsAdmin : Screen("wialon_ips_admin")
    object LinkFleet : Screen("link_fleet")
    object PermissionIntro : Screen("permission_intro")
    object Logbook : Screen("logbook")
    object About : Screen("about")
}

@Composable
fun NavGraph(
    navController: NavHostController, 
    viewModel: MainViewModel,
    diagnosticViewModel: DiagnosticViewModel,
    configuracoesViewModel: ConfiguracoesViewModel,
    linkFleetViewModel: LinkFleetViewModel,
    permissionsGranted: Boolean,
    permissionsDenied: Boolean,
    onActivateTelemetry: () -> Unit,
    onOpenGpsSettings: () -> Unit
) {
    val vehicleConfig by viewModel.vehicleConfig.collectAsState()
    val activeJourney by viewModel.activeJourney.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val satelliteCount by viewModel.satelliteCount.collectAsState()
    val showAutoStop by viewModel.showAutoStopPopup.collectAsState()
    val activeVinculo by viewModel.activeVinculo.collectAsState()
    val locationIntroDone by viewModel.locationIntroDone.collectAsState()
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsState()

    // 1. Iniciar serviço se tiver permissão e telemetria habilitada
    LaunchedEffect(permissionsGranted, telemetryEnabled) {
        if (permissionsGranted && telemetryEnabled) {
            com.soniel.plmagro.service.TelemetryForegroundService.start(navController.context)
        }
    }

    LaunchedEffect(showAutoStop) {
        val currentRoute = navController.currentDestination?.route
        if (showAutoStop && currentRoute != Screen.EndJourney.route && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.AutomaticStop.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onNext = {
                if (activeJourney != null) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else if (!locationIntroDone) {
                    navController.navigate(Screen.PermissionIntro.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else if (vehicleConfig == null) {
                    navController.navigate(Screen.VehicleConfig.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            })
        }
        composable(Screen.VehicleConfig.route) {
            SettingsScreen(
                viewModel = viewModel,
                diagnosticViewModel = diagnosticViewModel,
                configuracoesViewModel = configuracoesViewModel,
                onBack = { 
                    if (vehicleConfig != null) {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.VehicleConfig.route) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.VehicleConfig.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToDiagnostic = { navController.navigate(Screen.WialonDiagnostic.route) },
                onNavigateToLinkFleet = { navController.navigate(Screen.LinkFleet.route) },
                onNavigateToWialonIpsAdmin = { navController.navigate(Screen.WialonIpsAdmin.route) }
            )
        }
        composable(Screen.PermissionIntro.route) {
            // Função auxiliar de navegação para evitar repetição
            fun navigateNext() {
                if (vehicleConfig == null) {
                    navController.navigate(Screen.VehicleConfig.route) {
                        popUpTo(Screen.PermissionIntro.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.PermissionIntro.route) { inclusive = true }
                    }
                }
            }

            // 1. Se as permissões forem concedidas, salva e navega IMEDIATAMENTE
            LaunchedEffect(permissionsGranted) {
                if (permissionsGranted) {
                    viewModel.setLocationIntroDone()
                    navigateNext()
                }
            }

            // 2. Se o estado do DataStore mudar para concluído (ex: via botão offline)
            LaunchedEffect(locationIntroDone) {
                if (locationIntroDone) {
                    navigateNext()
                }
            }

            LocationPermissionIntroScreen(
                onActivate = onActivateTelemetry,
                onContinueWithoutGps = {
                    viewModel.setLocationIntroDoneWithoutTelemetry()
                },
                onOpenGpsSettings = onOpenGpsSettings,
                permissionsDenied = permissionsDenied
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { matricula ->
                    viewModel.setLoggedMatricula(matricula)
                    navController.navigate(Screen.StartJourney.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.VehicleConfig.route)
                }
            )
        }
        composable(Screen.StartJourney.route) {
            StartJourneyScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStart = { km, op, cc ->
                    viewModel.loginAndStartJourney(km.toIntOrNull() ?: 0, op, cc)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
            DashboardScreen(
                viewModel = viewModel,
                speed = speed,
                vehicleId = activeVinculo?.codigoFrotaLocal ?: vehicleConfig?.fleetCode ?: "---",
                vehiclePlate = activeVinculo?.placa ?: vehicleConfig?.plate ?: "---",
                wialonUnitName = activeVinculo?.wialonNome ?: vehicleConfig?.wialonUnitName,
                operatorName = activeJourney?.operatorMatricula?.let { "Operador $it" } ?: "---",
                kmAtual = activeJourney?.lastKm?.toString() ?: "0",
                kmRodado = "%.2f".format((activeJourney?.accumulatedDistance ?: 0.0) / 1000.0),
                gpsLocation = currentLocation?.let { "${"%.4f".format(it.first)}, ${"%.4f".format(it.second)}" } 
                    ?: activeJourney?.lastLat?.let { lat -> activeJourney?.lastLng?.let { lng -> "${"%.4f".format(lat)}, ${"%.4f".format(lng)}" } } 
                    ?: "---",
                currentTime = dateFormat.format(Date()),
                satelliteCount = satelliteCount,
                gpsSignalStatus = if (satelliteCount > 0 || currentLocation != null) "Ativo" else "Sem Sinal",
                onInformOperation = { navController.navigate(Screen.InformOperation.route) },
                onInformStop = { navController.navigate(Screen.InformStop.route) },
                onEndJourney = { 
                    viewModel.prepareEndJourney()
                    navController.navigate(Screen.EndJourney.route) 
                },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToLogbook = { navController.navigate(Screen.Logbook.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToJourney = {
                    if (activeJourney == null) {
                        navController.navigate(Screen.StartJourney.route)
                    } else {
                        navController.navigate(Screen.Logbook.route)
                    }
                },
                onNavigateToStops = { navController.navigate(Screen.InformStop.route) },
                onNavigateToEvents = { navController.navigate(Screen.Ocorrencias.route) },
                onNavigateToDiagnostic = { navController.navigate(Screen.WialonDiagnostic.route) },
                onNavigateToIpsAdmin = { navController.navigate(Screen.WialonIpsAdmin.route) },
                onNavigateToLinkFleet = { navController.navigate(Screen.LinkFleet.route) },
                onNavigateToOperationalSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.InformOperation.route) {
            InformOperationScreen(
                onBack = { navController.popBackStack() },
                onSave = { code, cc -> 
                    viewModel.registerEvent("OPERACAO", code, activeJourney?.lastKm ?: 0) 
                    navController.popBackStack() 
                }
            )
        }
        composable(Screen.InformStop.route) {
            InformStopScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToRefueling = { 
                    navController.navigate(Screen.Refueling.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onShiftChangeComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Refueling.route) {
            RefuelingScreen(
                onBack = { navController.popBackStack() },
                onConfirm = { liters, km -> 
                    viewModel.registerEvent("ABASTECIMENTO", "$liters L", km.toIntOrNull() ?: 0)
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                }
            )
        }
        composable(Screen.AutomaticStop.route) {
            AutomaticStopScreen(
                onInformReason = { 
                    viewModel.closeAutoStopPopup()
                    navController.navigate(Screen.InformStop.route) {
                        popUpTo(Screen.AutomaticStop.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.EndJourney.route) {
            var summary by remember { mutableStateOf<com.soniel.plmagro.viewmodel.JourneySummary?>(null) }
            LaunchedEffect(Unit) {
                summary = viewModel.getJourneySummary()
            }
            
            EndJourneyScreen(
                kmInicial = activeJourney?.kmInicial ?: 0,
                kmAtual = activeJourney?.lastKm ?: 0,
                vehicleId = activeVinculo?.codigoFrotaLocal ?: vehicleConfig?.fleetCode ?: "---",
                operatorName = activeJourney?.operatorMatricula?.let { "Operador $it" } ?: "---",
                summary = summary,
                onBack = { navController.popBackStack() },
                onFinish = { kmFinal ->
                    viewModel.endJourney(kmFinal)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Reports.route) { ReportsScreen(viewModel = viewModel) }
        composable(Screen.History.route) { 
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            ) 
        }
        composable(Screen.Ocorrencias.route) { OcorrenciasScreen(viewModel = viewModel) }
        composable(Screen.Logbook.route) {
            LogbookScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                diagnosticViewModel = diagnosticViewModel,
                configuracoesViewModel = configuracoesViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDiagnostic = { navController.navigate(Screen.WialonDiagnostic.route) },
                onNavigateToLinkFleet = { navController.navigate(Screen.LinkFleet.route) },
                onNavigateToWialonIpsAdmin = { navController.navigate(Screen.WialonIpsAdmin.route) }
            )
        }
        composable(Screen.WialonDiagnostic.route) {
            WialonDiagnosticScreen(
                viewModel = viewModel,
                diagnosticViewModel = diagnosticViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.WialonIpsAdmin.route) {
            WialonIpsAdminScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LinkFleet.route) {
            LinkFleetScreen(
                viewModel = viewModel,
                linkFleetViewModel = linkFleetViewModel,
                configuracoesViewModel = configuracoesViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    if (!navController.popBackStack(Screen.Dashboard.route, inclusive = false)) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Screen.About.route) {
            AboutSystemScreen(onBack = { navController.popBackStack() })
        }
    }
}
