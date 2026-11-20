package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.local.TokenManager
import com.example.myapplication.ui.dashboard.DashboardScreen
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.logs.LogsScreen
import com.example.myapplication.ui.query.QueryScreen
import com.example.myapplication.ui.results.ResultsScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object DashboardRoute

@Serializable
object QueryRoute

@Serializable
object ResultsRoute

@Serializable
object LogsRoute

@Composable
fun AppNavigation(
    tokenManager: TokenManager,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val initialToken by tokenManager.authTokenFlow.collectAsState(initial = null)
    val startDestination: Any = if (!initialToken.isNull_or_blank_safe()) DashboardRoute else LoginRoute

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(DashboardRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<DashboardRoute> {
            DashboardScreen(
                onNavigateToQuery = { navController.navigate(QueryRoute) },
                onNavigateToResults = { navController.navigate(ResultsRoute) },
                onNavigateToLogs = { navController.navigate(LogsRoute) },
                onLogout = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<QueryRoute> {
            QueryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToResults = { navController.navigate(ResultsRoute) },
                onNavigateToLogs = { navController.navigate(LogsRoute) }
            )
        }

        composable<ResultsRoute> {
            ResultsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToQuery = { navController.navigate(QueryRoute) }
            )
        }

        composable<LogsRoute> {
            LogsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun String?.isNull_or_blank_safe(): Boolean {
    return this.isNullOrBlank()
}
