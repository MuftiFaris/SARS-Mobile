package com.informatika.sars.data.repository

import com.informatika.sars.data.model.RequestResponse
import com.informatika.sars.data.model.SubmitRequestDto
import com.informatika.sars.data.model.ValidationRequest
import com.informatika.sars.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RequestRepository @Inject constructor() {

    suspend fun submitJadwalRequest(dto: SubmitRequestDto): Result<RequestResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Build ValidationRequest from DTO
            val request = ValidationRequest(
                scheduleId = dto.scheduleId,
                requestType = dto.requestType,
                targetDate = dto.targetDate,
                effectiveFromDate = dto.effectiveFromDate,
                proposedDay = dto.proposedDay,
                proposedStartTime = dto.proposedStartTime,
                proposedEndTime = dto.proposedEndTime,
                proposedRoomId = dto.proposedRoomId,
                reason = dto.reason,
                status = "PENDING"
            )

            // Insert to Supabase validation_requests table
            SupabaseClient.client.postgrest
                .from("validation_requests")
                .insert(request)

            // Return success response
            val response = RequestResponse(
                success = true,
                requestCode = "REQ-${System.currentTimeMillis()}",
                hasConflict = false,
                alternatives = null,
                message = "Pengajuan berhasil dikirim"
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Supabase error: ${e.message}"))
        }
    }

    suspend fun checkSlotConflict(dto: SubmitRequestDto): Result<RequestResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Query Supabase untuk cek conflict
            val existingRequests = SupabaseClient.client.postgrest
                .from("validation_requests")
                .select()
                .decodeList<ValidationRequest>()

            val hasConflict = existingRequests.any { existing ->
                existing.scheduleId == dto.scheduleId &&
                existing.proposedDay == dto.proposedDay &&
                existing.proposedStartTime == dto.proposedStartTime &&
                existing.proposedEndTime == dto.proposedEndTime &&
                existing.status != "REJECTED"
            }

            val response = RequestResponse(
                success = true,
                requestCode = null,
                hasConflict = hasConflict,
                alternatives = emptyList(),
                message = if (hasConflict) "Ada jadwal yang bentrok" else "Slot tersedia"
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Supabase error: ${e.message}"))
        }
    }
}
