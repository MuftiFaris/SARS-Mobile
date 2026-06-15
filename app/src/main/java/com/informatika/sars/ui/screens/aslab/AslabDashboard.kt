package com.informatika.sars.ui.screens.aslab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatika.sars.ui.components.DashboardCard
import com.informatika.sars.ui.theme.Success
import com.informatika.sars.ui.theme.Warning
import com.informatika.sars.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AslabDashboard(
    authViewModel: com.informatika.sars.viewmodel.AuthViewModel,
    dashboardViewModel: com.informatika.sars.viewmodel.DashboardViewModel,
    notificationViewModel: com.informatika.sars.viewmodel.NotificationViewModel,
    themeViewModel: com.informatika.sars.viewmodel.ThemeViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val requests by dashboardViewModel.requests.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    
    val pendingRequests = requests.filter { it.status == com.informatika.sars.data.model.RequestStatus.PENDING }
    val validatedRequests = requests.filter { it.status != com.informatika.sars.data.model.RequestStatus.PENDING }

    LaunchedEffect(currentUser) {
        dashboardViewModel.fetchData(currentUser)
        dashboardViewModel.startListeningToRequests(currentUser) { title, message ->
            notificationViewModel.triggerNotification(title, message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aslab Validation", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { dashboardViewModel.fetchData(currentUser) },
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ValidationStat("Pending", pendingRequests.size.toString(), Warning, Modifier.weight(1f))
                        ValidationStat("Processed", validatedRequests.size.toString(), Success, Modifier.weight(1f))
                    }
                }

                item {
                    Text("Need Validation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pendingRequests.isEmpty()) {
                        DashboardCard {
                            Text("No pending requests to validate.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        DashboardCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                pendingRequests.forEachIndexed { index, request ->
                                    ValidationItem(
                                        student = request.studentName ?: "Unknown",
                                        room = request.room ?: "-",
                                        time = request.timeSlot ?: "-",
                                        subject = request.subject ?: "-",
                                        onApprove = { request.id?.let { dashboardViewModel.updateRequestStatus(it, com.informatika.sars.data.model.RequestStatus.APPROVED) } },
                                        onReject = { request.id?.let { dashboardViewModel.updateRequestStatus(it, com.informatika.sars.data.model.RequestStatus.REJECTED) } }
                                    )
                                    if (index < pendingRequests.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Text("Recent History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    validatedRequests.take(5).forEach { request ->
                        RequestHistoryItem(request)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun ValidationItem(
    student: String, 
    room: String, 
    time: String, 
    subject: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(student, fontWeight = FontWeight.Bold)
            Text(subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text("$room • $time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            FilledTonalIconButton(
                onClick = onReject, 
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onApprove, 
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Success)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun RequestHistoryItem(request: com.informatika.sars.data.model.ValidationRequest) {
    val statusColor = when(request.status) {
        com.informatika.sars.data.model.RequestStatus.APPROVED -> Success
        com.informatika.sars.data.model.RequestStatus.REJECTED -> MaterialTheme.colorScheme.error
        else -> Warning
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(request.studentName ?: "Unknown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("${request.room} • ${request.subject}", style = MaterialTheme.typography.labelSmall)
            }
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    request.status.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ValidationStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
