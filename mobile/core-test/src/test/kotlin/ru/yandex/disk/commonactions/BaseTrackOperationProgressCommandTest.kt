package ru.yandex.disk.commonactions

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import com.yandex.disk.rest.json.ErrorData
import com.yandex.disk.rest.json.Operation
import ru.yandex.disk.DeleteInProgressRegistry
import ru.yandex.disk.remote.RemoteRepo
import ru.yandex.disk.test.AndroidTestCase2
import rx.schedulers.TestScheduler
import java.io.IOException
import java.util.*

abstract class BaseTrackOperationProgressCommandTest : AndroidTestCase2() {
    protected val testScheduler = TestScheduler()
    protected val deleteInProgressRegistry = mock<DeleteInProgressRegistry>()
    protected val operationResponses = ArrayList<Operation?>()

    protected val operationInProgress = mock<Operation> {
        on { isInProgress } doReturn true
    }

    protected val operationComplete = mock<Operation> {
        on { isInProgress } doReturn false
    }

    private val mockErrorData = mock<ErrorData> {
        on { error } doReturn ErrorData.ErrorTypes.DISK_OWNER_STORAGE_QUOTA_EXHAUSTED_ERROR
    }

    protected val operationFailedStorageExhausted = mock<Operation> {
        on { isFailed } doReturn true
        on { errorData } doReturn mockErrorData
    }

    protected val remoteRepo = mock<RemoteRepo> {
        on { getOperation(any()) }.then {
            operationResponses.removeAt(0) ?: throw IOException("Emulated network error")
        }
    }

}
