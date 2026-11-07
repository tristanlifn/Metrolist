package com.metrolist.music.playback

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.metrolist.music.constants.SleepTimerCustomDaysKey
import com.metrolist.music.constants.SleepTimerDayTimesKey
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerEnabledKey
import com.metrolist.music.constants.SleepTimerEndTimeKey
import com.metrolist.music.constants.SleepTimerRepeatKey
import com.metrolist.music.constants.SleepTimerStartTimeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SleepTimerTest {
    // 2026-11-02 is a Monday.
    private fun at(day: Int, time: String) = LocalDateTime.parse("2026-11-%02dT$time".format(2 + day))

    @Test
    fun `schedule is off unless enabled`() {
        assertNull(automaticSleepTimerMinutes(mutablePreferencesOf(), at(0, "12:00")))
    }

    @Test
    fun `daily window uses the default minutes`() {
        val prefs = mutablePreferencesOf(SleepTimerEnabledKey to true, SleepTimerDefaultKey to 45f)
        assertEquals(45, automaticSleepTimerMinutes(prefs, at(0, "12:00")))
        assertNull(automaticSleepTimerMinutes(prefs, at(0, "23:30")))
    }

    @Test
    fun `overnight windows wrap past midnight`() {
        val prefs = mutablePreferencesOf(SleepTimerEnabledKey to true, SleepTimerStartTimeKey to "22:00", SleepTimerEndTimeKey to "06:00")
        assertEquals(30, automaticSleepTimerMinutes(prefs, at(0, "23:30")))
        assertEquals(30, automaticSleepTimerMinutes(prefs, at(1, "05:00")))
        assertNull(automaticSleepTimerMinutes(prefs, at(1, "12:00")))
    }

    @Test
    fun `weekday modes use per day windows and skip other days`() {
        val prefs =
            mutablePreferencesOf(
                SleepTimerEnabledKey to true,
                SleepTimerRepeatKey to "weekdays",
                SleepTimerDayTimesKey to "0=08:00-09:00",
            )
        assertEquals(30, automaticSleepTimerMinutes(prefs, at(0, "08:30")))
        assertNull(automaticSleepTimerMinutes(prefs, at(0, "12:00")))
        // Tuesday has no own window, so the global one applies.
        assertEquals(30, automaticSleepTimerMinutes(prefs, at(1, "12:00")))
        assertNull(automaticSleepTimerMinutes(prefs, at(5, "12:00")))
    }

    @Test
    fun `custom mode only runs on the chosen days`() {
        val prefs = mutablePreferencesOf(SleepTimerEnabledKey to true, SleepTimerRepeatKey to "custom", SleepTimerCustomDaysKey to "6")
        assertEquals(30, automaticSleepTimerMinutes(prefs, at(6, "12:00")))
        assertNull(automaticSleepTimerMinutes(prefs, at(0, "12:00")))
    }

    @Test
    fun `malformed times disable the schedule instead of throwing`() {
        val prefs = mutablePreferencesOf(SleepTimerEnabledKey to true, SleepTimerStartTimeKey to "nine")
        assertNull(automaticSleepTimerMinutes(prefs, at(0, "12:00")))
    }

    private class FakeTimer : PlayerSleepTimer(CoroutineScope(Dispatchers.Unconfined)) {
        var pauses = 0

        override fun pausePlayback() {
            pauses++
        }

        override val positionMs = 0L
        override val durationMs = 0L
    }

    @Test
    fun `end of song mode pauses once on the next transition`() {
        val timer = FakeTimer()
        timer.start(-1)
        assertTrue(timer.isActive && timer.pauseWhenSongEnd)
        timer.notifySongTransition()
        timer.notifySongTransition()
        assertEquals(1, timer.pauses)
        assertFalse(timer.isActive)
    }

    @Test
    fun `timed mode ignores transitions and clear cancels it`() {
        val timer = FakeTimer()
        timer.start(10)
        assertTrue(timer.triggerTime > System.currentTimeMillis())
        timer.notifySongTransition()
        assertEquals(0, timer.pauses)
        timer.clear()
        assertFalse(timer.isActive)
    }
}
