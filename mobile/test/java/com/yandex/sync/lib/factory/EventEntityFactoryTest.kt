package com.yandex.sync.lib.factory

import android.database.Cursor
import android.database.MatrixCursor
import com.yandex.sync.lib.asSequence
import com.yandex.sync.lib.factory.EventEntityFactory.EVENT_PROJECTION
import com.yandex.sync.lib.utils.SyncTestRunner
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(SyncTestRunner::class)
class EventEntityFactoryTest {

    @Test
    fun simpleCase() {
        val cursorFirst = createMockEventCursor().asSequence().first()
        val event = EventEntityFactory.fromCursor(cursorFirst)

        Assertions.assertThat(event.id).isEqualTo(1)
        Assertions.assertThat(event.title).isEqualTo("title")
        Assertions.assertThat(event.startTime).isEqualTo(Date(1541768799000))
        Assertions.assertThat(event.endTime).isEqualTo(Date(1541768800000))
        Assertions.assertThat(event.syncId).isEqualTo("uid1")
        Assertions.assertThat(event.uid).isEqualTo("uid1")
        Assertions.assertThat(event.icsHref).isEqualTo("icshref1")
        Assertions.assertThat(event.allDay).isEqualTo(true)
        Assertions.assertThat(event.dirty).isEqualTo(true)
        Assertions.assertThat(event.isDeleted).isEqualTo(false)
        Assertions.assertThat(event.etag).isEqualTo("etag")
        Assertions.assertThat(event.originalId).isEqualTo(15)
        Assertions.assertThat(event.originalInstanceTime).isEqualTo(1541768799000)
        Assertions.assertThat(event.originalSyncId).isEqualTo("original_sync_id")
    }

    @Test
    fun `nullable fields`() {
        val cursorFirst = createMockEventNullableCursor().asSequence().first()
        val event = EventEntityFactory.fromCursor(cursorFirst)

        Assertions.assertThat(event.id).isEqualTo(1)
        Assertions.assertThat(event.title).isEqualTo("title")
        Assertions.assertThat(event.startTime).isEqualTo(Date(1541768799000))
        Assertions.assertThat(event.endTime).isEqualTo(Date(1541768800000))
        Assertions.assertThat(event.syncId).isEqualTo("")
        Assertions.assertThat(event.uid).isEqualTo("")
        Assertions.assertThat(event.icsHref).isEqualTo("")
        Assertions.assertThat(event.etag).isEqualTo("")
        Assertions.assertThat(event.originalSyncId).isEqualTo("")
        Assertions.assertThat(event.dirty).isEqualTo(false)
        Assertions.assertThat(event.isDeleted).isEqualTo(false)
    }

    @Test
    fun durationCase() {
        val cursorFirst = createMockEventCursorDuration().asSequence().first()
        val event = EventEntityFactory.fromCursor(cursorFirst)

        Assertions.assertThat(event.id).isEqualTo(1)
        Assertions.assertThat(event.title).isEqualTo("title")
        Assertions.assertThat(event.startTime).isEqualTo(Date(1541768799000))
        Assertions.assertThat(event.endTime).isEqualTo(Date(1541770599000))
        Assertions.assertThat(event.syncId).isEqualTo("uid1")
        Assertions.assertThat(event.uid).isEqualTo("uid1")
        Assertions.assertThat(event.icsHref).isEqualTo("icshref1")
        Assertions.assertThat(event.allDay).isEqualTo(true)
        Assertions.assertThat(event.dirty).isEqualTo(true)
        Assertions.assertThat(event.isDeleted).isEqualTo(false)
        Assertions.assertThat(event.etag).isEqualTo("etag")
        Assertions.assertThat(event.originalId).isEqualTo(15)
        Assertions.assertThat(event.originalInstanceTime).isEqualTo(1541768799000)
        Assertions.assertThat(event.originalSyncId).isEqualTo("original_sync_id")
    }

    @Test
    fun negativeDurationCase() {
        val cursorFirst = createMockEventCursorDuration("-P1800S").asSequence().first()
        val event = EventEntityFactory.fromCursor(cursorFirst)

        Assertions.assertThat(event.startTime).isEqualTo(Date(1541768799000))
        Assertions.assertThat(event.endTime).isEqualTo(Date(1541766999000))
    }

    companion object {

        fun createMockEventCursorDuration(duration: String = "P1800S"): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "1",
                                    "title",
                                    "1541768799000",
                                    null,
                                    "uid1",
                                    1,
                                    0,
                                    "etag",
                                    "uid1",
                                    "icshref1",
                                    "original_sync_id",
                                    1,
                                    "FREQ=WEEKLY;BYDAY=WE;INTERVAL=1",
                                    1541768799000,
                                    15,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    duration
                            )
                    )
                }

        fun createMockEventCursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "1",
                                    "title",
                                    "1541768799000",
                                    "1541768800000",
                                    "uid1",
                                    1,
                                    0,
                                    "etag",
                                    "uid1",
                                    "icshref1",
                                    "original_sync_id",
                                    1,
                                    "FREQ=WEEKLY;BYDAY=WE;INTERVAL=1",
                                    1541768799000,
                                    15,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            )
                    )
                }

        fun createMockEventNullableCursor(): Cursor =
                MatrixCursor(EVENT_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "1",
                                    "title",
                                    "1541768799000",
                                    "1541768800000",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            )
                    )
                }
    }

}
