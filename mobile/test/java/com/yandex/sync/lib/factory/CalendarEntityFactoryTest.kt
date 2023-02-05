package com.yandex.sync.lib.factory

import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Color
import com.yandex.sync.lib.asSequence
import com.yandex.sync.lib.factory.CalendarEntityFactory.CALENDAR_PROJECTION
import com.yandex.sync.lib.utils.SyncTestRunner
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SyncTestRunner::class)
class CalendarEntityFactoryTest {

    @Test
    fun color8Sign() {
        val cursorFirst = createMockCalendarCursor().asSequence().first()
        val calendar = CalendarEntityFactory.fromCursor(cursorFirst)

        Assertions.assertThat(calendar.calendarId).isEqualTo(1)
        Assertions.assertThat(calendar.color).isEqualTo("#aabbccee")
        Assertions.assertThat(calendar.displayName).isEqualTo("display_name")
        Assertions.assertThat(calendar.name).isEqualTo("name")
        Assertions.assertThat(calendar.href).isEqualTo("SYNC_ID_1")
        Assertions.assertThat(calendar.ctag).isEqualTo("ctag")
        Assertions.assertThat(calendar.syncToken).isEqualTo("syncToken")
    }

    @Test
    fun color6Sign() {
        val cursor = createMockCalendarCursor()
        cursor.moveToLast()
        val calendar = CalendarEntityFactory.fromCursor(cursor)

        Assertions.assertThat(calendar.calendarId).isEqualTo(2)
        Assertions.assertThat(calendar.color).isEqualTo("#ffbbccee")
        Assertions.assertThat(calendar.displayName).isEqualTo("display_name")
        Assertions.assertThat(calendar.name).isEqualTo("name")
        Assertions.assertThat(calendar.href).isEqualTo("SYNC_ID_2")
        Assertions.assertThat(calendar.ctag).isEqualTo("ctag")
        Assertions.assertThat(calendar.syncToken).isEqualTo("syncToken")
    }

    companion object {

        fun createMockCalendarCursor(): Cursor =
                MatrixCursor(CALENDAR_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "1",
                                    "account_name",
                                    "account_type",
                                    "name",
                                    "display_name",
                                    "SYNC_ID_1",
                                    "ctag",
                                    Color.parseColor("#aabbccee"),
                                    "owner_account",
                                    "syncToken"
                            )
                    )

                    addRow(
                            arrayOf(
                                    "2",
                                    "account_name",
                                    "account_type",
                                    "name",
                                    "display_name",
                                    "SYNC_ID_2",
                                    "ctag",
                                    Color.parseColor("#bbccee"),
                                    "owner_account",
                                    "syncToken"
                            )
                    )
                }

    }

}
