/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.constants.SleepTimerCustomDaysKey
import com.metrolist.music.constants.SleepTimerDayTimesKey
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerEnabledKey
import com.metrolist.music.constants.SleepTimerEndTimeKey
import com.metrolist.music.constants.SleepTimerRepeatKey
import com.metrolist.music.constants.SleepTimerStartTimeKey
import com.metrolist.music.ui.component.decodeDayTimes
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Minutes for the automatic sleep timer if playback starting at [now] falls inside the scheduled window
 * from the player settings, or null when it shouldn't start.
 */
fun automaticSleepTimerMinutes(
    preferences: Preferences,
    now: LocalDateTime,
): Int? {
    if (preferences[SleepTimerEnabledKey] != true) return null

    val repeat = preferences[SleepTimerRepeatKey] ?: "daily"
    val startTimeSetting = preferences[SleepTimerStartTimeKey] ?: "09:00"
    val endTimeSetting = preferences[SleepTimerEndTimeKey] ?: "23:00"
    val defaultMinutes = (preferences[SleepTimerDefaultKey] ?: 30f).roundToInt()
    val customDays = (preferences[SleepTimerCustomDaysKey] ?: "0,1,2,3,4").split(",").mapNotNull { it.trim().toIntOrNull() }

    // 0 = Monday ... 6 = Sunday
    val dayIndex = now.dayOfWeek.value - 1
    val isDayAllowed =
        when (repeat) {
            "daily" -> true
            "weekdays" -> dayIndex in 0..4
            "weekends" -> dayIndex in 5..6
            // both groups active; per-day time handles the distinction
            "weekdays_weekends" -> true
            "custom" -> dayIndex in customDays
            else -> false
        }
    if (!isDayAllowed) return null

    // "daily" uses the single global time window.
    // All other modes store per-day times in the dayTimes map so that
    // e.g. weekdays and weekends can have different windows.
    val (startText, endText) =
        if (repeat != "daily") {
            decodeDayTimes(preferences[SleepTimerDayTimesKey] ?: "")[dayIndex] ?: (startTimeSetting to endTimeSetting)
        } else {
            startTimeSetting to endTimeSetting
        }
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val startTime = runCatching { LocalTime.parse(startText, formatter) }.getOrNull() ?: return null
    val endTime = runCatching { LocalTime.parse(endText, formatter) }.getOrNull() ?: return null
    val time = now.toLocalTime()

    // Support overnight ranges (e.g. 22:00–06:00) in addition to normal ranges
    val isTimeInRange =
        if (endTime.isAfter(startTime)) {
            time.isAfter(startTime) && time.isBefore(endTime)
        } else {
            time.isAfter(startTime) || time.isBefore(endTime)
        }
    return defaultMinutes.takeIf { isTimeInRange }
}
