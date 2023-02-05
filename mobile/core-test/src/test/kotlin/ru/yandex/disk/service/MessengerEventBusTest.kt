package ru.yandex.disk.service

import android.os.Binder
import android.os.Messenger
import org.mockito.kotlin.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.Event

private const val TEST_UPLOADED_ITEM = "testUploadedItem"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MessengerEventBusTest {

    private val bus = MessengerEventBus()

    @Test
    fun `should pass event to registered messengers`() {
        val firstMockMessenger = mockAndRegisterMessenger()
        val secondMockMessenger = mockAndRegisterMessenger()

        bus.send(DiskEvents.UploadTaskCompleted(TEST_UPLOADED_ITEM))

        verifyMessengerReceivedEvent(firstMockMessenger)
        verifyMessengerReceivedEvent(secondMockMessenger)
    }

    @Test
    fun `should not send message if messenger binder is not alive`() {
        val mockMessenger = mockAndRegisterMessenger(isBinderAlive = false)

        bus.send(DiskEvents.UploadTaskCompleted(TEST_UPLOADED_ITEM))

        verify(mockMessenger, never()).send(any())
    }

    @Test(expected = RuntimeException::class)
    fun `should throw exception on not bundable event in debug`() {
        bus.send(object : Event() {})
    }

    private fun verifyMessengerReceivedEvent(firstMockMessenger: Messenger) {
        verify(firstMockMessenger).send(argThat({
            val event = DiskEvents.UploadTaskCompleted()
            event.restore(data)
            event.lastUploadedItem == TEST_UPLOADED_ITEM
        }))
    }

    private fun mockAndRegisterMessenger(isBinderAlive: Boolean = true): Messenger {
        val mockMessenger = mock<Messenger>()
        val binder = mock<Binder>()
        whenever(binder.isBinderAlive).thenReturn(isBinderAlive)
        whenever(mockMessenger.binder).thenReturn(binder)

        bus.register(mockMessenger)
        return mockMessenger
    }

}
