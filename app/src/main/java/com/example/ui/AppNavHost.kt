package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.ParkingLocation

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: ParkingViewModel,
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onEditLocation = { location ->
                    navController.navigate("edit/${location.id}")
                }
            )
        }
        composable("edit/{locationId}") { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId")?.toIntOrNull()
            if(locationId != null) {
                EditLocationScreen(
                    viewModel = viewModel,
                    locationId = locationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
