package com.mtginventory.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collection
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mtginventory.app.ui.analytics.AnalyticsScreen
import com.mtginventory.app.ui.collection.CollectionScreen
import com.mtginventory.app.ui.deck.DecksScreen
import com.mtginventory.app.ui.scanner.ScannerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MTGInventoryApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scanner.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scanner.route) {
                ScannerScreen(navController = navController)
            }
            composable(Screen.Collection.route) {
                CollectionScreen(navController = navController)
            }
            composable(Screen.Decks.route) {
                DecksScreen(navController = navController)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(navController = navController)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Scanner : Screen("scanner", "Scan", Icons.Default.CameraAlt)
    object Collection : Screen("collection", "Collection", Icons.Default.Collection)
    object Decks : Screen("decks", "Decks", Icons.Default.Deck)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
}

val bottomNavItems = listOf(
    Screen.Scanner,
    Screen.Collection,
    Screen.Decks,
    Screen.Analytics
)