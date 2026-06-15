package com.informatika.sars.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StudentDashboard : Screen("student_dashboard")
    object LecturerDashboard : Screen("lecturer_dashboard")
    object AslabDashboard : Screen("aslab_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object StudentRequest : Screen("student_request")
}

sealed class BottomNavItem(val route: String, val icon: Int, val label: String) {
    // Icons will be replaced with actual vector resources or material icons
    object Schedule : BottomNavItem("schedule", 0, "Schedule")
    object Requests : BottomNavItem("requests", 0, "Requests")
    object Validate : BottomNavItem("validate", 0, "Validate")
    object Profile : BottomNavItem("profile", 0, "Profile")
    object Dashboard : BottomNavItem("admin_dashboard", 0, "Dashboard")
    object Approvals : BottomNavItem("approvals", 0, "Approvals")
}
