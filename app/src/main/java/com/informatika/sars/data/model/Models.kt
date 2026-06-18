package com.informatika.sars.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
enum class UserRole {
    STUDENT, LECTURER, ASLAB, ADMIN
}

@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val nim: String,
    val role: UserRole,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class Semester(
    val id: Long,
    val name: String? = null,
    @SerialName("academic_year")
    val academicYear: String? = null,
    val term: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Course(
    val id: Long,
    @SerialName("semester_id")
    val semesterId: Long? = null,
    val code: String? = null,
    val name: String? = null,
    val credits: Int? = null,
    @SerialName("class_name")
    val className: String? = null,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class Room(
    val id: Long,
    val code: String? = null,
    val name: String? = "",
    val capacity: Int? = null,
    val building: String? = null,
    val floor: Int? = null,
    val type: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class TeachingAssignment(
    val id: Long,
    @SerialName("schedule_id")
    val scheduleId: Long? = null,
    @SerialName("user_id")
    val userId: Long? = null,
    @SerialName("role_in_class")
    val roleInClass: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    // Join with users
    @SerialName("users")
    private val usersRaw: JsonElement? = null
) {
    val user: User? get() {
        if (usersRaw == null || usersRaw.toString() == "null") return null
        val json = Json { ignoreUnknownKeys = true }
        return try {
            // Check if it's an object or an array of one element (Postgrest join)
            val dbUser = try {
                json.decodeFromJsonElement<DbUser>(usersRaw)
            } catch (e: Exception) {
                json.decodeFromJsonElement<List<DbUser>>(usersRaw).firstOrNull()
            }
            dbUser?.let {
                User(
                    id = it.id ?: 0L,
                    name = it.name ?: "User",
                    email = it.email ?: "",
                    nim = it.nimNip ?: "-",
                    role = try { UserRole.valueOf(it.role?.uppercase() ?: "LECTURER") } catch (e: Exception) { UserRole.LECTURER },
                    avatarUrl = it.avatarUrl
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class DbUser(
    val id: Long? = null,
    val name: String? = "",
    val email: String? = "",
    @SerialName("nim_nip")
    val nimNip: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val role: String? = null
)

@Serializable
data class ScheduleItem(
    val id: Long,
    @SerialName("course_id")
    val courseId: Long? = null,
    @SerialName("room_id")
    val roomId: Long? = null,
    @SerialName("semester_id")
    val semesterId: Long? = null,
    @SerialName("day_of_week")
    val dayOfWeek: String? = null,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("session_start")
    val sessionStart: Int? = null,
    @SerialName("session_duration")
    val sessionDuration: Int? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    
    // Relationships (Joined data)
    @SerialName("courses")
    private val coursesRaw: JsonElement? = null,
    @SerialName("rooms")
    private val roomsRaw: JsonElement? = null,
    @SerialName("semesters")
    private val semestersRaw: JsonElement? = null,
    @SerialName("teaching_assignments")
    private val assignmentsRaw: JsonElement? = null,
    
    val status: String? = null // For UI display
) {
    val day: String get() = dayOfWeek?.uppercase() ?: "UNKNOWN"
    val course: Course? get() = parseRelation<Course>(coursesRaw)
    val room: Room? get() = parseRelation<Room>(roomsRaw)
    val semester: Semester? get() = parseRelation<Semester>(semestersRaw)
    
    val lecturerName: String get() {
        val assignments = parseRelationList<TeachingAssignment>(assignmentsRaw)
        return assignments.firstOrNull()?.user?.name ?: "No Lecturer"
    }

    private inline fun <reified T> parseRelationList(element: JsonElement?): List<T> {
        if (element == null || element.toString() == "null") return emptyList()
        val json = Json { ignoreUnknownKeys = true }
        return try {
            json.decodeFromJsonElement<List<T>>(element)
        } catch (e: Exception) {
            try {
                listOf(json.decodeFromJsonElement<T>(element))
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    private inline fun <reified T> parseRelation(element: JsonElement?): T? {
        if (element == null || element.toString() == "null") return null
        val json = Json { ignoreUnknownKeys = true }
        return try {
            json.decodeFromJsonElement<T>(element)
        } catch (e: Exception) {
            try {
                json.decodeFromJsonElement<List<T>>(element).firstOrNull()
            } catch (e2: Exception) {
                null
            }
        }
    }
}

@Serializable
data class ValidationRequest(
    val id: Long? = null,
    @SerialName("request_code")
    val requestCode: String? = null,
    @SerialName("requester_id")
    val requesterId: Long? = null,
    @SerialName("schedule_id")
    val scheduleId: Long? = null,
    @SerialName("semester_id")
    val semesterId: Long? = null,
    @SerialName("request_type")
    val requestType: String? = null,
    @SerialName("target_date")
    val targetDate: String? = null,
    @SerialName("effective_from_date")
    val effectiveFromDate: String? = null,
    @SerialName("proposed_day")
    val proposedDay: String? = null,
    @SerialName("proposed_start_time")
    val proposedStartTime: String? = null,
    @SerialName("proposed_end_time")
    val proposedEndTime: String? = null,
    @SerialName("proposed_room_id")
    val proposedRoomId: Long? = null,
    val reason: String? = null,
    @SerialName("attachment_url")
    val attachmentUrl: String? = null,
    val status: String = "PENDING",
    @SerialName("conflict_checked")
    val conflictChecked: Boolean = false,
    @SerialName("has_conflict")
    val hasConflict: Boolean = false,
    @SerialName("created_at")
    val timestamp: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    // Helper computed properties (not serialized)
    @kotlinx.serialization.Transient
    val subject: String? = null
    
    @kotlinx.serialization.Transient
    val room: String? = null
    
    @kotlinx.serialization.Transient
    val proposedRoomName: String? = null
    
    @kotlinx.serialization.Transient
    val studentName: String? = null
    
    @kotlinx.serialization.Transient
    val timeSlot: String? = null
}

@Serializable
enum class RequestStatus {
    @SerialName("PENDING") PENDING,
    @SerialName("FORWARDED") FORWARDED,
    @SerialName("APPROVED") APPROVED,
    @SerialName("REJECTED") REJECTED
}
