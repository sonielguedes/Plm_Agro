package com.soniel.plmagro.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardFormattersTest {
    @Test
    fun formatJourneySinceUsesElapsedHoursAndMinutes() {
        val start = 1_000L
        val now = start + (2 * 60 * 60 * 1000) + (7 * 60 * 1000)

        assertEquals("02h07", DashboardFormatters.formatJourneySince(start, now))
    }

    @Test
    fun formatStorageShowsUsedPercentAndDetail() {
        val storage = DashboardFormatters.formatStorage(freeDiskMb = 250L, totalDiskMb = 1000L)

        assertEquals("75%", storage.percent)
        assertEquals("750 MB / 1000 MB", storage.detail)
    }

    @Test
    fun heartbeatIsOkOnlyInsideTimeout() {
        assertTrue(DashboardFormatters.isHeartbeatOk(lastHeartbeatTime = 1_000L, nowMillis = 61_000L, timeoutMillis = 120_000L))
        assertFalse(DashboardFormatters.isHeartbeatOk(lastHeartbeatTime = 1_000L, nowMillis = 181_001L, timeoutMillis = 120_000L))
    }
}
