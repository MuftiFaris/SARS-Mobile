package com.informatika.sars.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatika.sars.data.model.AlternativeSlot
import com.informatika.sars.data.model.RequestResponse
import com.informatika.sars.data.model.SubmitRequestDto
import com.informatika.sars.data.repository.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StudentRequestUiState {
    object Idle : StudentRequestUiState()
    object Loading : StudentRequestUiState()
    data class Success(val response: RequestResponse) : StudentRequestUiState()
    data class Conflict(val response: RequestResponse, val alternatives: List<AlternativeSlot>) : StudentRequestUiState()
    data class Error(val message: String) : StudentRequestUiState()
}

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val repository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentRequestUiState>(StudentRequestUiState.Idle)
    val uiState: StateFlow<StudentRequestUiState> = _uiState

    fun submitRequest(
        scheduleId: Long,
        requestType: String,
        targetDate: String?,
        effectiveFromDate: String?,
        proposedDay: String?,
        proposedStartTime: String?,
        proposedEndTime: String?,
        proposedRoomId: Long?,
        reason: String
    ) {
        if (reason.length < 20) {
            _uiState.value = StudentRequestUiState.Error("Alasan harus minimal 20 karakter.")
            return
        }

        viewModelScope.launch {
            _uiState.value = StudentRequestUiState.Loading
            repository.submitJadwalRequest(
                SubmitRequestDto(
                    scheduleId = scheduleId,
                    requestType = requestType,
                    targetDate = targetDate,
                    effectiveFromDate = effectiveFromDate,
                    proposedDay = proposedDay,
                    proposedStartTime = proposedStartTime,
                    proposedEndTime = proposedEndTime,
                    proposedRoomId = proposedRoomId,
                    reason = reason
                )
            ).onSuccess { response ->
                if (response.hasConflict && response.alternatives != null) {
                    _uiState.value = StudentRequestUiState.Conflict(response, response.alternatives)
                } else {
                    _uiState.value = StudentRequestUiState.Success(response)
                }
            }.onFailure { error ->
                _uiState.value = StudentRequestUiState.Error(error.message ?: "Terjadi kesalahan koneksi.")
            }
        }
    }

    fun resetState() {
        _uiState.value = StudentRequestUiState.Idle
    }
}
