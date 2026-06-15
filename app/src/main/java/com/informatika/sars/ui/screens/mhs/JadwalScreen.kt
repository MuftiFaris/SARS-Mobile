package com.informatika.sars.ui.screens.mhs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.informatika.sars.data.model.ScheduleItem
import com.informatika.sars.data.model.Room
import java.util.Calendar

@Composable
fun JadwalScreen(
    schedules: List<ScheduleItem>,
    rooms: List<Room>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val days = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT")
    val calendar = Calendar.getInstance()
    val todayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "SENIN"
        Calendar.TUESDAY -> "SELASA"
        Calendar.WEDNESDAY -> "RABU"
        Calendar.THURSDAY -> "KAMIS"
        Calendar.FRIDAY -> "JUMAT"
        else -> "SENIN"
    }
    var selectedDay by remember { mutableStateOf(todayName) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Tab Hari (Tetap statis di atas, tidak ikut di-zoom)
        SecondaryScrollableTabRow(
            selectedTabIndex = days.indexOf(selectedDay),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(days.indexOf(selectedDay)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            days.forEach { day ->
                Tab(
                    selected = selectedDay == day,
                    onClick = { selectedDay = day },
                    text = {
                        Text(
                            text = day.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Konten yang bisa di-zoom & pan
            ZoomableJadwalContainer {
                JadwalGridContent(
                    selectedDay = selectedDay,
                    schedules = schedules,
                    rooms = rooms
                )
            }
        }
    }
}

@Composable
fun ZoomableJadwalContainer(content: @Composable () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        
        // Batasi Offset agar konten tidak hilang ke luar layar
        val maxOffset = 500f * scale
        offset = Offset(
            x = (offset.x + offsetChange.x).coerceIn(-maxOffset, maxOffset),
            y = (offset.y + offsetChange.y).coerceIn(-maxOffset, maxOffset)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RectangleShape) // Penting: Biar konten gak luber saat di-zoom
            .transformable(state = state)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // KUNCI: Menggunakan graphicsLayer agar transformasi dilakukan oleh GPU
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            content()
        }
    }
}

@Composable
fun JadwalGridContent(
    selectedDay: String,
    schedules: List<ScheduleItem>,
    rooms: List<Room>
) {
    var selectedScheduleForDetail by remember { mutableStateOf<ScheduleItem?>(null) }
    
    val schedulesByRoom = remember(schedules, selectedDay) {
        schedules.filter { 
            it.day.trim().uppercase() == selectedDay.trim().uppercase() 
        }.groupBy { it.room?.name ?: "" }
    }
    
    val sessions = remember { (1..11).toList() }
    val cellWidth = 140.dp
    val cellHeight = 100.dp
    val roomHeaderWidth = 80.dp

    Column(modifier = Modifier.padding(16.dp)) {
        // Header Sesi
        Row {
            Box(modifier = Modifier.width(roomHeaderWidth).height(40.dp)) // Corner box
            sessions.forEach { session ->
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .height(40.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SESI $session", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Body Grid (Menggunakan Column biasa di dalam Zoomable biar lebih stabil saat transformasi GPU)
        rooms.forEach { room ->
            Row {
                // Nama Ruangan
                Box(
                    modifier = Modifier
                        .width(roomHeaderWidth)
                        .height(cellHeight)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(room.name ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                val roomSchedules = schedulesByRoom[room.name] ?: emptyList()
                
                var currentSession = 1
                while (currentSession <= 11) {
                    val schedule = roomSchedules.find { it.sessionStart == currentSession }
                    if (schedule != null) {
                        val duration = schedule.sessionDuration ?: 1
                        Box(
                            modifier = Modifier
                                .width(cellWidth * duration)
                                .height(cellHeight)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(4.dp)
                        ) {
                            ScheduleGridItemSmall(schedule) { selectedScheduleForDetail = it }
                        }
                        currentSession += duration
                    } else {
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(cellHeight)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        )
                        currentSession++
                    }
                }
            }
        }
    }

    if (selectedScheduleForDetail != null) {
        ScheduleDetailDialog(
            schedule = selectedScheduleForDetail!!,
            onDismiss = { selectedScheduleForDetail = null }
        )
    }
}

@Composable
fun ScheduleGridItemSmall(schedule: ScheduleItem, onClick: (ScheduleItem) -> Unit) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentColor = MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier.fillMaxSize().clickable { onClick(schedule) },
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = if (isDark) 0.2f else 0.1f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                "${schedule.course?.description ?: schedule.semester?.name ?: "Semester ${schedule.semesterId ?: "-"}"} • Kelas ${schedule.course?.className ?: "-"}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 7.sp,
                color = accentColor.copy(alpha = 0.8f),
                maxLines = 1
            )
            Text(
                schedule.course?.name ?: "-",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                color = accentColor
            )
            Text(
                schedule.lecturerName,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScheduleDetailDialog(schedule: ScheduleItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Detail Jadwal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailItem("Mata Kuliah", schedule.course?.name ?: "-")
                DetailItem("Semester", schedule.course?.description ?: schedule.semester?.name ?: "Semester ${schedule.semesterId ?: "-"}")
                DetailItem("Dosen", schedule.lecturerName)
                DetailItem("Waktu", "${schedule.startTime} - ${schedule.endTime} (Sesi ${schedule.sessionStart})")
                DetailItem("Ruangan", schedule.room?.name ?: "-")
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
