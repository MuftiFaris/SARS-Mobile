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
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DashboardViewModel : ViewModel() {
    private var realtimeJob: Job? = null
    private var currentUser: User? = null

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

                val newRequest = mutableMapOf<String, Any?>(
                    "request_code" to requestCode,
                    "requester_id" to requesterId,
                    "schedule_id" to scheduleId,
                    "semester_id" to semesterId,
                    "request_type" to requestType,
                    "reason" to reason,
                    "status" to "PENDING",
                    "conflict_checked" to false,
                    "has_conflict" to false,
                    "proposed_day" to (proposedDay ?: ""),
                    "proposed_start_time" to (proposedStartTime ?: ""),
                    "proposed_end_time" to (proposedEndTime ?: ""),
                    "proposed_room_id" to proposedRoomId,
                    "target_date" to targetDate,
                    "effective_from_date" to effectiveFromDate,
                    "student_name" to studentName,
                    "subject" to subject,
                    "room" to room,
                    "time_slot" to timeSlot
                )
                
                // Remove null values
                val cleanedRequest = newRequest.filterValues { it != null }
                
                Log.d("DashboardViewModel", "Submitting request with ${cleanedRequest.size} fields")
                cleanedRequest.forEach { (k, v) -> Log.d("DashboardViewModel", "$k = $v") }
                
                SupabaseClient.client.postgrest["change_requests"].insert(cleanedRequest)

                Log.d("DashboardViewModel", "Submit successful")
                _submitSuccess.value = true
                fetchRequests(currentUser) 
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Submit failed: ${e.message}", e)
                Log.e("DashboardViewModel", "Error cause: ${e.cause}")
                e.printStackTrace()
                _submitSuccess.value = false
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun resetSubmitStatus() { _submitSuccess.value = null }

    fun startListeningToRequests(user: User?, onNotify: (String, String) -> Unit) {
        if (user == null) return
        this.currentUser = user
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                val channel = SupabaseClient.client.realtime.channel("requests_${user.id}")
                
                // IMPORTANT: Setup flow BEFORE subscribe
                val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "change_requests"
                }
                
                // Subscribe after flow setup
                channel.subscribe()
                
                // Now listen to changes
                changeFlow.onEach { action ->
                    fetchRequests(user)
                    try {
                        val updated = action.decodeRecord<ValidationRequest>()
                        if (updated.status == RequestStatus.APPROVED) {
                            onNotify("Update Pengajuan", "Status pengajuan Anda telah diperbarui.")
                        }
                    } catch (e: Exception) {
                        Log.e("DashboardViewModel", "Decode error", e)
                    }
                }.launchIn(this)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Realtime setup failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
    }
}
