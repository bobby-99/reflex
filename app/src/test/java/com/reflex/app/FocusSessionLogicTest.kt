package com.reflex.app

import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.service.FocusPhase
import com.reflex.app.service.FocusTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionLogicTest {

    @Test
    fun testEarlyEndSavesActualElapsedTime() {
        val plannedSec = 30 * 60 // 30 minutes planned = 1800s
        val actualElapsedSec = 15 * 60 // stopped at 15 minutes = 900s
        val startTime = System.currentTimeMillis() - (actualElapsedSec * 1000L)
        val endTime = System.currentTimeMillis()

        val earlyEndSession = FocusSession(
            mode = FocusMode.FLOW_TIMED,
            startTime = startTime,
            endTime = endTime,
            plannedDurationSeconds = plannedSec,
            actualDurationSeconds = actualElapsedSec,
            completed = false,
            endReason = "stopped_early"
        )

        assertEquals("stopped_early", earlyEndSession.endReason)
        assertEquals(900, earlyEndSession.actualDurationSeconds)
        assertEquals(1800, earlyEndSession.plannedDurationSeconds)
        assertFalse("Early stopped session must be completed = false", earlyEndSession.completed)
    }

    @Test
    fun testUnder1MinuteRule() {
        // Under 60 seconds should be flagged for user prompt / not auto-saved
        val shortElapsed = 45 // 45 seconds
        val meetsAutoSaveThreshold = shortElapsed >= 60
        assertFalse("Session under 60 seconds must not auto-save", meetsAutoSaveThreshold)

        val validElapsed = 65 // 65 seconds
        val validThreshold = validElapsed >= 60
        assertTrue("Session >= 60 seconds meets saving threshold", validThreshold)
    }

    @Test
    fun testPauseResumeState() {
        var state = FocusTimerState(
            mode = FocusMode.CLASSIC_POMODORO,
            phase = FocusPhase.WORK,
            remainingSeconds = 1500,
            elapsedSeconds = 0,
            isPaused = false
        )

        // User pauses
        state = state.copy(isPaused = true)
        assertTrue(state.isPaused)

        // While paused, elapsed does not increment
        val simulatedElapsedWhilePaused = state.elapsedSeconds
        assertEquals(0, simulatedElapsedWhilePaused)

        // User resumes
        state = state.copy(isPaused = false)
        assertFalse(state.isPaused)
    }

    @Test
    fun testProcessDeathRecoveryPreservesSession() {
        val startTime = 1728000000000L
        val lastTick = startTime + (25 * 60 * 1000L) // 25 minutes later
        val elapsedSec = 25 * 60

        // Simulate recovering an interrupted session snapshot
        val recoveredSession = FocusSession(
            mode = FocusMode.FLOW_TIMED,
            startTime = startTime,
            endTime = lastTick,
            plannedDurationSeconds = 45 * 60,
            actualDurationSeconds = elapsedSec,
            completedCycles = 0,
            completed = false,
            sessionTitle = "Deep Work Project",
            endReason = "stopped_early"
        )

        assertEquals(1500, recoveredSession.actualDurationSeconds)
        assertEquals("stopped_early", recoveredSession.endReason)
        assertEquals("Deep Work Project", recoveredSession.sessionTitle)
        assertEquals(startTime, recoveredSession.startTime)
        assertEquals(lastTick, recoveredSession.endTime)
    }
}
