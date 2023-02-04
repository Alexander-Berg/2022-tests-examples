package ru.auto.ara

import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.test.runner.AllureRobolectricRunner
import kotlin.test.assertTrue

@RunWith(AllureRobolectricRunner::class) class AutoApplicationTest: RobolectricTest() {

    @Test
    fun `should configure given plugins within onCreate method`() {
        // AutoApplication.onCreate called while setup environment
        val pluginsInteractor = AutoApplication.pluginsInteractor
        val pluginsCount = pluginsInteractor.config.backgroundPlugins.size
            .plus(pluginsInteractor.config.startupPlugins.size)
            .plus(pluginsInteractor.config.interactivePlugins.size)

        assertTrue { pluginsCount > 0 }
    }
}
