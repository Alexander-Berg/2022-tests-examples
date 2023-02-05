package com.yandex.sync.lib.factory

import android.database.Cursor
import android.database.MatrixCursor
import android.provider.CalendarContract
import com.yandex.sync.lib.entity.AlarmEntity
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SyncTestRunner::class)
class AlarmEntityFactoryTest {

    @Test
    fun `simple test`() {
        val cursor = createMockCursor()
        cursor.moveToFirst()
        val alarmEntity = AlarmEntityFactory.fromCursor(cursor)
        assertEquals(20, alarmEntity.minBeforeEvent)
        assertEquals(1L, alarmEntity.eventId)
        assertEquals(AlarmEntity.AlarmAction.EMAIL, alarmEntity.action)
    }


    companion object {

        fun createMockCursor(): Cursor =
                MatrixCursor(AlarmEntityFactory.ALARM_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "20",
                                    "1",
                                    CalendarContract.Reminders.METHOD_EMAIL.toString()
                            )
                    )
                }

    }

}