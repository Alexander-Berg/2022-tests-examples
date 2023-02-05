package com.yandex.sync.lib

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.provider.CalendarContract
import androidx.os.bundleOf
import com.nhaarman.mockito_kotlin.mock
import com.yandex.sync.lib.entity.CalendarEntity
import com.yandex.sync.lib.entity.EventEntity
import com.yandex.sync.lib.provider.LocalCalendarProvider
import com.yandex.sync.lib.utils.SyncTestRunner
import io.mockk.every
import io.mockk.mockkClass
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.util.Date
import java.util.concurrent.TimeUnit


@RunWith(SyncTestRunner::class)
class SyncAdapterTest {

    private lateinit var adapter: TestAdapter

    lateinit var localCalendarProvider: LocalCalendarProvider

    @Before
    fun setUp() {
        localCalendarProvider = mockkClass(
                type = LocalCalendarProvider::class,
                relaxed = true,
                moreInterfaces = *arrayOf(CalendarProvider::class)
        ) {
            every {
                getEventsBySyncId(
                        //arrayListOf("sync1", "sync2", "sync3")
                        any()
                )
            } returns listOf(
                    EventEntity(
                            id = 2,
                            title = "Школа Мобильной разработки 2",
                            icsHref = "icsHref2",
                            startTime = Date(1573516900),
                            endTime = Date(1573517000),
                            uid = "uid2",
                            transparency = EventEntity.OPAQUE,
                            url = "url2",
                            etag = ""
                    ),
                    EventEntity(
                            id = 3,
                            title = "Школа Мобильной разработки 3",
                            icsHref = "icsHref3",
                            startTime = Date(1573517800),
                            endTime = Date(1573517000),
                            uid = "uid3",
                            transparency = EventEntity.OPAQUE,
                            url = "url3",
                            etag = ""
                    )
            )
        }
        val context = RuntimeEnvironment.application.baseContext
        val baseUrl = "https://caldav.com"

        adapter = TestAdapter(context, true, { Single.just("token") }, SyncProperties(baseUrl))
    }

    @Test
    fun `calendar diff test`() {
        val local = listOf(
                CalendarEntity(
                        calendarId = 1,
                        displayName = "Школа Мобильной разработки 2017",
                        name = "Школа Мобильной разработки 2017",
                        color = "#0d866aff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "sync1",
                        ctag = "1514287893608",
                        syncToken = "data:,1514287893608"
                ),
                CalendarEntity(
                        calendarId = 2,
                        displayName = "Школа Мобильной разработки 2017",
                        name = "Школа Мобильной разработки 2017",
                        color = "#0d866aff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "sync2",
                        ctag = "1514287893608",
                        syncToken = "data:,1514287893608"
                )
        )

        val remote = listOf(
                CalendarEntity(
                        calendarId = -1L,
                        displayName = "Школа Мобильной разработки",
                        name = "Школа Мобильной разработки",
                        color = "#0d866aff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "sync2",
                        ctag = "1514287893608",
                        syncToken = "data:,1514287893608"
                ),
                CalendarEntity(
                        calendarId = -1L,
                        displayName = "Школа Мобильной разработки 2017",
                        name = "Школа Мобильной разработки 2017",
                        color = "#0d866aff",
                        owner = "/principals/users/thevery%40yandex-team.ru/",
                        href = "sync3",
                        ctag = "1514287893608",
                        syncToken = "data:,1514287893608"
                )
        )
        val (create, update, delete) = adapter.calendarDifference(local, remote)

        assertEquals(1, create.size)
        assertEquals(1, update.size)
        assertEquals(1, delete.size)

        assertEquals(1L, delete[0].calendarId)
        assertEquals(2L, update[0].calendarId)
        assertEquals("sync2", update[0].syncId)
        assertEquals("sync3", create[0].syncId)
    }

