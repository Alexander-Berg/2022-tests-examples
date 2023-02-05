package ru.yandex.disk.feed

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.yandex.datasync.DataSyncManager
import com.yandex.datasync.ErrorType
import com.yandex.datasync.WrappersObserver
import com.yandex.datasync.YDSContext
import com.yandex.datasync.internal.database.excpetions.RecordNotExistsException
import com.yandex.datasync.wrappedModels.Collection
import com.yandex.datasync.wrappedModels.Database
import com.yandex.datasync.wrappedModels.Error
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import rx.observers.TestObserver
import rx.observers.TestSubscriber
import java.util.Collections.emptyList

private const val SOME_COLLECTION_NAME = "some"
private const val TEST_REVISION_VALUE = 100L

class DiskDataSyncManagerTest {

    private val mockDataSyncManager = mock<DataSyncManager>()
    private val mockDatabase = mock<Database> {
        on { this.revision } doReturn TEST_REVISION_VALUE
    }
    private val someCollection = mock<Collection> {
        on { this.collectionId } doReturn SOME_COLLECTION_NAME
    }

    private val feedDataSyncManager = DiskDataSyncManager(
            mockDataSyncManager,
            mock(),
            mock(),
            SOME_COLLECTION_NAME)

    @Test
    fun `should reset database in data sync manager on reset`() {
        feedDataSyncManager.close()

        verify(mockDataSyncManager).resetDatabase(any(), any())
    }

    @Test
    fun `should not reset database in data sync manager on reset if used`() {
        feedDataSyncManager.requestRemoteCollection(SOME_COLLECTION_NAME).subscribe()

        feedDataSyncManager.close()

        verify(mockDataSyncManager, never()).resetDatabase(any(), any())
    }

    @Test
    fun `should reset database in sync manager on reset after usage`() {
        feedDataSyncManager.requestRemoteCollection(SOME_COLLECTION_NAME).subscribe()

        feedDataSyncManager.close()

        val observerCaptor = ArgumentCaptor.forClass(WrappersObserver::class.java)
        verify(mockDataSyncManager).addObserver(observerCaptor.capture())
        observerCaptor.value.notifyCollectionRetrieved(someCollection, 0)

        verify(mockDataSyncManager).resetDatabase(any(), any())
    }

    @Test
    fun `should return empty observable after reset`() {
        feedDataSyncManager.close()

        val testObserver = TestObserver<BetterCollection>()
        feedDataSyncManager.requestRemoteCollection(SOME_COLLECTION_NAME).subscribe(testObserver)

        assertThat(testObserver.onCompletedEvents, not(empty()))
    }

    @Test
    fun `should not RecordNotExistsException be thrown out`() {
        feedDataSyncManager.close()

        whenever(mockDataSyncManager.requestLocalCollection(eq(YDSContext.APP), anyString(),
                eq(SOME_COLLECTION_NAME))).thenThrow(RecordNotExistsException("record", null, null))
        val testObserver = TestSubscriber<BetterCollection>()
        feedDataSyncManager.requestRemoteCollection(SOME_COLLECTION_NAME).subscribe(testObserver)

        assertThat(testObserver.onErrorEvents, empty())
        assertThat(testObserver.completions, not(0))
    }

    @Test
    fun `should return empty collection if not found error`() {
        val testObserver = TestObserver<BetterCollection>()
        val collectionId = SOME_COLLECTION_NAME
        feedDataSyncManager.requestRemoteCollection(collectionId).subscribe(testObserver)

        val observer = captureObserver()
        doAnswer {
            observer.notifyDatabaseInfoRetrieved(mockDatabase)
            null
        }.whenever(mockDataSyncManager).requestLocalDatabaseInfo(any<YDSContext>(), anyString())

        observer.notifyError(Error(ErrorType.HTTP_NOT_FOUND, "test"))

        val collection = testObserver.onNextEvents[0]
        assertThat(collection.collectionId, equalTo(collectionId))
        assertThat(collection.revision, equalTo(TEST_REVISION_VALUE))
        assertThat(collection.records, `is`(emptyList()))
        assertThat<String>(collection.nextCollectionId, nullValue())

        verify(mockDataSyncManager).removeObserver(observer)
    }

    private fun captureObserver(): WrappersObserver {
        val observerCaptor = ArgumentCaptor.forClass(WrappersObserver::class.java)
        verify(mockDataSyncManager).addObserver(observerCaptor.capture())
        return observerCaptor.value
    }

    @Test
    fun `should remove data sync observer after getting result`() {
        feedDataSyncManager.requestRemoteCollection(SOME_COLLECTION_NAME)
                .subscribe {
                    verify(mockDataSyncManager).removeObserver(any<WrappersObserver>())
                }

        val observer = captureObserver()

        observer.notifyCollectionRetrieved(someCollection, 0)
    }

    @Test
    fun `should not crash if unexpected NPE on Asus`() {
        whenever(mockDataSyncManager.requestCollection(any<YDSContext>(), anyString(), anyString()))
                .thenThrow(NullPointerException("Attempt to invoke virtual method 'retrofit2.l com.yandex.datasync.internal.api.retrofit.b.a(retrofit2.b)' on a null object reference"))

        val testSubscriber = TestSubscriber<BetterCollection>()
        feedDataSyncManager.requestRemoteCollection("123")
                .subscribe(testSubscriber)

        assertThat(testSubscriber.onErrorEvents.first(), instanceOf(DiskDataSyncException::class.java))
    }
}
