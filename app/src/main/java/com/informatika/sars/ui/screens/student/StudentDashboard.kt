package com.informatika.sars.ui.screens.student

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.informatika.sars.ui.components.DashboardCard
import com.informatika.sars.ui.components.ScheduleCard
import com.informatika.sars.ui.components.SectionHeader
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.informatika.sars.ui.theme.*
import com.informatika.sars.viewmodel.*
import com.informatika.sars.data.model.ScheduleItem
import com.informatika.sars.data.model.ValidationRequest
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import com.informatika.sars.data.model.RequestStatus
import androidx.compose.material3.pulltorefresh.*
import kotlin.math.roundToInt
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    themeViewModel: ThemeViewModel,
    chatViewModel: ChatViewModel,
    dashboardViewModel: com.informatika.sars.viewmodel.DashboardViewModel,
    initialTab: Int = 0,
    onRequestNew: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val schedules by dashboardViewModel.schedules.collectAsState()
    val semesters by dashboardViewModel.semesters.collectAsState()
    val rooms by dashboardViewModel.rooms.collectAsState()
    val requests by dashboardViewModel.requests.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    val errorLog by dashboardViewModel.errorLog.collectAsState()
    
    val notifications by notificationViewModel.notifications.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()
    val name = currentUser?.name?.split(" ")?.firstOrNull() ?: "User"

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    
    // Set initial tab from parameter
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    
    var showNotificationDialog by remember { mutableStateOf(false) }


    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Fetch data and start realtime listener on start
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
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                        Spacer(modifier = Modifier.width(12.dp))
                        val title = when(selectedTab) {
                            0 -> "Sistem Penjadwalan"
                            1 -> "Jadwal Kuliah"
                            2 -> "Request Jadwal"
                            3 -> "AI Assistant"
                            else -> "Pengaturan"
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { showNotificationDialog = true }) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge { Text(notifications.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifikasi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Custom Navigation Bar Background
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selectedTab == 1) 0.dp else 8.dp,
                    shadowElevation = if (selectedTab == 1) 0.dp else 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        CustomNavItem(
                            icon = Icons.Default.Home,
                            label = "Home",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        CustomNavItem(
                            icon = Icons.Default.DateRange,
                            label = "Jadwal",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Spacer for the elevated center button
                        Spacer(modifier = Modifier.weight(1f))

                        CustomNavItem(
                            icon = Icons.AutoMirrored.Filled.ListAlt,
                            label = "Request",
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        CustomNavItem(
                            icon = Icons.Default.Settings,
                            label = "Setting",
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Elevated AI Assistant Button
                FloatingActionButton(
                    onClick = { selectedTab = 3 },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-28).dp) // Bulge effect
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, 
                        contentDescription = "AI Assistant",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButton = { },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (selectedTab) {
            0 -> DashboardContent(
                padding = padding,
                name = name,
                currentUser = currentUser,
                notificationViewModel = notificationViewModel,
                schedules = schedules,
                requests = requests,
                isLoading = isLoading,
                onRefresh = { dashboardViewModel.fetchData(currentUser) },
                onViewAllJadwal = { selectedTab = 1 },
                onViewAllRequest = { selectedTab = 2 },
                onRequestNew = onRequestNew
            )
            1 -> ScheduleContent(
                padding = padding,
                schedules = schedules,
                rooms = rooms,
                isLoading = isLoading,
                onRefresh = { dashboardViewModel.fetchData(currentUser) }
            )
            2 -> RequestContent(
                padding = padding,
                requests = requests,
                isLoading = isLoading,
                onRefresh = { dashboardViewModel.fetchData(currentUser) },
                onRequestNew = onRequestNew
            )
            3 -> AIAssistantContent(padding, chatViewModel)
            4 -> SettingsContent(padding, authViewModel, themeViewModel)
        }
    }



    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Notifikasi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tidak ada notifikasi baru",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notifications.reversed()) { notification ->
                                NotificationItem(notification)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    TextButton(
                        onClick = { showNotificationDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Tutup",
                            fontWeight = FontWeight.SemiBold,
                            color = Success
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: String) {
    val parts = notification.split(": ", limit = 2)
    val title = parts.getOrNull(0) ?: "Notifikasi"
    val message = parts.getOrNull(1) ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(6.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFormDialog(
    currentUser: com.informatika.sars.data.model.User?,
    rooms: List<com.informatika.sars.data.model.Room> = emptyList(),
    schedules: List<ScheduleItem> = emptyList(),
    semesters: List<com.informatika.sars.data.model.Semester> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String?, String?, String?, String?, String?) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    
    // Step 1: Pilih Mata Kuliah
    var selectedSemester by remember { mutableStateOf("") }
    var selectedKelas by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    
    // Step 2: Tipe Request
    var requestType by remember { mutableStateOf("") } // "SEMENTARA" or "PERMANEN"
    
    // Step 3: Pilih Pertemuan
    var selectedPertemuan by remember { mutableStateOf("") }
    
    // Step 4: Alasan
    var reason by remember { mutableStateOf("") }
    
    // Step 5: Jadwal Pengganti
    var selectedRoom by remember { mutableStateOf("") }
    var selectedTimeSlot by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("") }

    val semesterNames = semesters.mapNotNull { it.name }
    val classes = listOf("A", "B", "C", "D", "A P", "B P", "C P", "D P")
    val subjects = schedules.filter { 
        (selectedSemester.isEmpty() || it.semester?.name == selectedSemester || it.semesterId?.toString() == selectedSemester) &&
        (selectedKelas.isEmpty() || it.course?.className == selectedKelas)
    }.mapNotNull { it.course?.name }.filterNotNull().distinct()
    
    val timeSlots = listOf("Sesi 1-3 (07:30 - 10:00)", "Sesi 4-6 (10:15 - 12:45)", "Sesi 7-9 (13:15 - 15:45)", "Sesi 10-11 (16:00 - 17:40)")
    val days = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header with Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Requests",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
                
                // Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val currentStep = index + 1
                        val isCompleted = currentStep < step
                        val isActive = currentStep == step
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isActive || isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text(
                                    currentStep.toString(),
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        if (index < 5) {
                            Box(modifier = Modifier.weight(1f).height(2.dp).background(if (currentStep < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when (step) {
                        1 -> StepPilihMK(
                            semesterNames, selectedSemester, { selectedSemester = it },
                            classes, selectedKelas, { selectedKelas = it },
                            subjects, selectedSubject, { selectedSubject = it }
                        )
                        2 -> StepTipeRequest(requestType) { requestType = it }
                        3 -> StepPilihPertemuan(selectedPertemuan) { selectedPertemuan = it }
                        4 -> StepAlasan(reason) { reason = it }
                        5 -> StepJadwalPengganti(
                            days, selectedDay, { selectedDay = it },
                            rooms, selectedRoom, { selectedRoom = it },
                            timeSlots, selectedTimeSlot, { selectedTimeSlot = it }
                        )
                        6 -> StepReview(
                            selectedSubject, requestType, selectedPertemuan, reason, 
                            "$selectedDay, $selectedRoom, $selectedTimeSlot"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Kembali")
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (step < 6) {
                                step++
                            } else {
                                onSubmit(
                                    selectedSubject, 
                                    selectedRoom, 
                                    selectedTimeSlot, 
                                    reason,
                                    selectedSemester,
                                    selectedKelas,
                                    requestType,
                                    selectedPertemuan,
                                    "$selectedDay | $selectedTimeSlot"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = when(step) {
                            1 -> selectedSemester.isNotBlank() && selectedKelas.isNotBlank() && selectedSubject.isNotBlank()
                            2 -> requestType.isNotBlank()
                            3 -> if (requestType == "SEMENTARA") selectedPertemuan.isNotBlank() else true
                            4 -> reason.isNotBlank()
                            5 -> selectedDay.isNotBlank() && selectedRoom.isNotBlank() && selectedTimeSlot.isNotBlank()
                            else -> true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (step == 6) "Kirim & Review" else "Lanjut →")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepPilihMK(
    semesterNames: List<String>, selectedSemester: String, onSemesterSelect: (String) -> Unit,
    classes: List<String>, selectedKelas: String, onKelasSelect: (String) -> Unit,
    subjects: List<String>, selectedSubject: String, onSubjectSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pilih Mata Kuliah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Pilih jadwal mata kuliah yang ingin diajukan perubahan.", style = MaterialTheme.typography.bodySmall)
        
        var expSem by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expSem, onExpandedChange = { expSem = it }) {
            OutlinedTextField(
                value = if (selectedSemester.isEmpty()) "Pilih Semester..." else selectedSemester,
                onValueChange = {}, readOnly = true, label = { Text("SEMESTER *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expSem) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expSem, onDismissRequest = { expSem = false }) {
                semesterNames.forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { onSemesterSelect(s); expSem = false })
                }
            }
        }

        var expKel by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expKel, onExpandedChange = { expKel = it }) {
            OutlinedTextField(
                value = if (selectedKelas.isEmpty()) "Pilih Kelas..." else "Kelas $selectedKelas",
                onValueChange = {}, readOnly = true, label = { Text("KELAS *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expKel) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expKel, onDismissRequest = { expKel = false }) {
                classes.forEach { c ->
                    DropdownMenuItem(text = { Text("Kelas $c") }, onClick = { onKelasSelect(c); expKel = false })
                }
            }
        }

        var expSub by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expSub, onExpandedChange = { expSub = it }) {
            OutlinedTextField(
                value = if (selectedSubject.isEmpty()) "Pilih Mata Kuliah..." else selectedSubject,
                onValueChange = {}, readOnly = true, label = { Text("MATA KULIAH *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expSub) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expSub, onDismissRequest = { expSub = false }) {
                if (subjects.isEmpty()) {
                    DropdownMenuItem(text = { Text("Tidak ada jadwal ditemukan") }, onClick = {})
                } else {
                    subjects.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { onSubjectSelect(s); expSub = false })
                    }
                }
            }
        }
    }
}

@Composable
fun StepTipeRequest(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tipe Request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Tentukan apakah perubahan jadwal ini bersifat sementara atau permanen.", style = MaterialTheme.typography.bodySmall)
        
        RequestTypeCard(
            title = "Perubahan Satu Pertemuan",
            desc = "Jadwal kuliah hanya bergeser untuk tanggal pertemuan yang dipilih.",
            type = "1X SEMENTARA",
            selected = selected == "SEMENTARA",
            onClick = { onSelect("SEMENTARA") }
        )
        
        RequestTypeCard(
            title = "Perubahan Sisa Semester",
            desc = "Jadwal kuliah akan berubah secara permanen setiap minggunya untuk sisa semester aktif.",
            type = "PERMANEN",
            selected = selected == "PERMANEN",
            onClick = { onSelect("PERMANEN") }
        )
    }
}

@Composable
fun RequestTypeCard(title: String, desc: String, type: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(type, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
fun StepPilihPertemuan(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pilih Pertemuan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Pilih pertemuan ke berapa yang ingin diajukan perubahan.", style = MaterialTheme.typography.bodySmall)
        
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..16).forEach { p ->
                FilterChip(
                    selected = selected == "Pertemuan $p",
                    onClick = { onSelect("Pertemuan $p") },
                    label = { Text("Pertemuan $p") }
                )
            }
        }
    }
}

@Composable
fun StepAlasan(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Alasan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text("Tuliskan alasan perubahan jadwal...") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepJadwalPengganti(
    days: List<String>, selectedDay: String, onDaySelect: (String) -> Unit,
    rooms: List<com.informatika.sars.data.model.Room>, selectedRoom: String, onRoomSelect: (String) -> Unit,
    timeSlots: List<String>, selectedTimeSlot: String, onTimeSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Jadwal Pengganti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        var expDay by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expDay, onExpandedChange = { expDay = it }) {
            OutlinedTextField(
                value = selectedDay.ifEmpty { "Pilih Hari..." },
                onValueChange = {}, readOnly = true, label = { Text("HARI PENGGANTI *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expDay) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expDay, onDismissRequest = { expDay = false }) {
                days.forEach { d ->
                    DropdownMenuItem(text = { Text(d) }, onClick = { onDaySelect(d); expDay = false })
                }
            }
        }

        var expTime by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expTime, onExpandedChange = { expTime = it }) {
            OutlinedTextField(
                value = selectedTimeSlot.ifEmpty { "Pilih Sesi..." },
                onValueChange = {}, readOnly = true, label = { Text("SESI / WAKTU *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expTime) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expTime, onDismissRequest = { expTime = false }) {
                timeSlots.forEach { t ->
                    DropdownMenuItem(text = { Text(t) }, onClick = { onTimeSelect(t); expTime = false })
                }
            }
        }

        var expRoom by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expRoom, onExpandedChange = { expRoom = it }) {
            OutlinedTextField(
                value = selectedRoom.ifEmpty { "Pilih Ruangan..." },
                onValueChange = {}, readOnly = true, label = { Text("RUANGAN *") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expRoom) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expRoom, onDismissRequest = { expRoom = false }) {
                rooms.forEach { r ->
                    DropdownMenuItem(text = { Text(r.name ?: "") }, onClick = { onRoomSelect(r.name ?: ""); expRoom = false })
                }
            }
        }
    }
}

@Composable
fun StepReview(subject: String, type: String, meeting: String, reason: String, schedule: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Review & Kirim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewItem("Mata Kuliah", subject)
                ReviewItem("Tipe Request", type)
                if (type == "SEMENTARA") ReviewItem("Pertemuan", meeting)
                ReviewItem("Alasan", reason)
                ReviewItem("Jadwal Baru", schedule)
            }
        }
        Text("Dengan menekan tombol kirim, Anda menyetujui perubahan jadwal ini untuk diproses oleh Aslab.", 
             style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReviewItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    padding: PaddingValues, 
    name: String, 
    currentUser: com.informatika.sars.data.model.User?,
    notificationViewModel: NotificationViewModel,
    schedules: List<ScheduleItem>,
    requests: List<ValidationRequest>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onViewAllJadwal: () -> Unit,
    onViewAllRequest: () -> Unit,
    onRequestNew: () -> Unit,
    onRequestClick: (ValidationRequest) -> Unit = {}
) {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val todayIndo = when (dayOfWeek) {
        Calendar.MONDAY -> "SENIN"
        Calendar.TUESDAY -> "SELASA"
        Calendar.WEDNESDAY -> "RABU"
        Calendar.THURSDAY -> "KAMIS"
        Calendar.FRIDAY -> "JUMAT"
        Calendar.SATURDAY -> "SABTU"
        Calendar.SUNDAY -> "MINGGU"
        else -> ""
    }

    // Dynamic Time and Date
    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH.mm.ss", Locale.getDefault()).format(Date())) }
    val currentDate = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date()) }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = SimpleDateFormat("HH.mm.ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.padding(padding)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Koneksi DB:", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = Success.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(Success, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AKTIF", color = Success, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text("Database: SARS_DB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Real-time Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("$todayIndo, $currentDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Hello, $name.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "JADWAL PERKULIAHAN • MINGGU 11",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SectionHeader(title = "Jadwal Hari Ini", actionText = "LIHAT SEMUA", onActionClick = onViewAllJadwal)
                val todaySchedules = schedules.filter { it.day.trim().uppercase() == todayIndo.uppercase() }
                if (todaySchedules.isEmpty()) {
                    Text(
                        "Tidak ada jadwal untuk hari ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    todaySchedules.take(2).forEach { item ->
                        ScheduleCard(
                            title = item.course?.name ?: "No Title",
                            lecturer = item.lecturerName,
                            room = item.room?.name ?: "No Room",
                            time = "${item.startTime} - ${item.endTime}",
                            semester = item.course?.description ?: item.semester?.name ?: "Semester ${item.semesterId ?: "-"}",
                            status = item.status,
                            color = if (item.status == "LIVE") Success else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                SectionHeader(title = "Request Terbaru", actionText = "LIHAT SEMUA", onActionClick = onViewAllRequest)
                DashboardCard {
                    if (requests.isEmpty()) {
                        Text(
                            "Belum ada request pengajuan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column {
                            requests.take(2).forEachIndexed { index, request ->
                                RequestItem(
                                    request = request,
                                    onClick = { onRequestClick(request) }
                                )
                                if (index < requests.take(2).size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                SectionHeader(title = "Pengajuan Pinjam Ruangan")
                DashboardCard {
                    if (requests.isEmpty()) {
                        Text(
                            "Belum ada data pengajuan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            requests.take(3).forEach { request ->
                                CompactRequestStatusItem(request, onClick = { onRequestClick(request) })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CompactRequestStatusItem(request: ValidationRequest, onClick: () -> Unit = {}) {
    val (statusText, statusColor) = when (request.status) {
        RequestStatus.PENDING -> "Menunggu" to Warning
        RequestStatus.FORWARDED -> "Diteruskan" to PrimaryBlue
        RequestStatus.APPROVED -> "Disetujui" to Success
        RequestStatus.REJECTED -> "Ditolak" to Color.Red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.subject ?: "-",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = request.room ?: "-",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RequestItem(request: ValidationRequest, onClick: () -> Unit = {}) {
    val (statusText, statusColor) = when (request.status) {
        RequestStatus.PENDING -> "Reviewing" to Warning
        RequestStatus.FORWARDED -> "Forwarded" to PrimaryBlue
        RequestStatus.APPROVED -> "Published" to Success
        RequestStatus.REJECTED -> "Rejected" to Color.Red
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = request.subject ?: "-",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Request Info", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Surface(
            color = statusColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AIAssistantContent(padding: PaddingValues, chatViewModel: ChatViewModel) {
    val messages by chatViewModel.messages.collectAsState()
    val isTyping by chatViewModel.isTyping.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 60.dp) // Ditingkatkan sedikit lagi ke 60.dp agar lebih aman
    ) {
        // Chat History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isTyping) {
                item {
                    Text(
                        "AI sedang mengetik...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tanyakan sesuatu...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: com.informatika.sars.viewmodel.ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = shape,
            tonalElevation = 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleContent(
    padding: PaddingValues,
    schedules: List<ScheduleItem>,
    rooms: List<com.informatika.sars.data.model.Room>,
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
    var selectedScheduleForDetail by remember { mutableStateOf<ScheduleItem?>(null) }
    
    // Zoom state
    var scale by remember { mutableFloatStateOf(1.0f) }

    // DEBUG LOG
    LaunchedEffect(schedules, selectedDay) {
        android.util.Log.d("ScheduleContent", "Total schedules: ${schedules.size}")
        android.util.Log.d("ScheduleContent", "Selected Day: '$selectedDay'")
        if (schedules.isNotEmpty()) {
            android.util.Log.d("ScheduleContent", "First schedule day: '${schedules[0].day}'")
        }
    }

    val schedulesByRoom = remember(schedules, selectedDay) {
        schedules.filter { 
            it.day.trim().equals(selectedDay.trim(), ignoreCase = true)
        }.groupBy { it.room?.name ?: "" }
    }
    
    val sessions = remember { (1..11).toList() }
    val cellWidth = 140.dp
    val cellHeight = 100.dp
    val roomWidth = 80.dp
    val headerHeight = 40.dp

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val borderColor = if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE)
    val headerBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
    ) {
        // Header Judul sesuai gambar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Jadwal & Ketersediaan Ruangan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Tab Hari & Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecondaryScrollableTabRow(
                    selectedTabIndex = days.indexOf(selectedDay),
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(days.indexOf(selectedDay)),
                            color = Indigo600
                        )
                    }
                ) {
                    days.forEach { day ->
                        Tab(
                            selected = selectedDay == day,
                            onClick = { 
                                selectedDay = day 
                            },
                            text = {
                                Text(
                                    text = day.lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            selectedContentColor = Indigo600,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Mini Filters/Action
            Row(modifier = Modifier.padding(end = 16.dp)) {
                IconButton(onClick = { /* Filter Logic */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { /* Download Logic */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(18.dp))
                }
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isLoading && rooms.isEmpty()) {
                    EmptyState(
                        message = "Data ruangan tidak ditemukan.",
                        onRetry = onRefresh
                    )
                } else {
                    val currentCellWidth = cellWidth * scale
                    val currentCellHeight = cellHeight * scale
                    val currentRoomWidth = roomWidth * scale
                    val currentHeaderHeight = headerHeight * scale

                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. TOP ROW: Intersection + Horizontal Headers (Sticky Top)
                        Row(modifier = Modifier.fillMaxWidth().background(headerBgColor)) {
                            // Top-Left Corner (Sticky both ways)
                            Box(
                                modifier = Modifier
                                    .size(width = currentRoomWidth, height = currentHeaderHeight)
                                    .border(0.5.dp, borderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "RUANGAN", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    fontWeight = FontWeight.Bold, 
                                    color = Color.Gray,
                                    fontSize = (9 * scale).sp
                                )
                            }
                            
                            // Horizontal Headers (Scroll Horizontal, Sticky Vertical)
                            Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                                Row {
                                    sessions.forEach { session ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = currentCellWidth, height = currentHeaderHeight)
                                                .border(0.5.dp, borderColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "SESI $session", 
                                                style = MaterialTheme.typography.labelSmall, 
                                                fontWeight = FontWeight.Bold, 
                                                color = Color.Gray,
                                                fontSize = (9 * scale).sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 2. BOTTOM ROW: Vertical Headers + Main Grid
                        Row(modifier = Modifier.weight(1f)) {
                            // Vertical Headers (Rooms) (Scroll Vertical, Sticky Horizontal)
                            Box(modifier = Modifier.fillMaxHeight().verticalScroll(verticalScrollState)) {
                                Column {
                                    rooms.forEach { room ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = currentRoomWidth, height = currentCellHeight)
                                                .background(if (isDark) Color(0xFF1A1A1A) else Color.White)
                                                .border(0.5.dp, borderColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                room.name ?: "", 
                                                fontWeight = FontWeight.Bold, 
                                                style = MaterialTheme.typography.bodySmall, 
                                                textAlign = TextAlign.Center,
                                                fontSize = (10 * scale).sp,
                                                lineHeight = (12 * scale).sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Main Grid (Scroll both ways)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScrollState)
                                    .verticalScroll(verticalScrollState)
                                    .background(if (isDark) Color(0xFF121212) else Color(0xFFFAFAFA))
                            ) {
                                Column {
                                    rooms.forEach { room ->
                                        Row {
                                            val roomSchedules = schedulesByRoom[room.name] ?: emptyList()
                                            var currentSesi = 1
                                            while (currentSesi <= 11) {
                                                val schedule = roomSchedules.find { it.sessionStart == currentSesi }
                                                if (schedule != null) {
                                                    val duration = schedule.sessionDuration ?: 1
                                                    Box(
                                                        modifier = Modifier
                                                            .width(currentCellWidth * duration)
                                                            .height(currentCellHeight)
                                                            .border(0.5.dp, borderColor)
                                                            .padding((4 * scale).dp)
                                                    ) {
                                                        ScheduleGridItem(
                                                            schedule = schedule,
                                                            scale = scale,
                                                            onClick = { selectedScheduleForDetail = it }
                                                        )
                                                    }
                                                    currentSesi += duration
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(currentCellWidth)
                                                            .height(currentCellHeight)
                                                            .border(0.5.dp, borderColor)
                                                            .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f))
                                                    )
                                                    currentSesi++
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { scale = (scale + 0.1f).coerceAtMost(2.0f) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }
                SmallFloatingActionButton(
                    onClick = { scale = 1.0f },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
                }
                SmallFloatingActionButton(
                    onClick = { scale = (scale - 0.1f).coerceAtLeast(0.5f) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
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
fun ScheduleGridItem(
    schedule: ScheduleItem,
    scale: Float = 1.0f,
    onClick: (ScheduleItem) -> Unit
) {
    val courseName = schedule.course?.name ?: "No Subject"
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    // Warna berdasarkan gambar yang diberikan
    val colorPalette = remember(courseName) {
        val hash = abs(courseName.hashCode())
        val colors = listOf(
            // (Background, Text)
            Color(0xFFFCE4EC) to Color(0xFFE91E63), // Pink (Semester 2 - Kelas B/D)
            Color(0xFFE8F5E9) to Color(0xFF4CAF50), // Green
            Color(0xFFE3F2FD) to Color(0xFF2196F3), // Blue (Semester 2 - Kelas A/P)
            Color(0xFFFFF3E0) to Color(0xFFFF9800), // Orange (Semester 4 - Kelas B)
            Color(0xFFF3E5F5) to Color(0xFF9C27B0), // Purple (Semester 4 - Kelas C)
            Color(0xFFE0F7FA) to Color(0xFF00BCD4)  // Cyan
        )
        colors[hash % colors.size]
    }

    val (bgColor, textColor) = if (isDark) {
        colorPalette.second.copy(alpha = 0.2f) to colorPalette.first
    } else {
        colorPalette.first to colorPalette.second
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick(schedule) },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape((6 * scale).dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isDark) null else androidx.compose.foundation.BorderStroke((0.5 * scale).dp, textColor.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Colored side bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((4 * scale).dp)
                    .background(textColor.copy(alpha = 0.8f))
            )
            
            Column(
                modifier = Modifier.padding((8 * scale).dp),
                verticalArrangement = Arrangement.spacedBy((2 * scale).dp)
            ) {
                if (scale > 0.6f) {
                    Text(
                        "${schedule.course?.description ?: schedule.semester?.name ?: "Semester ${schedule.semesterId ?: "-"}"} • Kelas ${schedule.course?.className ?: "-"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (9 * scale).sp,
                        maxLines = 1
                    )
                }
                Text(
                    courseName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = if (scale < 0.7f) 1 else 2,
                    lineHeight = (11 * scale).sp,
                    fontSize = (11 * scale).sp
                )
                if (scale > 0.75f) {
                    Text(
                        schedule.lecturerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = (9 * scale).sp,
                        maxLines = 1
                    )
                }
                
                if (scale > 0.5f) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    val sessionStart = schedule.sessionStart ?: 1
                    val sessionDuration = schedule.sessionDuration ?: 1
                    val sessionEnd = sessionStart + sessionDuration - 1
                    val sessionText = if (sessionDuration <= 1) {
                        "Sesi $sessionStart"
                    } else {
                        "Sesi $sessionStart - $sessionEnd"
                    }
                    
                    Surface(
                        color = textColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape((4 * scale).dp)
                    ) {
                        Text(
                            if (scale > 0.9f) "$sessionText | ${schedule.startTime} - ${schedule.endTime}" else sessionText,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (8 * scale).sp,
                            modifier = Modifier.padding(horizontal = (4 * scale).dp, vertical = (2 * scale).dp)
                        )
                    }
                }
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detail Jadwal", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) { 
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        ) 
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                DetailInfoItem("MATA KULIAH", "${schedule.course?.name} (${schedule.course?.id ?: "-"})")
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailInfoItem("KELAS", schedule.course?.className ?: "-", Modifier.weight(1f))
                    DetailInfoItem("SEMESTER", schedule.course?.description ?: schedule.semester?.name ?: "Semester ${schedule.semesterId ?: "-"}", Modifier.weight(1f))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailInfoItem("RUANGAN", schedule.room?.name ?: "-", Modifier.weight(1f))
                    DetailInfoItem("HARI", schedule.day, Modifier.weight(1f))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val sessionStart = schedule.sessionStart ?: 1
                    val sessionDuration = schedule.sessionDuration ?: 1
                    DetailInfoItem("WAKTU", "${schedule.startTime} - ${schedule.endTime}", Modifier.weight(1f))
                    DetailInfoItem("SESI", "Sesi $sessionStart - ${sessionStart + sessionDuration - 1}", Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "DOSEN PENGAJAR", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                schedule.lecturerName.take(1),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        schedule.lecturerName,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DetailInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value, 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestContent(
    padding: PaddingValues,
    requests: List<ValidationRequest>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onRequestNew: () -> Unit,
    onRequestClick: (ValidationRequest) -> Unit = {}
) {
    val processingRequests = requests.filter { it.status == RequestStatus.PENDING || it.status == RequestStatus.FORWARDED }
    val completedRequests = requests.filter { it.status == RequestStatus.APPROVED || it.status == RequestStatus.REJECTED }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Request Jadwal & Ruangan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Pantau status pengajuan penggunaan ruangan Anda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SectionHeader(title = "Sedang Diproses")
                    DashboardCard {
                        if (processingRequests.isEmpty()) {
                            Text("Tidak ada request yang sedang diproses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column {
                                processingRequests.forEachIndexed { index, request ->
                                    RequestItem(
                                        request = request,
                                        onClick = { onRequestClick(request) }
                                    )
                                    if (index < processingRequests.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SectionHeader(title = "Selesai")
                    DashboardCard {
                        if (completedRequests.isEmpty()) {
                            Text("Belum ada request yang selesai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column {
                                completedRequests.forEachIndexed { index, request ->
                                    RequestItem(
                                        request = request,
                                        onClick = { onRequestClick(request) }
                                    )
                                    if (index < completedRequests.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // BUTTON BUAT PENGAJUAN BARU
                    Button(
                        onClick = onRequestNew,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buat Pengajuan Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsContent(padding: PaddingValues, authViewModel: AuthViewModel, themeViewModel: ThemeViewModel) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                // Untuk demo, kita asumsikan updateAvatar hanya update State di ViewModel
                // Di aplikasi asli ini akan upload ke Supabase Storage
                authViewModel.updateAvatar(it.toString())
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Section
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentUser?.avatarUrl != null) {
                AsyncImage(
                    model = currentUser?.avatarUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Edit Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = currentUser?.name ?: "User",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = currentUser?.email ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Theme Setting (Segmented Choice)
        Text(
            text = "Tampilan Aplikasi",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ThemeOptionRow(
                    title = "Terang (Light)",
                    icon = Icons.Default.LightMode,
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { themeViewModel.setThemeMode(ThemeMode.LIGHT) }
                )
                ThemeOptionRow(
                    title = "Gelap (Dark)",
                    icon = Icons.Default.DarkMode,
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { themeViewModel.setThemeMode(ThemeMode.DARK) }
                )
                ThemeOptionRow(
                    title = "Default Sistem",
                    icon = Icons.Default.SettingsSuggest,
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { themeViewModel.setThemeMode(ThemeMode.SYSTEM) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Logout / Keluar", fontWeight = FontWeight.Bold)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }
                ) {
                    Text("Ya, Keluar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CustomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    onRetry: () -> Unit,
    icon: ImageVector = Icons.Default.CloudOff
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Muat Ulang Data")
        }
    }
}


// Custom shape dengan notch di tengah untuk AI button
class NotchedShape(
    private val notchRadius: androidx.compose.ui.unit.Dp,
    private val notchMargin: androidx.compose.ui.unit.Dp
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val notchRadiusPx = with(density) { notchRadius.toPx() }
        val notchMarginPx = with(density) { notchMargin.toPx() }
        
        val path = androidx.compose.ui.graphics.Path().apply {
            val centerX = size.width / 2f
            
            // Start from top left with rounded corner
            moveTo(24f, 0f)
            
            // Top left corner arc
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, 48f, 48f),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            
            // Left edge
            lineTo(0f, size.height - 24f)
            
            // Bottom left corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, size.height - 48f, 48f, size.height),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            
            // Bottom edge to notch
            lineTo(centerX - notchRadiusPx - notchMarginPx, size.height)
            
            // Semicircle notch UP (untuk AI button dari bawah)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = centerX - notchRadiusPx,
                    top = size.height - notchRadiusPx * 2,
                    right = centerX + notchRadiusPx,
                    bottom = size.height
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            
            // Continue bottom edge
            lineTo(size.width - 24f, size.height)
            
            // Bottom right corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    size.width - 48f,
                    size.height - 48f,
                    size.width,
                    size.height
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            
            // Right edge
            lineTo(size.width, 24f)
            
            // Top right corner
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    size.width - 48f,
                    0f,
                    size.width,
                    48f
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            
            // Close path
            close()
        }
        
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
