package com.informatika.sars.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatika.sars.ui.components.DashboardCard
import com.informatika.sars.ui.theme.Success
import com.informatika.sars.ui.theme.Warning
import com.informatika.sars.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(notificationViewModel: NotificationViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
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
                Text(
                    "Sistem Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("Users", "124", Icons.Default.People, Modifier.weight(1f))
                    StatCard("Rooms", "12", Icons.Default.MeetingRoom, Modifier.weight(1f))
                }
            }

            item {
                SectionHeader("Pending Approvals")
                DashboardCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminRequestItem("Aula Utama", "Seminar Nasional", "Student A")
                        HorizontalDivider()
                        AdminRequestItem("Lab 404", "Riset IoT", "Student B")
                    }
                }
            }

            item {
                SectionHeader("System Health")
                DashboardCard {
                    HealthRow("Database", "Healthy", Success)
                    HealthRow("Storage", "Healthy", Success)
                    HealthRow("Auth Service", "Warning", Warning)
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun AdminRequestItem(room: String, event: String, requester: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(room, fontWeight = FontWeight.Bold)
            Text("$event • $requester", style = MaterialTheme.typography.bodySmall)
        }
        Row {
            IconButton(onClick = { /* Reject */ }) {
                Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = { /* Approve */ }) {
                Icon(Icons.Default.Check, contentDescription = "Approve", tint = Success)
            }
        }
    }
}

@Composable
fun HealthRow(service: String, status: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(service)
        Text(status, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
