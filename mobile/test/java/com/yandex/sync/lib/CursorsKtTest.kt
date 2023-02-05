package com.yandex.sync.lib

import android.database.MatrixCursor
import com.yandex.sync.lib.utils.SyncTestRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(SyncTestRunner::class)
class CursorsKtTest {

    @Test
    fun asSequence() {
        val cursor = MatrixCursor(arrayOf("col 0"))
        cursor.addRow(arrayOf("row 0"))
        cursor.addRow(arrayOf("row 1"))
        val list = cursor.asSequence().map { it.getString(0) }.toList()
        assertEquals(2, list.size)
        assertEquals("row 0", list[0])
        assertEquals("row 1", list[1])
    }
}
