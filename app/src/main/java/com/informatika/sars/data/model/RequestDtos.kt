package com.informatika.sars.data.model

import com.google.gson.annotations.SerializedName

data class AlternativeSlot(
    @SerializedName("room_id") val roomId: Long,
    @SerializedName("room_code") val roomCode: String,
    @SerializedName("room_name") val roomName: String,
    @SerializedName("day") val day: String,
    @SerializedName("start") val start: String,
    @SerializedName("end") val end: String
)

data class SubmitRequestDto(
    @SerializedName("schedule_id") val scheduleId: Long,
    @SerializedName("request_type") val requestType: String,
    @SerializedName("target_date") val targetDate: String?,
    @SerializedName("effective_from_date") val effectiveFromDate: String?,
    @SerializedName("proposed_day") val proposedDay: String?,
    @SerializedName("proposed_start_time") val proposedStartTime: String?,
    @SerializedName("proposed_end_time") val proposedEndTime: String?,
    @SerializedName("proposed_room_id") val proposedRoomId: Long?,
    @SerializedName("reason") val reason: String
)

data class RequestResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("request_code") val requestCode: String?,
    @SerializedName("has_conflict") val hasConflict: Boolean,
    @SerializedName("alternatives") val alternatives: List<AlternativeSlot>?
)