    @Test
    fun `events diff test`() {
        adapter.localCalendarProvider = localCalendarProvider
        val (create, update, delete) = adapter.eventsDifference(listOf(
                EventEntity(
                        id = -1L,
                        title = "Школа Мобильной разработки 1",
                        icsHref = "icsHref1",
                        startTime = Date(1573516900),
                        endTime = Date(1573517000),
                        uid = "uid1",
                        transparency = EventEntity.OPAQUE,
                        url = "url1",
                        etag = ""
                ),
                EventEntity(
                        id = -1L,
                        title = "Школа Мобильной разработки 2",
                        icsHref = "icsHref2",
                        startTime = Date(1573516900),
                        endTime = Date(1573517000),
                        uid = "uid2",
                        transparency = EventEntity.OPAQUE,
                        url = "url2",
                        etag = ""
                ),
                EventEntity(
                        id = -1L,
                        title = "",
                        icsHref = "icsHref3",
                        startTime = Date(0),
                        endTime = Date(0),
                        uid = "uid3",
                        transparency = EventEntity.OPAQUE,
                        url = "url3",
                        etag = ""
                ).apply {
                    responseStatus = EventEntity.STATUS_REMOVED
                }
        ))


        assertEquals(1, create.size)
        assertEquals(1, update.size)
        assertEquals(1, delete.size)

        assertEquals("uid1", create[0].syncId)
        assertEquals("uid2", update[0].syncId)
        assertEquals("uid3", delete[0].syncId)
    }

    @Test
    fun `onPerformSync migration to 4_26`() {
        val account = Account("name", "type")
        ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, Bundle.EMPTY, TimeUnit.MINUTES.toSeconds(15))
        assertEquals(ContentResolver.getPeriodicSyncs(account, CalendarContract.AUTHORITY).isEmpty(), false)

        adapter.onPerformSync(account, Bundle.EMPTY, CalendarContract.AUTHORITY, mock(), SyncResult())
        assertEquals(ContentResolver.getPeriodicSyncs(account, CalendarContract.AUTHORITY).isEmpty(), true)
    }

    @Test
    fun `extras upload test`() {
        val account = Account("name", "type")

        adapter.resetFlags()

        adapter.onPerformSync(account, Bundle.EMPTY, CalendarContract.AUTHORITY, mock(), SyncResult())
        assertEquals(adapter.flagSyncedLocalChanges, true)
        assertEquals(adapter.flagSyncedRemoteChanges, true)

        adapter.resetFlags()

        adapter.onPerformSync(account, bundleOf(ContentResolver.SYNC_EXTRAS_UPLOAD to true), CalendarContract.AUTHORITY, mock(), SyncResult())
        assertEquals(adapter.flagSyncedLocalChanges, true)
        assertEquals(adapter.flagSyncedRemoteChanges, false)

        adapter.resetFlags()

        adapter.onPerformSync(account, bundleOf(ContentResolver.SYNC_EXTRAS_UPLOAD to false), CalendarContract.AUTHORITY, mock(), SyncResult())
        assertEquals(adapter.flagSyncedLocalChanges, true)
        assertEquals(adapter.flagSyncedRemoteChanges, true)
    }

    class TestAdapter(context: Context,
                      autoInitialize: Boolean,
                      override val tokenProvider: () -> Single<String>,
                      override val syncProperties: SyncProperties)
        : AbstractCalDavSyncAdapter(context, autoInitialize) {

        var flagSyncedLocalChanges: Boolean = false

        var flagSyncedRemoteChanges: Boolean = false

        override fun reportEvent(event: String) {}

        override fun reportEvent(event: String, map: Map<String, Any?>) {}

        override fun reportError(throwable: Throwable) {}

        override fun syncRemoteChanges() {
            flagSyncedRemoteChanges = true
            super.syncRemoteChanges()
        }

        override fun syncLocalChanges() {
            flagSyncedLocalChanges = true
            super.syncLocalChanges()
        }

        public override lateinit var localCalendarProvider: LocalCalendarProvider

        fun resetFlags() {
            flagSyncedRemoteChanges = false
            flagSyncedLocalChanges = false
        }

    }

}
