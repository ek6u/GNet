package com.pira.gnetp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pira.gnetp.data.LogRepository
import com.pira.gnetp.navigation.Screen
import com.pira.gnetp.ui.home.HomeScreen
import com.pira.gnetp.ui.home.HomeViewModel
import com.pira.gnetp.ui.hotspot.HotspotScreen
import com.pira.gnetp.ui.logs.LogsScreen
import com.pira.gnetp.ui.settings.SettingsScreen
import com.pira.gnetp.ui.theme.GNetTheme
import com.pira.gnetp.ui.theme.ThemeManager
import com.pira.gnetp.ui.theme.ThemeSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var logRepository: LogRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            MainApp(logRepository)
        }
    }
}

@Composable
fun MainApp(logRepository: LogRepository) {
    val navController = rememberNavController()
    val themeManager = ThemeManager(androidx.compose.ui.platform.LocalContext.current)
    var themeSettings by remember { mutableStateOf(themeManager.loadThemeSettings()) }
    
    // Callback to update theme settings
    fun updateThemeSettings(newSettings: ThemeSettings) {
        themeSettings = newSettings
        themeManager.saveThemeSettings(newSettings)
    }
    
    GNetTheme(themeSettings = themeSettings) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController)
                }
            ) { innerPadding ->
                MainNavHost(
                    navController = navController,
                    logRepository = logRepository,
                    modifier = Modifier.padding(innerPadding),
                    onThemeSettingsChanged = ::updateThemeSettings
                )
            }
        }
    }
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    logRepository: LogRepository,
    modifier: Modifier = Modifier,
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {}
) {
    // Create a single instance of the HomeViewModel to be shared between HomeScreen and HotspotScreen
    val homeViewModel = hiltViewModel<HomeViewModel>()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                uiState = homeUiState,
                onStartProxy = { homeViewModel.startProxy() },
                onStopProxy = { homeViewModel.stopProxy() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHotspot = { navController.navigate(Screen.Hotspot.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onVpnPermissionRequest = { },
                onSelectIpAddress = { ip -> homeViewModel.selectIpAddress(ip) }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onThemeSettingsChanged = onThemeSettingsChanged
            )
        }
        
        composable(Screen.Hotspot.route) {
            HotspotScreen(
                uiState = homeUiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Logs.route) {
            LogsScreen(
                onNavigateBack = { navController.popBackStack() },
                logRepository = logRepository
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.Hotspot,
        Screen.Logs,
        Screen.Settings
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.Home -> Icons.Default.Home
                            Screen.Settings -> Icons.Default.Settings
                            Screen.Hotspot -> Icons.Default.Info
                            Screen.Logs -> Icons.AutoMirrored.Filled.List
                        },
                        contentDescription = null
                    )
                },
                label = { Text(getScreenTitle(screen)) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    // Special handling for Home screen to ensure proper navigation
                    if (screen == Screen.Home) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

fun getScreenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Home -> "Home"
        Screen.Settings -> "Settings"
        Screen.Hotspot -> "Hotspot"
        Screen.Logs -> "Logs"
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    val themeManager = ThemeManager(androidx.compose.ui.platform.LocalContext.current)
    val themeSettings = themeManager.loadThemeSettings()
    
    GNetTheme(themeSettings = themeSettings) {
        MainApp(logRepository = LogRepository())
    }
}