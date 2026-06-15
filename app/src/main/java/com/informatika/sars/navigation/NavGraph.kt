package com.informatika.sars.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    
    // Logic Flaw Fix: Pantau perubahan status login secara global
    LaunchedEffect(currentUser, isInitializing) {
        if (!isInitializing && currentUser == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // 1. Tampilkan Loading Screen saat aplikasi pertama kali dibuka (menghindari flicker)
    if (isInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 2. Tentukan start destination
    val startDest = if (currentUser != null) {
        when(currentUser?.role) {
            UserRole.LECTURER -> Screen.LecturerDashboard.route
            UserRole.ASLAB -> Screen.AslabDashboard.route
            else -> Screen.StudentDashboard.route
        }
    } else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDest
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
                onNavigateToRequestScreen = {
                    navController.navigate(Screen.StudentRequest.route)
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
                onBack = { navController.navigateUp() }
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
