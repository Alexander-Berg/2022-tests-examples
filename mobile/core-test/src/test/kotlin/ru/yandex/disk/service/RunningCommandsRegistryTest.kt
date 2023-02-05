package ru.yandex.disk.service

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2

class RunningCommandsRegistryTest : AndroidTestCase2() {

    val serviceController = mock<ServiceController>()
    val registry = RunningCommandsRegistry(serviceController)

    class CommandRequest1 : CommandRequest()
    class CommandRequest2 : CommandRequest()

    @Test
    fun `should notify service controller when request is added to registry`() {
        registry.addToRegistry(CommandRequest1())

        verify(serviceController).onCommandExecutionStarted(any<CommandRequest1>())
    }

    @Test
    fun `should notify service controller when request is finished`() {
        registry.addToRegistry(CommandRequest1())
        registry.addToRegistry(CommandRequest1())
        registry.addToRegistry(CommandRequest2())

        registry.removeFromRegistry(CommandRequest2())
        registry.removeFromRegistry(CommandRequest1())
        registry.removeFromRegistry(CommandRequest1())
        verify(serviceController).onCommandExecutionFinished(any<CommandRequest2>())
        verify(serviceController, times(2)).onCommandExecutionFinished(any<CommandRequest1>())
        verify(serviceController).onAllCommandsExecutionFinished()
    }
}
