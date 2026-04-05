package com.comp3074_101384549.projectui.utils

import java.util.Calendar

/**
 * Utility class to parse availability strings and determine which days are available.
 *
 * Supported formats:
 * - "Mon-Fri", "Monday-Friday" → Weekdays
 * - "Sat-Sun", "Weekends" → Weekends
 * - "Fridays", "Friday" → Specific day
 * - "Mon, Wed, Fri" → Multiple specific days
 * - "Daily", "Everyday", "All days" → All days
 * - "9am-5pm" (time only, no day restriction) → All days
 */
object AvailabilityParser {

    // Map of day names to Calendar day constants
    private val dayMap = mapOf(
        "sunday" to Calendar.SUNDAY,
        "sun" to Calendar.SUNDAY,
        "monday" to Calendar.MONDAY,
        "mon" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY,
        "tue" to Calendar.TUESDAY,
        "tues" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY,
        "wed" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY,
        "thu" to Calendar.THURSDAY,
        "thur" to Calendar.THURSDAY,
        "thurs" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY,
        "fri" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY,
        "sat" to Calendar.SATURDAY
    )

    // Plural forms
    private val pluralDayMap = mapOf(
        "sundays" to Calendar.SUNDAY,
        "mondays" to Calendar.MONDAY,
        "tuesdays" to Calendar.TUESDAY,
        "wednesdays" to Calendar.WEDNESDAY,
        "thursdays" to Calendar.THURSDAY,
        "fridays" to Calendar.FRIDAY,
        "saturdays" to Calendar.SATURDAY
    )

    /**
     * Parses an availability string and returns a set of allowed days (Calendar constants).
     * Returns null if all days are allowed (no day restriction found).
     */
    fun parseAvailableDays(availability: String): Set<Int>? {
        val lowerAvailability = availability.lowercase().trim()

        // Check for "daily", "everyday", "all days" - no restriction
        if (lowerAvailability.contains("daily") ||
            lowerAvailability.contains("everyday") ||
            lowerAvailability.contains("all day") ||
            lowerAvailability.contains("7 days") ||
            lowerAvailability.contains("24/7")) {
            return null // No restriction
        }

        // Check for "weekends"
        if (lowerAvailability.contains("weekend")) {
            return setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        }

        // Check for "weekdays"
        if (lowerAvailability.contains("weekday")) {
            return setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY)
        }

        val availableDays = mutableSetOf<Int>()

        // Check for day ranges like "Mon-Fri" or "Monday-Friday"
        val rangePattern = Regex("(\\w+)\\s*[-–—to]+\\s*(\\w+)", RegexOption.IGNORE_CASE)
        val rangeMatch = rangePattern.find(lowerAvailability)
        if (rangeMatch != null) {
            val startDay = dayMap[rangeMatch.groupValues[1].lowercase()]
            val endDay = dayMap[rangeMatch.groupValues[2].lowercase()]

            if (startDay != null && endDay != null) {
                // Add all days in the range
                var current: Int = startDay!!
                while (true) {
                    availableDays.add(current)
                    if (current == endDay) break
                    current = if (current == Calendar.SATURDAY) Calendar.SUNDAY else current + 1
                }
                return availableDays
            }
        }

        // Check for individual days (including plural forms)
        val allDayMaps = dayMap + pluralDayMap
        for ((dayName, dayConstant) in allDayMaps) {
            if (lowerAvailability.contains(dayName)) {
                availableDays.add(dayConstant)
            }
        }

        // If we found specific days, return them
        if (availableDays.isNotEmpty()) {
            return availableDays
        }

        // If only time is specified (e.g., "9am-5pm") without days, allow all days
        val timePattern = Regex("\\d{1,2}\\s*(am|pm|:)")
        if (timePattern.containsMatchIn(lowerAvailability)) {
            return null // Time only, all days allowed
        }

        // Default: no restriction found
        return null
    }

    /**
     * Checks if a specific date is available based on the availability string.
     */
    fun isDateAvailable(availability: String, calendar: Calendar): Boolean {
        val availableDays = parseAvailableDays(availability) ?: return true
        return calendar.get(Calendar.DAY_OF_WEEK) in availableDays
    }

    /**
     * Returns a human-readable description of the available days.
     */
    fun getAvailableDaysDescription(availability: String): String {
        val availableDays = parseAvailableDays(availability) ?: return "All days"

        val dayNames = mapOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )

        return availableDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}
