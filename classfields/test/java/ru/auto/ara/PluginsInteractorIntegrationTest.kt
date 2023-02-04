package ru.auto.ara

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import ru.auto.ara.di.component.main.IMainTabbarProvider
import ru.auto.test.runner.AllureRobolectricRunner

@RunWith(AllureRobolectricRunner::class) class PluginsInteractorIntegrationTest: RobolectricTest() {

    private val pluginsInteractor: PluginsInteractor = mock()

    @Test
    fun `should call plugins through startup flow`() {
        AutoApplication.pluginsInteractor = pluginsInteractor
        IMainTabbarProvider.ref = AutoApplication.COMPONENT_MANAGER.mainTabbarRef

        verify(pluginsInteractor, never()).setupStartupPlugins()
        verify(pluginsInteractor, never()).setupInteractionPlugins()

        Robolectric.buildActivity(SplashActivity::class.java).create().start().resume()

        verify(pluginsInteractor, atLeastOnce()).setupStartupPlugins()
        verify(pluginsInteractor, never()).setupInteractionPlugins()

        Robolectric.buildActivity(MainActivity::class.java).create().start().resume()

        verify(pluginsInteractor, atLeastOnce()).setupInteractionPlugins()
    }
}
