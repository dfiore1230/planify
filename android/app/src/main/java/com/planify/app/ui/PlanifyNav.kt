package com.planify.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.planify.app.LocalInstanceStore
import com.planify.app.ui.screens.*

sealed class TabDestination(val route: String, val label: String, val icon: ImageVector) {
    object Events : TabDestination("events", "Events", Icons.Filled.CalendarToday)
    object Talent : TabDestination("talent", "Talent", Icons.Filled.Groups)
    object Venues : TabDestination("venues", "Venues", Icons.Filled.LocationCity)
    object Tickets : TabDestination("tickets", "Tickets", Icons.Filled.ConfirmationNumber)
    object Settings : TabDestination("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun PlanifyNav() {
    val navController = rememberNavController()
    val tabs = remember { listOf(TabDestination.Events, TabDestination.Talent, TabDestination.Venues, TabDestination.Tickets, TabDestination.Settings) }

    val instanceStore = LocalInstanceStore.current
    val active = instanceStore.activeInstance()

    LaunchedEffect(active?.id) {
        val target = if (active != null) TabDestination.Events.route else "onboarding"
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (active != null) {
                BottomNavBar(navController, tabs)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (active == null) "onboarding" else TabDestination.Events.route,
            modifier = Modifier.padding(padding)
        ) {
            composable("onboarding") { InstanceOnboardingScreen(onAddServer = { navController.navigate("settings/add-server") }) }
            composable(TabDestination.Events.route) { EventsListScreen(navController) }
            composable("events/detail/{eventId}") { backStack ->
                val eventId = backStack.arguments?.getString("eventId") ?: ""
                EventDetailScreen(navController, eventId)
            }
            composable("events/new") { EventFormScreen(navController, null) }
            composable("events/edit/{eventId}") { backStack ->
                val eventId = backStack.arguments?.getString("eventId") ?: ""
                EventFormScreen(navController, eventId)
            }

            composable(TabDestination.Talent.route) { TalentListScreen(navController) }
            composable("talent/detail/{talentId}") { backStack ->
                val id = backStack.arguments?.getString("talentId") ?: "0"
                TalentDetailScreen(navController, id.toInt())
            }
            composable("talent/new") { TalentFormScreen(navController, null) }
            composable("talent/edit/{talentId}") { backStack ->
                val id = backStack.arguments?.getString("talentId") ?: "0"
                TalentFormScreen(navController, id.toInt())
            }

            composable(TabDestination.Venues.route) { VenueListScreen(navController) }
            composable("venues/detail/{venueId}") { backStack ->
                val id = backStack.arguments?.getString("venueId") ?: "0"
                VenueDetailScreen(navController, id.toInt())
            }
            composable("venues/new") { VenueFormScreen(navController, null) }
            composable("venues/edit/{venueId}") { backStack ->
                val id = backStack.arguments?.getString("venueId") ?: "0"
                VenueFormScreen(navController, id.toInt())
            }

            composable(TabDestination.Tickets.route) { TicketListScreen(navController) }
            composable("tickets/detail/{ticketId}") { backStack ->
                val id = backStack.arguments?.getString("ticketId") ?: "0"
                TicketDetailScreen(navController, id.toInt())
            }

            composable(TabDestination.Settings.route) { SettingsScreen(navController) }
            composable("settings/add-server") { ServerFormScreen(navController, null) }
            composable("settings/edit-server/{serverId}") { backStack ->
                val id = backStack.arguments?.getString("serverId") ?: ""
                ServerFormScreen(navController, id)
            }

            composable("scanner/{eventId}") { backStack ->
                val eventId = backStack.arguments?.getString("eventId") ?: ""
                QRScannerScreen(navController, eventId)
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController, tabs: List<TabDestination>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
