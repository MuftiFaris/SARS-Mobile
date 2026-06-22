package com.informatika.sars.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatika.sars.data.model.ScheduleItem
import com.informatika.sars.data.model.ValidationRequest
import com.informatika.sars.data.model.User
import com.informatika.sars.data.model.UserRole
import com.informatika.sars.data.model.RequestStatus
import com.informatika.sars.data.model.Semester
import com.informatika.sars.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DashboardViewModel : ViewModel() {
    private val _notificationEvent = MutableStateFlow<Pair<String, String>?>(null)
    val notificationEvent: StateFlow<Pair<String, String>?> = _notificationEvent
    
    private var realtimeJob: Job? = null
    private var currentUser: User? = null
    private var previousRequestStates: Map<Long, String> = mutableMapOf() // Track previous statuses

    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules

    private val _rooms = MutableStateFlow<List<com.informatika.sars.data.model.Room>>(emptyList())
    val rooms: StateFlow<List<com.informatika.sars.data.model.Room>> = _rooms

    private val _semesters = MutableStateFlow<List<Semester>>(emptyList())
    val semesters: StateFlow<List<Semester>> = _semesters

    private val _requests = MutableStateFlow<List<ValidationRequest>>(emptyList())
    val requests: StateFlow<List<ValidationRequest>> = _requests

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _submitSuccess = MutableStateFlow<Boolean?>(null)
    val submitSuccess: StateFlow<Boolean?> = _submitSuccess

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorLog = MutableStateFlow<String?>(null)
    val errorLog: StateFlow<String?> = _errorLog

    fun fetchData(user: User? = null) {
        if (user != null) this.currentUser = user
        viewModelScope.launch {
            _isLoading.value = true
            _errorLog.value = null
            try {
                // 1. Fetch Rooms
                try {
                    val roomResult = SupabaseClient.client.postgrest["rooms"].select()
                    _rooms.value = roomResult.decodeAs<List<com.informatika.sars.data.model.Room>>()
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Room Error", e)
                }

                // 2. Fetch Semesters
                try {
                    val semesterResult = SupabaseClient.client.postgrest["semesters"].select()
                    _semesters.value = semesterResult.decodeAs<List<Semester>>()
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Semester Error", e)
                }

                // 3. Fetch Schedules
                try {
                    val query = "*, courses(*), rooms(*), semesters(*), teaching_assignments(*, users(*))"
                    val scheduleResult = SupabaseClient.client.postgrest["schedules"].select(Columns.raw(query))
                    _schedules.value = scheduleResult.decodeAs<List<ScheduleItem>>()
                } catch (e: Exception) {
                    Log.e("DashboardViewModel", "Schedule fetch failed", e)
                    try {
                        val scheduleResult = SupabaseClient.client.postgrest["schedules"].select()
                        _schedules.value = scheduleResult.decodeAs<List<ScheduleItem>>()
                    } catch (e2: Exception) {
                        _errorLog.value = "Koneksi Bermasalah"
                    }
                }

                // 4. Fetch Requests
                fetchRequests(user ?: currentUser)
                
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Fetch Data Error", e)
                _errorLog.value = "Gagal memuat data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchRequests(user: User? = null) {
        try {
            val query = "*, schedules(*, courses(*), rooms(*)), proposed_room:rooms!proposed_room_id(*), users(*)"
            val response = SupabaseClient.client.postgrest["change_requests"].select(Columns.raw(query)) {
                if (user?.role == UserRole.STUDENT) {
                    filter {
                        eq("requester_id", user.id)
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(15)
            }
            _requests.value = response.decodeAs<List<ValidationRequest>>()
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Request fetch failed", e)
            _requests.value = emptyList()
        }
    }

    fun updateRequestStatus(requestId: Long, status: RequestStatus) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.postgrest["change_requests"].update({
                    set("status", status.name)
                }) {
                    filter {
                        eq("id", requestId)
                    }
                }
                fetchRequests(currentUser)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Update status failed", e)
            }
        }
    }

    fun submitRequest(
        requesterId: Long,
        scheduleId: Long,
        semesterId: Long,
        requestType: String,
        reason: String,
        targetDate: String? = null,
        effectiveFromDate: String? = null,
        proposedDay: String? = null,
        proposedStartTime: String? = null,
        proposedEndTime: String? = null,
        proposedRoomId: Long? = null,
        studentName: String? = null,
        subject: String? = null,
        room: String? = null,
        timeSlot: String? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _submitSuccess.value = null
            try {
                // Validation
                if (proposedRoomId == null) {
                    Log.e("DashboardViewModel", "Submit failed: proposedRoomId is null")
                    _submitSuccess.value = false
                    _isSubmitting.value = false
                    return@launch
                }
                
                val requestCode = "REQ-${System.currentTimeMillis() / 1000}"

                val requestJson = buildJsonObject {
                    put("request_code", JsonPrimitive(requestCode))
                    put("requester_id", JsonPrimitive(requesterId))
                    put("schedule_id", JsonPrimitive(scheduleId))
                    put("semester_id", JsonPrimitive(semesterId))
                    put("request_type", JsonPrimitive(requestType))
                    put("reason", JsonPrimitive(reason))
                    put("status", JsonPrimitive("PENDING_ASLAB"))
                    put("conflict_checked", JsonPrimitive(false))
                    put("has_conflict", JsonPrimitive(false))
                    proposedDay?.let { put("proposed_day", JsonPrimitive(it)) }
                    proposedStartTime?.let { put("proposed_start_time", JsonPrimitive(it)) }
                    proposedEndTime?.let { put("proposed_end_time", JsonPrimitive(it)) }
                    proposedRoomId?.let { put("proposed_room_id", JsonPrimitive(it)) }
                    targetDate?.let { put("target_date", JsonPrimitive(it)) }
                    effectiveFromDate?.let { put("effective_from_date", JsonPrimitive(it)) }
                }
                
                Log.d("DashboardViewModel", "Submitting request JSON: $requestJson")
                
                try {
                    SupabaseClient.client.postgrest["change_requests"].insert(requestJson)
                    Log.d("DashboardViewModel", "Submit successful")
                    _submitSuccess.value = true
                    fetchRequests(currentUser)
                } catch (insertEx: Exception) {
                    Log.e("DashboardViewModel", "Insert to change_requests failed", insertEx)
                    // Try minimal
                    val minimalJson = buildJsonObject {
                        put("request_code", JsonPrimitive(requestCode))
                        put("requester_id", JsonPrimitive(requesterId))
                        put("schedule_id", JsonPrimitive(scheduleId))
                        put("semester_id", JsonPrimitive(semesterId))
                        put("request_type", JsonPrimitive(requestType))
                        put("reason", JsonPrimitive(reason))
                        put("status", JsonPrimitive("PENDING_ASLAB"))
                        proposedRoomId?.let { put("proposed_room_id", JsonPrimitive(it)) }
                    }
                    Log.d("DashboardViewModel", "Trying minimal: $minimalJson")
                    SupabaseClient.client.postgrest["change_requests"].insert(minimalJson)
                    Log.d("DashboardViewModel", "Minimal insert successful")
                    _submitSuccess.value = true
                    fetchRequests(currentUser)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Submit failed: ${e.message}", e)
                Log.e("DashboardViewModel", "Error cause: ${e.cause}")
                Log.e("DashboardViewModel", "Full stack trace: ${e.stackTraceToString()}")
                e.printStackTrace()
                _submitError.value = "Error: ${e.message ?: "Unknown error"}"
                _submitSuccess.value = false
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun resetSubmitStatus() { 
        _submitSuccess.value = null
        _submitError.value = null
    }

    fun startListeningToRequests(user: User?, onNotify: (String, String) -> Unit) {
        if (user == null) {
            Log.w("DashboardViewModel", "startListeningToRequests called but user is null")
            return
        }
        Log.i("DashboardViewModel", "✅ startListeningToRequests STARTED for user: ${user.name} (ID: ${user.id})")
        this.currentUser = user
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                Log.i("DashboardViewModel", "🚀 Polling job launched - starting infinite while loop")
                var pollCount = 0
                while (true) {
                    try {
                        pollCount++
                        Log.i("DashboardViewModel", "📊 POLL #$pollCount START - fetching requests for user ${currentUser?.id}...")
                        fetchRequests(currentUser)
                        Log.i("DashboardViewModel", "📊 POLL #$pollCount FETCH DONE")
                        
                        // Check for status changes
                        val currentRequests = _requests.value
                        Log.i("DashboardViewModel", "📊 POLL #$pollCount - found ${currentRequests.size} requests")
                        for (request in currentRequests) {
                            val requestId = request.id ?: continue
                            val previousStatus = previousRequestStates[requestId]
                            val currentStatus = request.status
                            
                            if (previousStatus != null && previousStatus != currentStatus) {
                                val message = when (currentStatus) {
                                    "APPROVED" -> "Pengajuan Anda telah disetujui!"
                                    "REJECTED" -> "Pengajuan Anda ditolak."
                                    "FORWARDED" -> "Pengajuan Anda sedang diproses admin."
                                    else -> "Status pengajuan berubah menjadi $currentStatus"
                                }
                                _notificationEvent.value = "SARS Notification" to message
                                onNotify("SARS Notification", message)
                                Log.i("DashboardViewModel", "🔔 NOTIFICATION: Request $requestId: $previousStatus -> $currentStatus")
                            }
                            
                            previousRequestStates = previousRequestStates.toMutableMap().apply {
                                this[requestId] = currentStatus ?: "UNKNOWN"
                            }
                        }
                        Log.i("DashboardViewModel", "📊 POLL #$pollCount END - waiting 5 seconds...")
                    } catch (pollEx: Exception) {
                        Log.e("DashboardViewModel", "❌ POLL #$pollCount ERROR", pollEx)
                    }
                    kotlinx.coroutines.delay(5000)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "❌ POLLING STOPPED - outer exception", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
    }
}
