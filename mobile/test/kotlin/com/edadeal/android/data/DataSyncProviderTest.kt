package com.edadeal.android.data

import android.test.mock.MockContext
import com.edadeal.android.data.datasync.DataSyncConfig
import com.edadeal.android.data.datasync.DataSyncDatabaseIds
import com.edadeal.android.data.datasync.DataSyncDatabaseInfo
import com.edadeal.android.data.datasync.DataSyncJsonWriter
import com.edadeal.android.data.datasync.DataSyncManagerDelegate
import com.edadeal.android.data.datasync.DataSyncProvider
import com.edadeal.android.data.datasync.DataSyncRevisionInfo
import com.edadeal.android.model.AuthPresenter
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.datasync.wrappedModels.Record
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import okio.Buffer
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(MockitoJUnitRunner::class)
class DataSyncProviderTest {
    private val databaseId = "dataSyncDb"
    private val userInfo = AuthPresenter.YaUserInfo(socialUid = "", accessToken = "token")

    @Mock private lateinit var ctx: MockContext
    @Mock private lateinit var jsonWriter: DataSyncJsonWriter
    @Mock private lateinit var dataSyncDelegate: DataSyncManagerDelegate
    @Mock private lateinit var remoteRecords: List<Record>
    @Mock private lateinit var localRecords: List<Record>

    private lateinit var provider: DataSyncProvider

    @BeforeTest
    fun prepare() {
        val databaseIds = DataSyncDatabaseIds(DataSyncConfig.DEV)
        val realProvider = DataSyncProvider(
            ctx, databaseIds, Single.just(userInfo), { jsonWriter }, { _, _ -> dataSyncDelegate }
        )
        provider = spy(realProvider)
        whenever(dataSyncDelegate.userId).thenReturn(userInfo.socialUid)
    }

    @Test
    fun `dataSyncProvider should get local database if it available`() {
        whenever(dataSyncDelegate.retrieveDatabaseRecords(any(), eq(true)))
            .thenReturn(Single.just(localRecords))
        val observer = TestObserver<Buffer>()

        provider.getDatabaseRecords(databaseId).subscribe(observer)

        verify(dataSyncDelegate, never()).resetDatabase(any())
        verify(dataSyncDelegate, never()).getLocalDatabaseRevision(any())
        verify(dataSyncDelegate).retrieveDatabaseRecords(databaseId, true)
        verify(dataSyncDelegate, never()).retrieveDatabaseRecords(databaseId, false)
        verify(jsonWriter).writeArray(localRecords)
        observer.assertValueCount(1)
        observer.assertComplete()
    }

    @Test
    fun `dataSyncProvider should get remote database if local is unavailable`() {
        whenever(dataSyncDelegate.retrieveDatabaseRecords(any(), eq(false)))
            .thenReturn(Single.just(remoteRecords))
        whenever(dataSyncDelegate.retrieveDatabaseRecords(any(), eq(true)))
            .thenReturn(Single.error(Exception()))
        val observer = TestObserver<Buffer>()

        provider.getDatabaseRecords(databaseId).subscribe(observer)

        verify(dataSyncDelegate, never()).resetDatabase(any())
        verify(dataSyncDelegate, never()).getLocalDatabaseRevision(any())
        verify(dataSyncDelegate).retrieveDatabaseRecords(databaseId, false)
        verify(dataSyncDelegate).retrieveDatabaseRecords(databaseId, true)
        verify(jsonWriter).writeArray(remoteRecords)
        observer.assertValueCount(1)
        observer.assertComplete()
    }

    @Test
    fun `dataSyncProvider should use sync for local database update`() {
        val revisionInfo = DataSyncRevisionInfo(revision = 1L, databaseId = "")
        val databaseInfo = DatabaseInfo(revision = revisionInfo.revision)
        whenever(dataSyncDelegate.syncLocalDatabase(any())).thenReturn(Single.just(revisionInfo))
        whenever(dataSyncDelegate.getLocalDatabaseRevision(any())).thenReturn(Single.just(0L))
        whenever(dataSyncDelegate.getLocalDatabaseInfo(any())).thenReturn(Single.just(databaseInfo))
        val observer = TestObserver<DataSyncDatabaseInfo>()

        provider.updateDatabase(databaseId).subscribe(observer)

        verify(dataSyncDelegate).getLocalDatabaseRevision(databaseId)
        verify(dataSyncDelegate).syncLocalDatabase(databaseId)
        verify(dataSyncDelegate, never()).resetDatabase(any())
        verify(dataSyncDelegate, never()).retrieveDatabaseRecords(any(), any())
        verify(jsonWriter, never()).writeArray(any())
        observer.assertValueCount(1)
        observer.assertComplete()
    }

    @Test
    fun `dataSyncProvider shouldn't signal on update if local and remote revisions are matches`() {
        val revisionInfo = DataSyncRevisionInfo(revision = 0L, databaseId = "")
        whenever(dataSyncDelegate.syncLocalDatabase(any())).thenReturn(Single.just(revisionInfo))
        whenever(dataSyncDelegate.getLocalDatabaseRevision(any())).thenReturn(Single.just(0L))
        val observer = TestObserver<DataSyncDatabaseInfo>()

        provider.updateDatabase(databaseId).subscribe(observer)

        verify(dataSyncDelegate).syncLocalDatabase(databaseId)
        verify(dataSyncDelegate, never()).resetDatabase(any())
        verify(dataSyncDelegate, never()).retrieveDatabaseRecords(any(), any())
        verify(jsonWriter, never()).writeArray(any())
        observer.assertNoValues()
        observer.assertComplete()
    }

    @Test
    fun `dataSyncProvider should signal error on get if remote and local databases are unavailable`() {
        val error = Exception("retrieveDatabaseError")
        whenever(dataSyncDelegate.retrieveDatabaseRecords(any(), eq(false)))
            .thenReturn(Single.error(error))
        whenever(dataSyncDelegate.retrieveDatabaseRecords(any(), eq(true)))
            .thenReturn(Single.error(Exception()))
        val observer = TestObserver<Buffer>()

        provider.getDatabaseRecords(databaseId).subscribe(observer)

        verify(dataSyncDelegate, never()).resetDatabase(any())
        verify(dataSyncDelegate, never()).getLocalDatabaseRevision(any())
        verify(dataSyncDelegate).retrieveDatabaseRecords(databaseId, false)
        verify(dataSyncDelegate).retrieveDatabaseRecords(databaseId, true)
        verify(jsonWriter, never()).writeArray(any())
        observer.assertError(error)
    }

    private class DatabaseInfo(
        override val revision: Long = 0L,
        override val timestamp: Long = 0L,
        override val databaseId: String = ""
    ) : DataSyncDatabaseInfo
}
