package com.yandex.sync.lib.factory

import android.database.Cursor
import android.database.MatrixCursor
import com.yandex.sync.lib.entity.AttendeeEntity
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SyncTestRunner::class)
class AttendeeEntityFactoryTest {

    @Test
    fun `simple test`() {
        val cursor = createMockAttendeeCursor()
        cursor.moveToFirst()
        val attendee = AttendeeEntityFactory.fromCursor(cursor)
        assertEquals(false, attendee.isOrganizer)
        assertEquals("email@email.com", attendee.email)
        assertEquals("Your Name", attendee.name)
        assertEquals(2L, attendee.eventId)
        assertEquals("INDIVIDUAL", attendee.type)
        assertEquals(AttendeeEntity.Status.ACCEPTED, attendee.status)
    }


    companion object {

        fun createMockAttendeeCursor(): Cursor =
                MatrixCursor(AttendeeEntityFactory.ATTENDEE_PROJECTION).apply {
                    addRow(
                            arrayOf(
                                    "2",
                                    "1",
                                    "1",
                                    "1",
                                    "email@email.com",
                                    "Your Name"
                            )
                    )
                }

    }

}