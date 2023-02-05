package ru.yandex.disk.commonactions

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.offline.operations.Operation
import ru.yandex.disk.offline.operations.PendingOperations
import ru.yandex.disk.offline.operations.command.SyncPendingOperationsCommandRequest
import ru.yandex.disk.offline.operations.delete.DeleteResourcePayload
import ru.yandex.disk.provider.DiskContract
import ru.yandex.disk.provider.DiskItemBuilder
import ru.yandex.disk.service.CommandLogger

private const val PATH = "/disk/A"

@RunWith(RobolectricTestRunner::class)
class DeleteCommandTest {
    private val eventLogger = EventLogger()
    private val commandLogger = CommandLogger()
    private val pendingOperations = Mockito.mock(PendingOperations::class.java)

    val command = DeleteCommand(
        eventLogger,
        commandLogger,
        pendingOperations
    )

    @Test
    fun `should add files to delete in progress registry`() {
        val fileA = DiskItemBuilder().setPath(PATH).setIsDir(false).build()

        command.execute(DeleteCommandRequest(listOf(fileA)))
        val captor = ArgumentCaptor.forClass(Operation::class.java)
        Mockito.verify(pendingOperations).add(captor.capture())
        val operation = captor.value
        MatcherAssert.assertThat(operation.type(), Matchers.equalTo(DeleteResourcePayload.TYPE))
        MatcherAssert.assertThat(operation.status(), Matchers.equalTo(DiskContract.Operations.Status.INITIAL))
        val payload = operation.payload()
        MatcherAssert.assertThat(payload, Matchers.instanceOf(DeleteResourcePayload::class.java))
        val deletePayload = payload as DeleteResourcePayload
        MatcherAssert.assertThat(deletePayload.resourceId, Matchers.equalTo(PATH))
    }

    @Test
    fun `should start sync pending operations command`() {
        val fileA = DiskItemBuilder().setPath(PATH).setIsDir(false).build()

        command.execute(DeleteCommandRequest(listOf(fileA)))

        MatcherAssert.assertThat(commandLogger.allClasses, Matchers.hasItem(SyncPendingOperationsCommandRequest::class.java))
    }
}