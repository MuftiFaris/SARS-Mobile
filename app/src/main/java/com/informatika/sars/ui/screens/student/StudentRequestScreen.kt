package com.informatika.sars.ui.screens.student

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.informatika.sars.ui.components.DashboardCard
import com.informatika.sars.ui.components.PrimaryButton
import com.informatika.sars.ui.components.SecondaryButton
import com.informatika.sars.ui.components.StepProgressHeader
import androidx.compose.ui.window.DialogProperties
import com.informatika.sars.data.model.RequestStatus
import com.informatika.sars.data.model.Room
import com.informatika.sars.data.model.ScheduleItem
import com.informatika.sars.data.model.User
import com.informatika.sars.data.model.ValidationRequest
import com.informatika.sars.ui.components.DashboardCard
import com.informatika.sars.ui.components.PrimaryButton
import com.informatika.sars.ui.components.SecondaryButton
import com.informatika.sars.ui.theme.Error
import com.informatika.sars.ui.theme.Success
import com.informatika.sars.ui.theme.Warning
import com.informatika.sars.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentRequestScreen(
    schedules: List<ScheduleItem>,
    rooms: List<Room>,
    requests: List<ValidationRequest>,
    currentUser: User?,
    dashboardViewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isSubmitting by dashboardViewModel.isSubmitting.collectAsState()
    val submitSuccess by dashboardViewModel.submitSuccess.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }

    // Back Handler: intercept back presses to go back one step instead of exiting
    androidx.activity.compose.BackHandler(enabled = true) {
        if (currentStep > 1) {
            currentStep--
        } else {
            onBack()
        }
    }

    // Form inputs state
    var selectedScheduleItem by remember { mutableStateOf<ScheduleItem?>(null) }
    var requestType by remember { mutableStateOf("TEMPORARY") } // "TEMPORARY" or "PERMANENT"
    var targetDate by remember { mutableStateOf("") }
    var effectiveFromDate by remember { mutableStateOf("") }
    var proposedDay by remember { mutableStateOf("") }
    var proposedStartTime by remember { mutableStateOf("") }
    var proposedEndTime by remember { mutableStateOf("") }
    var selectedRoom by remember { mutableStateOf<Room?>(null) }
    var reason by remember { mutableStateOf("") }

    // Filter states for Step 1
    var selectedClassFilter by remember { mutableStateOf("") }
    var selectedSemesterFilter by remember { mutableStateOf("") }
    var classExpanded by remember { mutableStateOf(false) }
    var semesterExpanded by remember { mutableStateOf(false) }
    var scheduleExpanded by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var dayExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    // Date pickers calendar instances
    val calendar = Calendar.getInstance()
    val today = calendar.time
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")) }

    // Dropdown options
    val dayOptions = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT")
    val timeSlots = listOf(
        "07:30" to "10:00",
        "10:15" to "12:45",
        "13:15" to "15:45",
        "16:00" to "17:40"
    )
    var proposedTimeSlotStr by remember { mutableStateOf("") }
    var selectedRequestForDetail by remember { mutableStateOf<ValidationRequest?>(null) }

    // Handle submit results
    LaunchedEffect(submitSuccess) {
        if (submitSuccess == true) {
            Toast.makeText(context, "Pengajuan berhasil dikirim!", Toast.LENGTH_SHORT).show()
            dashboardViewModel.resetSubmitStatus()
            // Clear and go to start step
            currentStep = 1
            selectedScheduleItem = null
            selectedClassFilter = ""
            selectedSemesterFilter = ""
            requestType = "TEMPORARY"
            targetDate = ""
            effectiveFromDate = ""
            proposedDay = ""
            proposedStartTime = ""
            proposedEndTime = ""
            proposedTimeSlotStr = ""
            selectedRoom = null
            reason = ""
        } else if (submitSuccess == false) {
            Toast.makeText(context, "Gagal mengirim pengajuan. Silakan coba lagi.", Toast.LENGTH_LONG).show()
            dashboardViewModel.resetSubmitStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Form Pengajuan Jadwal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // STEP PROGRESS INDICATOR
            StepProgressHeader(currentStep = currentStep, totalSteps = 6)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentStep) {
                            1 -> "Langkah 1: Pilih Mata Kuliah"
                            2 -> "Langkah 2: Tipe Perubahan"
                            3 -> if (requestType == "TEMPORARY") "Langkah 3: Pilih Pertemuan" else "Langkah 3: Tanggal Mulai Efektif"
                            4 -> "Langkah 4: Tuliskan Alasan"
                            5 -> "Langkah 5: Cari Jadwal Pengganti"
                            else -> "Langkah 6: Review & Kirim"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    DashboardCard {
                        when (currentStep) {
                            1 -> {
                                val classOptions = schedules.mapNotNull { it.course?.className }.distinct().sorted()
                                val semesterOptions = schedules.filter { 
                                    selectedClassFilter.isEmpty() || it.course?.className == selectedClassFilter
                                }.mapNotNull { it.course?.description ?: it.semester?.name ?: "Semester ${it.semesterId}" }.distinct().sorted()
                                
                                val filteredSchedules = schedules.filter { schedule ->
                                    (selectedClassFilter.isEmpty() || schedule.course?.className == selectedClassFilter) &&
                                    (selectedSemesterFilter.isEmpty() || (schedule.course?.description ?: schedule.semester?.name ?: "Semester ${schedule.semesterId}") == selectedSemesterFilter)
                                }

                                // 1. Pilih Kelas
                                Text("Kelas *", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { classExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            selectedClassFilter.ifEmpty { "Pilih Kelas" },
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = classExpanded,
                                        onDismissRequest = { classExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Semua Kelas") },
                                            onClick = {
                                                selectedClassFilter = ""
                                                selectedSemesterFilter = ""
                                                selectedScheduleItem = null
                                                classExpanded = false
                                            }
                                        )
                                        classOptions.forEach { className ->
                                            DropdownMenuItem(
                                                text = { Text("Kelas $className") },
                                                onClick = {
                                                    selectedClassFilter = className
                                                    selectedSemesterFilter = ""
                                                    selectedScheduleItem = null
                                                    classExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 2. Pilih Semester
                                val isSemesterEnabled = selectedClassFilter.isNotEmpty()
                                Text(
                                    "Semester *",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSemesterEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { semesterExpanded = true },
                                        enabled = isSemesterEnabled,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            selectedSemesterFilter.ifEmpty { "Pilih Semester" },
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = semesterExpanded,
                                        onDismissRequest = { semesterExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Semua Semester") },
                                            onClick = {
                                                selectedSemesterFilter = ""
                                                selectedScheduleItem = null
                                                semesterExpanded = false
                                            }
                                        )
                                        semesterOptions.forEach { semName ->
                                            DropdownMenuItem(
                                                text = { Text(semName) },
                                                onClick = {
                                                    selectedSemesterFilter = semName
                                                    selectedScheduleItem = null
                                                    semesterExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 3. Pilih Mata Kuliah
                                val isMatkulEnabled = selectedClassFilter.isNotEmpty() && selectedSemesterFilter.isNotEmpty()
                                Text(
                                    "Mata Kuliah *",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isMatkulEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { scheduleExpanded = true },
                                        enabled = isMatkulEnabled,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            selectedScheduleItem?.let { "${it.course?.name} (${it.course?.className})" } ?: "Pilih Mata Kuliah Anda",
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = scheduleExpanded,
                                        onDismissRequest = { scheduleExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        if (filteredSchedules.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Tidak ada mata kuliah cocok") },
                                                onClick = { scheduleExpanded = false }
                                            )
                                        } else {
                                            filteredSchedules.forEach { schedule ->
                                                DropdownMenuItem(
                                                    text = { Text("${schedule.course?.name} (${schedule.course?.className})") },
                                                    onClick = {
                                                        selectedScheduleItem = schedule
                                                        scheduleExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                Text("Pilih Tipe Perubahan Jadwal *", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        onClick = { requestType = "TEMPORARY" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (requestType == "TEMPORARY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = requestType == "TEMPORARY",
                                                onClick = { requestType = "TEMPORARY" }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Sementara", fontWeight = FontWeight.Bold)
                                                Text("Hanya merubah jadwal untuk 1 kali pertemuan saja", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Surface(
                                        onClick = { requestType = "PERMANENT" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (requestType == "PERMANENT") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = requestType == "PERMANENT",
                                                onClick = { requestType = "PERMANENT" }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Permanen", fontWeight = FontWeight.Bold)
                                                Text("Merubah jadwal kuliah seterusnya mulai tanggal efektif", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                if (requestType == "TEMPORARY") {
                                    val dayName = selectedScheduleItem?.day ?: "SENIN"
                                    val upcomingDates: List<Date> = getNextDatesForDayOfWeek(dayName, 6)

                                    Text("Pilih Pertemuan yang Ingin Diganti *", style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        upcomingDates.forEach { date ->
                                            val dateVal = dateFormatter.format(date)
                                            val isSelected = targetDate == dateVal
                                            Surface(
                                                onClick = { targetDate = dateVal },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(displayDateFormatter.format(date), fontWeight = FontWeight.Bold)
                                                        Text("${selectedScheduleItem?.course?.name} (${dayName.lowercase().replaceFirstChar { it.uppercase() }})", style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    if (isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val dayName = selectedScheduleItem?.day ?: "SENIN"
                                    val timeInfo = "${selectedScheduleItem?.startTime ?: "??:??"} - ${selectedScheduleItem?.endTime ?: "??:??"}"
                                    
                                    Text("Jadwal akan diganti mulai dari:", style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Hari: $dayName | Sesi: $timeInfo",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Pilih Minggu Mulai Efektif *", style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val upcomingDates: List<Date> = getNextDatesForDayOfWeek(dayName, 16)
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        upcomingDates.forEachIndexed { index, date ->
                                            val dateVal = dateFormatter.format(date)
                                            val isSelected = effectiveFromDate == dateVal
                                            val isPast = date.time < today.time
                                            
                                            if (!isPast) {
                                                Surface(
                                                    onClick = { effectiveFromDate = dateVal },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("Minggu ${index + 1}", fontWeight = FontWeight.Bold)
                                                            Text(displayDateFormatter.format(date), style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        if (isSelected) {
                                                            Icon(Icons.Default.Check, contentDescription = null)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                Text("Tuliskan Alasan Pengajuan Perubahan * (Min 20 Karakter)", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    placeholder = { Text("Tuliskan alasan perubahan jadwal secara detail...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Karakter saat ini: ${reason.length} / 20",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (reason.length >= 20) Success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            5 -> {
                                Text("Cari Jadwal Pengganti untuk Pertemuan ${targetDate.ifEmpty { "Pilih di Step 3" }}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text("Pilih Hari Pengganti *", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { dayExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            proposedDay.ifEmpty { "Pilih Hari Pengganti" },
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = dayExpanded,
                                        onDismissRequest = { dayExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        dayOptions.forEach { d ->
                                            DropdownMenuItem(
                                                text = { Text(d) },
                                                onClick = {
                                                    proposedDay = d
                                                    dayExpanded = false
                                                    // Clear manual options since we changed the day
                                                    proposedStartTime = ""
                                                    proposedEndTime = ""
                                                    proposedTimeSlotStr = ""
                                                    selectedRoom = null
                                                }
                                            )
                                        }
                                    }
                                }

                                if (proposedDay.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Rekomendasi Jadwal Kosong (Tersedia)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Dynamic available slots - exclude current schedule being replaced
                                    val availableSlots = remember(proposedDay, schedules, rooms, selectedScheduleItem) {
                                        val list = mutableListOf<Triple<Room, String, String>>()
                                        for (room in rooms) {
                                            for (slot in timeSlots) {
                                                // Check if this slot is occupied by any OTHER schedule (not the one being replaced)
                                                val isOccupied = schedules.any { sched ->
                                                    sched.id != selectedScheduleItem?.id && // EXCLUDE current schedule being replaced
                                                    sched.roomId == room.id &&
                                                    sched.day.equals(proposedDay, ignoreCase = true) &&
                                                    sched.startTime == slot.first
                                                }
                                                if (!isOccupied) {
                                                    list.add(Triple(room, slot.first, slot.second))
                                                }
                                            }
                                        }
                                        list.take(8)
                                    }

                                    if (availableSlots.isEmpty()) {
                                        Text("Tidak ada kelas/slot yang kosong di hari $proposedDay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            availableSlots.forEach { slot ->
                                                val slotRoom = slot.first
                                                val startT = slot.second
                                                val endT = slot.third
                                                val isSelected = selectedRoom?.id == slotRoom.id && proposedStartTime == startT

                                                Surface(
                                                    onClick = {
                                                        selectedRoom = slotRoom
                                                        proposedStartTime = startT
                                                        proposedEndTime = endT
                                                        proposedTimeSlotStr = "$startT - $endT"
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("${slotRoom.name} (${slotRoom.code})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text("Sesi: $startT - $endT", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        if (isSelected) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Pilihan Alternatif / Manual", style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Manual slot picker
                                    Text("Pilih Sesi Waktu", style = MaterialTheme.typography.bodySmall)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        timeSlots.forEach { slot ->
                                            val slotStr = "${slot.first} - ${slot.second}"
                                            FilterChip(
                                                selected = proposedTimeSlotStr == slotStr,
                                                onClick = {
                                                    if (proposedTimeSlotStr == slotStr) {
                                                        proposedTimeSlotStr = ""
                                                        proposedStartTime = ""
                                                        proposedEndTime = ""
                                                    } else {
                                                        proposedTimeSlotStr = slotStr
                                                        proposedStartTime = slot.first
                                                        proposedEndTime = slot.second
                                                    }
                                                },
                                                label = { Text(slotStr, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("Pilih Ruangan", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { roomExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                selectedRoom?.name ?: "Pilih Ruangan Baru",
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = roomExpanded,
                                            onDismissRequest = { roomExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            rooms.forEach { room ->
                                                DropdownMenuItem(
                                                    text = { Text("${room.name} (${room.code})") },
                                                    onClick = {
                                                        selectedRoom = room
                                                        roomExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                Text("Review & Konfirmasi Pengajuan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ReviewField(label = "Mata Kuliah", value = selectedScheduleItem?.let { "${it.course?.name} (${it.course?.className})" } ?: "-")
                                    ReviewField(label = "Tipe Perubahan", value = if (requestType == "TEMPORARY") "Sementara" else "Permanen")
                                    ReviewField(
                                        label = if (requestType == "TEMPORARY") "Tanggal Pertemuan" else "Tanggal Efektif",
                                        value = if (requestType == "TEMPORARY") targetDate else effectiveFromDate
                                    )
                                    ReviewField(label = "Alasan", value = reason)
                                    ReviewField(label = "Hari Usulan", value = proposedDay)
                                    ReviewField(label = "Sesi Waktu", value = proposedTimeSlotStr)
                                    ReviewField(label = "Ruangan Usulan", value = selectedRoom?.let { "${it.name} (${it.code})" } ?: "-")
                                }
                            }
                        }
                    }
                }

                // BOTTOM NAVIGATION BUTTONS FOR WIZARD - FULL WIDTH AT BOTTOM
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                    ) {
                        if (currentStep < 6) {
                            // Two buttons layout for "Kembali" and "Lanjut"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (currentStep > 1) {
                                    SecondaryButton(
                                        text = "← Kembali",
                                        onClick = { currentStep-- },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                PrimaryButton(
                                    text = "Lanjut →",
                                    onClick = {
                                        // Validate step
                                        when (currentStep) {
                                            1 -> {
                                                if (selectedScheduleItem == null) {
                                                    Toast.makeText(context, "Silakan pilih mata kuliah terlebih dahulu", Toast.LENGTH_SHORT).show()
                                                    return@PrimaryButton
                                                }
                                            }
                                            2 -> {
                                                // Always selected by default
                                            }
                                            3 -> {
                                                if (requestType == "TEMPORARY" && targetDate.isEmpty()) {
                                                    Toast.makeText(context, "Silakan pilih tanggal pertemuan terlebih dahulu", Toast.LENGTH_SHORT).show()
                                                    return@PrimaryButton
                                                }
                                                if (requestType == "PERMANENT" && effectiveFromDate.isEmpty()) {
                                                    Toast.makeText(context, "Silakan pilih tanggal efektif terlebih dahulu", Toast.LENGTH_SHORT).show()
                                                    return@PrimaryButton
                                                }
                                            }
                                            4 -> {
                                                if (reason.length < 20) {
                                                    Toast.makeText(context, "Alasan harus minimal 20 karakter", Toast.LENGTH_SHORT).show()
                                                    return@PrimaryButton
                                                }
                                            }
                                            5 -> {
                                                if (proposedDay.isEmpty() || proposedStartTime.isEmpty() || proposedEndTime.isEmpty() || selectedRoom == null) {
                                                    Toast.makeText(context, "Silakan pilih usulan jadwal pengganti (hari, sesi, dan ruangan)", Toast.LENGTH_SHORT).show()
                                                    return@PrimaryButton
                                                }
                                            }
                                        }
                                        currentStep++
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Step 6: Review only - buttons moved below
                            Text("Review & Konfirmasi Pengajuan", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ReviewField(label = "Mata Kuliah", value = selectedScheduleItem?.let { "${it.course?.name} (${it.course?.className})" } ?: "-")
                                ReviewField(label = "Tipe Perubahan", value = if (requestType == "TEMPORARY") "Sementara" else "Permanen")
                                ReviewField(
                                    label = if (requestType == "TEMPORARY") "Tanggal Pertemuan" else "Tanggal Efektif",
                                    value = if (requestType == "TEMPORARY") targetDate else effectiveFromDate
                                )
                                ReviewField(label = "Alasan", value = reason)
                                ReviewField(label = "Hari Usulan", value = proposedDay)
                                ReviewField(label = "Sesi Waktu", value = proposedTimeSlotStr)
                                ReviewField(label = "Ruangan Usulan", value = selectedRoom?.let { "${it.name} (${it.code})" } ?: "-")
                            }
                        }
                    }
                }

                // Section 2: Request History
                item {
                    Text(
                        "Riwayat Pengajuan Anda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (requests.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Belum ada riwayat pengajuan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(requests) { request ->
                        HistoryItemCard(request, onClick = { selectedRequestForDetail = request })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Detail Request Modal Dialog
    if (selectedRequestForDetail != null) {
        val request = selectedRequestForDetail!!
        val statusColor = when (request.status) {
            RequestStatus.PENDING -> Warning
            RequestStatus.FORWARDED -> com.informatika.sars.ui.theme.Info
            RequestStatus.APPROVED -> Success
            RequestStatus.REJECTED -> Error
        }
        val statusText = when (request.status) {
            RequestStatus.PENDING -> "Menunggu"
            RequestStatus.FORWARDED -> "Diteruskan"
            RequestStatus.APPROVED -> "Disetujui"
            RequestStatus.REJECTED -> "Ditolak"
        }

        Dialog(
            onDismissRequest = { selectedRequestForDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detail Pengajuan",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        IconButton(onClick = { selectedRequestForDetail = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    
                    // Content
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = request.requestCode ?: "REQ-UNKNOWN",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        HorizontalDivider()
                        
                        Column {
                            Text("Mata Kuliah", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(request.subject ?: "-", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        
                        Column {
                            Text("Tipe Pengajuan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(request.requestType ?: "-", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        if (request.targetDate != null) {
                            Column {
                                Text("Tanggal Pertemuan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(request.targetDate ?: "-", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        if (request.effectiveFromDate != null) {
                            Column {
                                Text("Tanggal Efektif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(request.effectiveFromDate ?: "-", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (request.proposedDay != null || request.proposedStartTime != null || request.proposedRoomName != null) {
                            Column {
                                Text("Usulan Pengganti", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    buildString {
                                        if (request.proposedRoomName != null) append("Ruangan: ${request.proposedRoomName}\n")
                                        if (request.proposedDay != null) append("Hari: ${request.proposedDay}\n")
                                        if (request.proposedStartTime != null) append("Waktu: ${request.proposedStartTime} - ${request.proposedEndTime}")
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Column {
                            Text("Alasan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(request.reason ?: "-", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    
                    // Button full width
                    Button(
                        onClick = { selectedRequestForDetail = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            "Tutup",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(request: ValidationRequest, onClick: () -> Unit) {
    val statusColor = when (request.status) {
        RequestStatus.PENDING -> Warning
        RequestStatus.FORWARDED -> com.informatika.sars.ui.theme.Info
        RequestStatus.APPROVED -> Success
        RequestStatus.REJECTED -> Error
    }
    val statusText = when (request.status) {
        RequestStatus.PENDING -> "Menunggu"
        RequestStatus.FORWARDED -> "Diteruskan"
        RequestStatus.APPROVED -> "Disetujui"
        RequestStatus.REJECTED -> "Ditolak"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.subject ?: "Mata Kuliah",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Req: ${request.requestCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(6.dp, 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StepProgressHeader(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isCompleted = i < currentStep
            val isActive = i == currentStep
            val circleColor = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val textColor = when {
                isCompleted -> MaterialTheme.colorScheme.onPrimary
                isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(circleColor)
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = i.toString(),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (i < totalSteps) {
                HorizontalDivider(
                    color = if (i < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReviewField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

fun getNextDatesForDayOfWeek(dayOfWeekStr: String, count: Int): List<Date> {
    val targetDay = when (dayOfWeekStr.uppercase()) {
        "SENIN", "MONDAY" -> Calendar.MONDAY
        "SELASA", "TUESDAY" -> Calendar.TUESDAY
        "RABU", "WEDNESDAY" -> Calendar.WEDNESDAY
        "KAMIS", "THURSDAY" -> Calendar.THURSDAY
        "JUMAT", "FRIDAY" -> Calendar.FRIDAY
        "SABTU", "SATURDAY" -> Calendar.SATURDAY
        "MINGGU", "SUNDAY" -> Calendar.SUNDAY
        else -> return emptyList()
    }
    val list = mutableListOf<Date>()
    val cal = Calendar.getInstance()
    var found = 0
    for (i in 0 until 90) {
        if (cal.get(Calendar.DAY_OF_WEEK) == targetDay) {
            list.add(cal.time)
            found++
            if (found == count) break
        }
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}