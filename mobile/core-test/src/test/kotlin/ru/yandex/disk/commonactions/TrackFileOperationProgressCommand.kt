package ru.yandex.disk.commonactions

import org.mockito.kotlin.verify
import com.yandex.disk.rest.json.Link
import org.junit.Test
import java.util.concurrent.TimeUnit

private const val FILE_PATH = "/disk/dir"

class TrackFileOperationProgressCommandTest : BaseTrackOperationProgressCommandTest() {

    private val request = TrackFileOperationProgressCommandRequest(Link(), FILE_PATH)
    private val command = TrackFileOperationProgressCommand(
            remoteRepo, testScheduler, deleteInProgressRegistry)


    @Test
    fun `should delete file path from registry`() {
        operationResponses.add(operationComplete)

        command.execute(request)

        testScheduler.advanceTimeBy(36, TimeUnit.SECONDS)
        verify(deleteInProgressRegistry).remove(FILE_PATH)
    }

}
