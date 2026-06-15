package com.informatika.sars.data.repository

import com.informatika.sars.data.model.RequestResponse
import com.informatika.sars.data.model.SubmitRequestDto
import com.informatika.sars.data.model.ValidationRequest
import com.informatika.sars.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RequestRepository {

    suspend fun submitJadwalRequest(dto: SubmitRequestDto): Result<RequestResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Convert DTO to ValidationRequest for insertion
            val request = ValidationRequest(
                scheduleId = dto.scheduleId,
                requestType = dto.requestType,
                targetDate = dto.targetDate,
                effectiveFromDate = dto.effectiveFromDate,
                proposedDay = dto.proposedDay,
                proposedStartTime = dto.proposedStartTime,
                proposedEndTime = dto.proposedEndTime,
                proposedRoomId = dto.proposedRoomId,
                reason = dto.reason
            )

            // Insert into validation_requests table using Supabase Postgrest
            SupabaseClient.client.postgrest.from("validation_requests").insert(request)

            // Return success response
            val result = RequestResponse(
                success = true,
                requestCode = null,
                hasConflict = false,
                alternatives = null
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(Exception("Supabase error: ${e.message}"))
        }
    }
}
