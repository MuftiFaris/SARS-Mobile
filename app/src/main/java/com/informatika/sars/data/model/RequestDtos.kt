package com.informatika.sars.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlternativeSlot(
    @SerialName("room_id") val roomId: Long,
    @SerialName("room_code") val roomCode: String,
    @SerialName("room_name") val roomName: String,
    @SerialName("day") val day: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String
)

@Serializable
data class SubmitRequestDto(
    @SerialName("schedule_id") val scheduleId: Long,
    @SerialName("request_type") val requestType: String,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("effective_from_date") val effectiveFromDate: String? = null,
    @SerialName("proposed_day") val proposedDay: String? = null,
    @SerialName("proposed_start_time") val proposedStartTime: String? = null,
    @SerialName("proposed_end_time") val proposedEndTime: String? = null,
    @SerialName("proposed_room_id") val proposedRoomId: Long? = null,
    @SerialName("reason") val reason: String
)

@Serializable
data class RequestResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("request_code") val requestCode: String? = null,
    @SerialName("has_conflict") val hasConflict: Boolean,
    @SerialName("alternatives") val alternatives: List<AlternativeSlot>? = null,
    @SerialName("message") val message: String? = null
)
