package ru.yandex.disk

import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.disk.service.Command
import ru.yandex.disk.service.CommandFactoryAndExecutor
import ru.yandex.disk.service.CommandRequest
import ru.yandex.disk.service.SimpleCommandsMap
import ru.yandex.disk.util.Executors2
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class SimpleCommandsMapTest {

    class MyCommandRequest : CommandRequest()

    class MyCommand : Command<MyCommandRequest> {
        override fun execute(request: MyCommandRequest) {
        }
    }

    @Test
    fun `should get by class`() {
        val r = MyCommandRequest::class.java
        val p = Provider { MyCommand() }
        val e = Executors2.RUN_IMMEDIATELY_EXECUTOR
        val map = SimpleCommandsMap(mapOf(r to CommandFactoryAndExecutor(p, e)))

        val factoryAndExecutors = map.get(MyCommandRequest())
        val command = factoryAndExecutors[0].factory.get()

        assertThat(command, instanceOf(MyCommand::class.java))
    }
}
