package com.informatika.sars.ui.screens.lecturer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatika.sars.ui.components.ScheduleCard
import com.informatika.sars.viewmodel.AuthViewModel
import com.informatika.sars.viewmodel.DashboardViewModel
import com.informatika.sars.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerDashboard(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    notificationViewModel: NotificationViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val schedules by dashboardViewModel.schedules.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            dashboardViewModel.fetchData(user)
            dashboardViewModel.startListeningToRequests(user) { title, message ->
                notificationViewModel.triggerNotification(title, message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lecturer Dashboard", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* New Schedule */ },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Schedule") },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Your Teaching Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (isLoading && schedules.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (schedules.isEmpty()) {
                item {
                    Text("No schedules found.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(schedules.filter { it.lecturerName == currentUser?.name }) { item ->
                    ScheduleCard(
                        title = item.course?.name ?: "No Subject",
                        lecturer = item.lecturerName,
                        room = item.room?.name ?: "No Room",
                        time = "${item.startTime} - ${item.endTime}",
                        semester = item.semester?.name,
                        status = item.status ?: "UPCOMING",
                        color = if (item.status == "LIVE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Student Attendance", fontWeight = FontWeight.Bold)
                            Text("Average: 92% this week", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
