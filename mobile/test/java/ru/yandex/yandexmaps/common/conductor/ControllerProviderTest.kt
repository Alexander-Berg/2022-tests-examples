package ru.yandex.yandexmaps.common.conductor

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)

class ControllerProviderTest {
    @Test
    fun `test restore unique controller`() {
        val id = 11L
        val provider = UniqueClassControllerProvider(TestUniqueController::class, id)
        val controller = provider.createController() as TestUniqueController
        assertEquals(id, controller.uniqueControllerId)

        val restoredController = TestUniqueController()
        restoredController.args.putAll(controller.args)
        assertEquals(id, restoredController.uniqueControllerId)
    }
}
