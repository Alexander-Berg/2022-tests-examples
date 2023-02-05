package com.yandex.launcher.allapps

import android.content.SharedPreferences
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assume
import org.junit.Test
import java.util.Calendar

class NewAppsGridUpdateManagerTest: BaseRobolectricTest() {

    private val mockSharedPrefs = mock<SharedPreferences>()
    private val mockAppsGrid = mock<NewAppsGrid>()

    private val updateManager = spy(NewAppsGridUpdateManager(mockAppsGrid, mockSharedPrefs))

    override fun setUp() {
        super.setUp()
        doNothing().whenever(updateManager).rememberUpdateInfo(any())
        doNothing().whenever(updateManager).restoreUpdateInfo()
        doNothing().whenever(updateManager).updateCalendar()

        //to avoid blinking tests
        updateManager.calendar.value[Calendar.MINUTE] = 0
        clearInvocations(updateManager)
    }

    @Test
    fun `page not selected, update required, apps updated`() {
        verifyNoInteractions(mockAppsGrid)

        updateManager.requireUpdate = true
        updateManager.onPageSelected(false)

        verify(mockAppsGrid).updateApps(false)
    }

    @Test
    fun `page not selected, update not required, apps not updated`() {
        verifyNoInteractions(mockAppsGrid)

        updateManager.setHourNotChanged()

        updateManager.requireUpdate = false
        updateManager.onPageSelected(false)

        verifyNoInteractions(mockAppsGrid)
    }

    @Test
    fun `page selected, update required, apps not updated`() {
        verifyNoInteractions(mockAppsGrid)

        updateManager.setHourNotChanged()

        updateManager.requireUpdate = true
        updateManager.onPageSelected(true)

        verifyNoInteractions(mockAppsGrid)
    }

    @Test
    fun `page selected, update not required, apps not updated`() {
        verifyNoInteractions(mockAppsGrid)

        updateManager.setHourNotChanged()

        updateManager.requireUpdate = false
        updateManager.onPageSelected(true)

        verifyNoInteractions(mockAppsGrid)
    }

    @Test
    fun `page selected, app in foreground, app launched from all apps, update is required`() {
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        whenever(mockAppsGrid.isAppForeground).doReturn(true)

        updateManager.onHistoryChanged(true)

        assertThat(updateManager.requireUpdate, `is`(true))
    }

    @Test
    fun `page selected, app in foreground, app launched from all apps, update not performed`() {
        verifyNoInteractions(mockAppsGrid)

        mockAppsGrid.isAppForeground = true

        updateManager.onHistoryChanged(true)

        verifyNoInteractions(mockAppsGrid)
    }

    @Test
    fun `page selected, app in background, app launched from all apps, update not required`() {
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        mockAppsGrid.isAppForeground = false

        updateManager.onHistoryChanged(true)

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `page selected, app in background, app launched from all apps, update is performed`() {
        verifyNoInteractions(mockAppsGrid)

        mockAppsGrid.isAppForeground = false

        updateManager.onHistoryChanged(true)

        verify(mockAppsGrid).updateApps(false)
    }

    @Test
    fun `page not selected, app launched from all apps, update performed`() {
        verifyNoInteractions(mockAppsGrid)

        updateManager.onHistoryChanged(false)

        verify(mockAppsGrid).updateApps(false)
    }

    @Test
    fun `page not selected, app launched from all apps, update is not required`() {
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.onHistoryChanged(false)

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `on all apps closed, installed app is null, update not required`() {
        Assume.assumeThat(updateManager.installedApp, nullValue())
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.setHourNotChanged()

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `on all apps closed, open count is 0, update not required`() {
        Assume.assumeThat(updateManager.openCount, `is`(0))
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.setHourNotChanged()

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `on all apps closed, open count is 1, installed app is null, update not required`() {
        updateManager.openCount = 1
        Assume.assumeThat(updateManager.openCount, `is`(1))
        Assume.assumeThat(updateManager.installedApp, nullValue())
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.setHourNotChanged()

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `on all apps closed, open count is 0, installed app is NOT null, update not required`() {
        updateManager.installedApp = "installed app"
        Assume.assumeThat(updateManager.openCount, `is`(0))
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.setHourNotChanged()

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `on all apps closed, open count is 1, installed app is NOT null, update is required`() {
        updateManager.installedApp = "installed app"
        updateManager.openCount = 1
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireUpdate, `is`(true))
    }

    @Test
    fun `on all apps closed, open count is bigger than 1, installed app is NOT null, update is required`() {
        updateManager.installedApp = "installed app"
        updateManager.openCount = 2
        Assume.assumeThat(updateManager.requireUpdate, `is`(false))

        updateManager.setHourNotChanged()

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireUpdate, `is`(false))
    }

    @Test
    fun `on all apps closed, animation required, open count is 1, installed app is NOT null, animation NOT required`() {
        updateManager.installedApp = "installed app"
        updateManager.openCount = 1
        updateManager.requireAnimation = true

        updateManager.onAllAppsClosed()

        assertThat(updateManager.requireAnimation, `is`(false))
    }

    @Test
    fun `last update hour changed, update required`() {
        updateManager.lastUpdateHour = 10
        updateManager.lastUpdateDay = 1
        updateManager.calendar.value[Calendar.HOUR_OF_DAY] = 11
        updateManager.calendar.value[Calendar.DAY_OF_WEEK] = 1

        assertThat(updateManager.requireUpdateForVanga(), `is`(true))
    }

    @Test
    fun `last update day changed, update required`() {
        updateManager.lastUpdateHour = 11
        updateManager.lastUpdateDay = 2
        updateManager.calendar.value[Calendar.HOUR_OF_DAY] = 11
        updateManager.calendar.value[Calendar.DAY_OF_WEEK] = 1

        assertThat(updateManager.requireUpdateForVanga(), `is`(true))
    }

    @Test
    fun `day and hour not changed, update NOT required`() {
        updateManager.setHourNotChanged()

        assertThat(updateManager.requireUpdateForVanga(), `is`(false))
    }

    @Test
    fun `when called requireUpdateForVanga, calendar updated`() {
        verifyNoInteractions(updateManager)

        updateManager.requireUpdateForVanga()

        verify(updateManager).updateCalendar()
    }

    @Test
    fun `apps updated, update calendar called`() {
        verifyNoInteractions(updateManager)

        updateManager.onAppsUpdated()

        verify(updateManager).updateCalendar()
    }
}

private fun NewAppsGridUpdateManager.setHourNotChanged() {
    lastUpdateHour = 11
    lastUpdateDay = 1
    calendar.value[Calendar.HOUR_OF_DAY] = 11
    calendar.value[Calendar.DAY_OF_WEEK] = 1
}
