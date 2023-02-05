package ru.yandex.disk.cleanup

import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.Mockito.mock
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.util.SystemClock
import java.text.SimpleDateFormat

class CleanupPolicyTest : AndroidTestCase2() {

    private val systemClock = mock(SystemClock::class.java)
    private val cleanupPolicy = CleanupPolicy(systemClock)
    private var format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    private val tooEarlyDate = format.parse("07.12.2017 08:59:59").time
    private val idealDate = format.parse("07.12.2017 15:50:00").time
    private val tooLateDate = format.parse("07.12.2017 21:00:01").time

    @Test
    fun `should not change hour`() {
        whenever(systemClock.currentTimeMillis()).thenReturn(idealDate)

        val expectedPostponed = idealDate + CleanupPolicy.ONE_DAY
        assertEquals(expectedPostponed, cleanupPolicy.nextPostponedCleanupDate)

        val expectedInitial = idealDate + CleanupPolicy.THREE_DAYS
        assertEquals(expectedInitial, cleanupPolicy.nextInitialCleanupDate)

        val expectedDefault= idealDate + CleanupPolicy.ONE_WEEK
        assertEquals(expectedDefault, cleanupPolicy.nextDefaultCleanupDate)

        val expectedMax = idealDate + CleanupPolicy.ONE_MONTH
        assertEquals(expectedMax, cleanupPolicy.nextMaxCleanupDate)
    }

    @Test
    fun `should select min threshold`() {
        whenever(systemClock.currentTimeMillis()).thenReturn(tooEarlyDate)

        val expectedPostponed = format.parse("08.12.2017 10:00:00").time
        assertEquals(expectedPostponed, cleanupPolicy.nextPostponedCleanupDate)

        val expectedInitial = format.parse("10.12.2017 10:00:00").time
        assertEquals(expectedInitial, cleanupPolicy.nextInitialCleanupDate)

        val expectedDefault = format.parse("14.12.2017 10:00:00").time
        assertEquals(expectedDefault, cleanupPolicy.nextDefaultCleanupDate)

        val expectedMax = format.parse("06.01.2018 10:00:00").time
        assertEquals(expectedMax, cleanupPolicy.nextMaxCleanupDate)

    }

    @Test
    fun `should select max threshold`() {
        whenever(systemClock.currentTimeMillis()).thenReturn(tooLateDate)

        val expectedPostponed = format.parse("08.12.2017 20:00:00").time
        assertEquals(expectedPostponed, cleanupPolicy.nextPostponedCleanupDate)

        val expectedInitial = format.parse("10.12.2017 20:00:00").time
        assertEquals(expectedInitial, cleanupPolicy.nextInitialCleanupDate)

        val expectedDefault = format.parse("14.12.2017 20:00:00").time
        assertEquals(expectedDefault, cleanupPolicy.nextDefaultCleanupDate)

        val expectedMax = format.parse("06.01.2018 20:00:00").time
        assertEquals(expectedMax, cleanupPolicy.nextMaxCleanupDate)

    }
 }
