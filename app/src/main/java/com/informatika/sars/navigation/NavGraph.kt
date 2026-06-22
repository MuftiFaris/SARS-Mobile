package com.informatika.sars.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.informatika.sars.ui.screens.auth.LoginScreen
import com.informatika.sars.ui.screens.student.StudentDashboard
import com.informatika.sars.ui.screens.lecturer.LecturerDashboard
import com.informatika.sars.data.model.UserRole
import com.informatika.sars.viewmodel.AuthViewModel
import com.informatika.sars.viewmodel.ChatViewModel
import com.informatika.sars.viewmodel.DashboardViewModel
import com.informatika.sars.viewmodel.NotificationViewModel
import com.informatika.sars.viewmodel.ThemeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    themeViewModel: ThemeViewModel,
    chatViewModel: ChatViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isInitializing by authViewModel.isInitializing.collectAsState()
    
    // Auto navigate based on auth state
    LaunchedEffect(isInitializing, currentUser) {
        if (!isInitializing) {
            if (currentUser == null) {
                // Redirect to login if user is null (initially or after logout)
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                // If on login screen, move to dashboard
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Screen.Login.route) {
                    val dest = when(currentUser?.role) {
                        UserRole.LECTURER -> Screen.LecturerDashboard.route
                        UserRole.ASLAB -> Screen.AslabDashboard.route
                        else -> Screen.StudentDashboard.route
                    }
                    navController.navigate(dest) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { role ->
                    val dest = when(role) {
                        UserRole.LECTURER -> Screen.LecturerDashboard.route
                        UserRole.ASLAB -> Screen.AslabDashboard.route
                        else -> Screen.StudentDashboard.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.StudentDashboard.route) {
            StudentDashboard(
                authViewModel = authViewModel,
                notificationViewModel = notificationViewModel,
                themeViewModel = themeViewModel,
                chatViewModel = chatViewModel,
                dashboardViewModel = dashboardViewModel,
                initialTab = 0,
                onRequestNew = {
                    navController.navigate(Screen.StudentRequest.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable("${Screen.StudentDashboard.route}/{tab}") {
            val tabArg = it.arguments?.getString("tab")?.toIntOrNull() ?: 0
            StudentDashboard(
                authViewModel = authViewModel,
                notificationViewModel = notificationViewModel,
                themeViewModel = themeViewModel,
                chatViewModel = chatViewModel,
                dashboardViewModel = dashboardViewModel,
                initialTab = tabArg,
                onRequestNew = {
                    navController.navigate(Screen.StudentRequest.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StudentRequest.route) {
            val schedules by dashboardViewModel.schedules.collectAsState()
            val rooms by dashboardViewModel.rooms.collectAsState()
            val requests by dashboardViewModel.requests.collectAsState()
            val currentUser by authViewModel.currentUser.collectAsState()
            
            com.informatika.sars.ui.screens.student.StudentRequestScreen(
                schedules = schedules,
                rooms = rooms,
                requests = requests,
                currentUser = currentUser,
                dashboardViewModel = dashboardViewModel,
                onBack = { 
                    navController.navigate("${Screen.StudentDashboard.route}/2") {
                        popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LecturerDashboard.route) {
            LecturerDashboard(authViewModel, dashboardViewModel, notificationViewModel)
        }

        composable(Screen.AslabDashboard.route) {
            com.informatika.sars.ui.screens.aslab.AslabDashboard(
                authViewModel,
                dashboardViewModel,
                notificationViewModel,
                themeViewModel
            )
        }
    }
}
