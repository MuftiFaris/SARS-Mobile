package com.informatika.sars.utils

import com.informatika.sars.data.model.Room
import com.informatika.sars.data.model.ScheduleItem

/**
 * Session-to-time mapping (fixed schedule)
 * Each session = ~50 minutes based on standard 07:30-10:00 = 3 sessions
 */
object SessionTimeMapping {
    fun sessionToStartTime(session: Int): String = when (session) {
        1 -> "07:30"
        2 -> "08:20"
        3 -> "09:10"
        4 -> "10:15"
        5 -> "11:05"
        6 -> "11:55"
        7 -> "13:15"
        8 -> "14:05"
        9 -> "14:55"
        10 -> "16:00"
        11 -> "16:50"
        else -> "00:00"
    }

    fun sessionToEndTime(session: Int, duration: Int = 1): String {
        val endSession = session + duration - 1
        return when (endSession) {
            1 -> "08:20"
            2 -> "09:10"
            3 -> "10:00"
            4 -> "11:05"
            5 -> "11:55"
            6 -> "12:45"
            7 -> "14:05"
            8 -> "14:55"
            9 -> "15:45"
            10 -> "16:50"
            11 -> "17:40"
            else -> "18:30"
        }
    }

    fun getDuration(startTime: String, endTime: String): Int {
        val startSession = timeToSession(startTime)
        val endSession = timeToSession(endTime)
        return if (startSession > 0 && endSession > 0) {
            maxOf(1, endSession - startSession + 1)
        } else {
            1
        }
    }

    fun timeToSession(time: String): Int = when (time) {
        "07:30" -> 1
        "08:20" -> 2
        "09:10" -> 3
        "10:15" -> 4
        "11:05" -> 5
        "11:55" -> 6
        "13:15" -> 7
        "14:05" -> 8
        "14:55" -> 9
        "16:00" -> 10
        "16:50" -> 11
        else -> -1
    }
}

/**
 * Find available time slots in a room for a given day
 * Returns list of (Room, StartTime, EndTime) tuples for available slots
 */
fun findAvailableSlots(
    room: Room,
    day: String,
    schedules: List<ScheduleItem>,
    excludeScheduleId: Long? = null,
    preferredDuration: Int = 1
): List<Triple<Room, String, String>> {
    val availableSlots = mutableListOf<Triple<Room, String, String>>()
    
    // Get all occupied sessions in this room on this day
    val occupiedSessions = mutableSetOf<Int>()
    schedules
        .filter {
            it.roomId == room.id &&
            it.day.trim().uppercase() == day.trim().uppercase() &&
            it.id != excludeScheduleId &&
            it.isActive
        }
        .forEach { schedule ->
            val startSession = schedule.sessionStart ?: SessionTimeMapping.timeToSession(schedule.startTime ?: "07:30")
            val duration = schedule.sessionDuration ?: 1
            
            if (startSession > 0) {
                for (s in startSession until (startSession + duration)) {
                    if (s in 1..11) occupiedSessions.add(s)
                }
            }
        }
    
    // Find consecutive free slots
    var freeStart = -1
    for (session in 1..11) {
        if (session !in occupiedSessions) {
            if (freeStart == -1) freeStart = session
        } else {
            if (freeStart != -1) {
                val duration = session - freeStart
                if (duration >= preferredDuration) {
                    val startTime = SessionTimeMapping.sessionToStartTime(freeStart)
                    val endTime = SessionTimeMapping.sessionToEndTime(freeStart + preferredDuration - 1, preferredDuration)
                    availableSlots.add(Triple(room, startTime, endTime))
                }
                freeStart = -1
            }
        }
    }
    
    // Handle end-of-day case
    if (freeStart != -1) {
        val duration = 12 - freeStart
        if (duration >= preferredDuration) {
            val startTime = SessionTimeMapping.sessionToStartTime(freeStart)
            val endTime = SessionTimeMapping.sessionToEndTime(freeStart + preferredDuration - 1, preferredDuration)
            availableSlots.add(Triple(room, startTime, endTime))
        }
    }
    
    return availableSlots
}

/**
 * Get recommended empty slots for a day across all rooms
 * Returns up to [limit] slots, prioritizing smaller rooms for flexibility
 */
fun getRecommendedEmptySlots(
    day: String,
    schedules: List<ScheduleItem>,
    rooms: List<Room>,
    excludeScheduleId: Long? = null,
    limit: Int = 8,
    preferredDuration: Int = 1
): List<Triple<Room, String, String>> {
    val allSlots = mutableListOf<Triple<Room, String, String>>()
    
    // Sort rooms to prioritize smaller ones (more flexibility)
    val sortedRooms = rooms.filter { it.isActive }.sortedBy { it.capacity ?: 0 }
    
    for (room in sortedRooms) {
        val slots = findAvailableSlots(room, day, schedules, excludeScheduleId, preferredDuration)
        allSlots.addAll(slots)
        
        if (allSlots.size >= limit) break
    }
    
    return allSlots.take(limit)
}
