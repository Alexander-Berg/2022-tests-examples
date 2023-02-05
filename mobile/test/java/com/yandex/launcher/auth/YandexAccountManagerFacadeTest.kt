package com.yandex.launcher.auth

import android.app.Activity
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.Launcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test

class YandexAccountManagerFacadeTest: BaseRobolectricTest() {

    val launcherActivity = mock<Launcher>()
    val notLauncherActivity = mock<Activity>()
    val yandexAccountManagerFacade = YandexAccountManagerFacade()

    @Test
    fun `with non-launcher activity is allowed to start login activity`() {
        assertThat(yandexAccountManagerFacade.isAllowedToStartActivity(notLauncherActivity), `is`(true))
    }

    @Test
    fun `not allowed to start activity, if workspace locked`() {
        whenever(launcherActivity.isWorkspaceLocked).thenReturn(true)
        whenever(launcherActivity.isPaused).thenReturn(false)

        assertThat(yandexAccountManagerFacade.isAllowedToStartActivity(launcherActivity), `is`(false))
    }

    @Test
    fun `not allowed to start activity, if launcher is paused`() {
        whenever(launcherActivity.isWorkspaceLocked).thenReturn(false)
        whenever(launcherActivity.isPaused).thenReturn(true)

        assertThat(yandexAccountManagerFacade.isAllowedToStartActivity(launcherActivity), `is`(false))
    }

    @Test
    fun `allowed to start activity if workspace not locked and launcher not paused`() {
        whenever(launcherActivity.isWorkspaceLocked).thenReturn(false)
        whenever(launcherActivity.isPaused).thenReturn(false)

        assertThat(yandexAccountManagerFacade.isAllowedToStartActivity(launcherActivity), `is`(true))
    }
}
