package com.cheese.watchlist.util

import java.time.LocalDate
import java.time.LocalTime

object GreetingUtils {
    /** Real-time greeting based on the device's current hour. */
    fun timeOfDayGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private val motivationalLines = listOf(
        "What are you in the mood to watch today?",
        "Got a plan for movie night?",
        "Something's waiting on your watchlist.",
        "Pick up where you left off?",
        "What's next on your list?"
    )

    /** Stable for the whole day, changes daily — feels intentional rather than random noise. */
    fun motivationalLine(): String {
        val index = LocalDate.now().dayOfYear % motivationalLines.size
        return motivationalLines[index]
    }
}
