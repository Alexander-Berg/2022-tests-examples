package ru.yandex.disk.commonactions

import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoMoreInteractions
import com.yandex.disk.rest.json.ErrorData
import com.yandex.disk.rest.json.Link
import org.hamcrest.Matchers.*
import org.junit.Test
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.DiskEvents.RemoteDirectoryChanged
import ru.yandex.disk.event.EventLogger
import java.util.concurrent.TimeUnit

private const val DIR_PATH = "/disk/dir"

@Config(manifest = Config.NONE)
class TrackDirectoryOperationProgressCommandTest : BaseTrackOperationProgressCommandTest() {
    private val eventLogger = EventLogger()
    private val link = Link()
    private val request = TrackDirectoryOperationProgressCommandRequest(link, DIR_PATH)
    private val command = TrackDirectoryOperationProgressCommand(
            remoteRepo, testScheduler, eventLogger, deleteInProgressRegistry)

    @Test
    fun `should send events on fast operation`() {
        operationResponses.add(operationComplete)

        command.execute(request)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        assertThat(eventLogger.count, equalTo(1))
        assertThat(eventLogger.first, instanceOf(RemoteDirectoryChanged::class.java))
        assertTrue(operationResponses.isEmpty())
    }

    @Test
    fun `should poll when get in progress`() {
        operationResponses.add(operationInProgress)
        operationResponses.add(operationComplete)

        command.execute(request)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
        assertThat(eventLogger.count, equalTo(0))

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        assertThat(eventLogger.count, equalTo(1))
        assertThat(eventLogger.first, instanceOf(RemoteDirectoryChanged::class.java))

        assertTrue(operationResponses.isEmpty())
    }

    @Test
    fun `should poll many times`() {
        for (i in 0..19) {
            operationResponses.add(operationInProgress)
        }
        operationResponses.add(operationComplete)

        command.execute(request)

        testScheduler.advanceTimeBy(1, TimeUnit.HOURS)
        assertThat(eventLogger.count, equalTo(1))
        assertThat(eventLogger.first, instanceOf(RemoteDirectoryChanged::class.java))
        assertThat(operationResponses.size, equalTo(12))
    }

    @Test
    fun `should skip error polling`() {
        operationResponses.add(operationInProgress)
        operationResponses.add(null)
        operationResponses.add(null)
        operationResponses.add(operationComplete)

        command.execute(request)

        testScheduler.advanceTimeBy(36, TimeUnit.SECONDS)
        assertThat(eventLogger.count, equalTo(1))
        assertThat(eventLogger.first, instanceOf(RemoteDirectoryChanged::class.java))
        assertTrue(operationResponses.isEmpty())
    }


    @Test
    fun `should delete dir path from registry`() {
        operationResponses.add(operationComplete)

        command.execute(request)

        testScheduler.advanceTimeBy(36, TimeUnit.SECONDS)
        verify(deleteInProgressRegistry).remove(DIR_PATH)
    }

    @Test
    fun `should stop pulling on failed operation`() {
        operationResponses.add(operationInProgress)
        operationResponses.add(operationFailedStorageExhausted)

        command.execute(request)

        testScheduler.advanceTimeBy(6, TimeUnit.SECONDS)

        verify(remoteRepo, times(2)).getOperation(link)
        verifyNoMoreInteractions(remoteRepo)
        assertThat(operationResponses, empty())
    }

    @Test
    fun `should send event on operation failed`() {
        operationResponses.add(operationFailedStorageExhausted)

        command.execute(request)

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)


        assertThat(eventLogger.count, equalTo(2))
        assertThat(eventLogger.first, instanceOf(DiskEvents.RemoteDirectoryChanged::class.java))

        val secondEvent = eventLogger.get(1)
        assertThat(secondEvent, instanceOf(DiskEvents.RemoteOperationFailed::class.java))
        assertThat((secondEvent as DiskEvents.RemoteOperationFailed).error, equalTo(ErrorData.ErrorTypes.DISK_OWNER_STORAGE_QUOTA_EXHAUSTED_ERROR))
    }

}
